package com.weatherconsensus.domain.consensus

import com.weatherconsensus.domain.model.ConfidenceLevel
import com.weatherconsensus.domain.model.ConsensusDailyForecast
import com.weatherconsensus.domain.model.ConsensusHourlyForecast
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.NormalizedDailyForecast
import com.weatherconsensus.domain.model.NormalizedHourlyForecast
import com.weatherconsensus.domain.model.NormalizedWeatherSnapshot
import com.weatherconsensus.domain.model.ProviderValue
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.domain.model.WeatherDetails
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.domain.model.WeatherWarning
import com.weatherconsensus.domain.consensus.ProviderWeightPolicy
import com.weatherconsensus.domain.location.ForecastDateUtils
import com.weatherconsensus.domain.normalization.WeatherConditionMapper
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

class ConsensusEngine {

    fun compute(
        location: GeoLocation,
        providerResults: List<ProviderWeatherResult>,
        weights: ProviderWeights,
        missingApiKeys: List<WeatherProvider>,
        supplementaryDetails: WeatherDetails = WeatherDetails(),
        timezoneId: String? = null,
        weatherWarnings: List<WeatherWarning> = emptyList(),
        warningsLoadError: String? = null,
        isInGermany: Boolean = false,
    ): WeatherConsensusResult {
        val successful = providerResults.filter { it.isSuccess }
        val dwdAvailable = successful.any { it.provider == WeatherProvider.DWD }
        val currentSnapshots = successful.mapNotNull { result ->
            result.current?.let { ProviderValue(result.provider, it) }
        }

        var currentConsensus = buildSnapshot(currentSnapshots, weights, providerResults, supplementaryDetails)

        if (isInGermany && dwdAvailable && currentConsensus.confidence == ConfidenceLevel.UNCERTAIN) {
            val boostedWeights = ProviderWeightPolicy.withUncertaintyDwdBoost(
                weights = weights,
                confidence = currentConsensus.confidence,
                dwdAvailable = true,
            )
            currentConsensus = buildSnapshot(
                currentSnapshots,
                boostedWeights,
                providerResults,
                supplementaryDetails,
            )
        }

        val hourlyTimestamps = successful
            .flatMap { it.hourlyForecast }
            .map { it.timestampEpochSeconds }
            .distinct().sorted().take(24)

        val hourlyConsensus = hourlyTimestamps.map { timestamp ->
            val entries = successful.mapNotNull { result ->
                result.hourlyForecast.find { it.timestampEpochSeconds == timestamp }
                    ?.let { ProviderValue(result.provider, it) }
            }
            buildHourlySnapshot(timestamp, entries, weights)
        }

        val dailyConsensus = buildDailyConsensus(successful, weights, timezoneId)

        return WeatherConsensusResult(
            location = location,
            current = currentConsensus,
            hourlyForecast = hourlyConsensus,
            dailyForecast = dailyConsensus,
            providerResults = providerResults,
            missingApiKeys = missingApiKeys,
            fetchedAtEpochMs = System.currentTimeMillis(),
            timezoneId = timezoneId,
            weatherWarnings = weatherWarnings,
            warningsLoadError = warningsLoadError,
            isInGermany = isInGermany,
        )
    }

    private fun buildDailyConsensus(
        successful: List<ProviderWeatherResult>,
        weights: ProviderWeights,
        timezoneId: String?,
    ): List<ConsensusDailyForecast> {
        val zone = ForecastDateUtils.resolveZone(timezoneId)
        val today = LocalDate.now(zone)
        val dailyByDate = mutableMapOf<LocalDate, MutableList<ProviderValue<NormalizedDailyForecast>>>()

        successful.forEach { result ->
            result.dailyForecast.forEach { daily ->
                val localDate = Instant.ofEpochSecond(daily.dateEpochSeconds).atZone(zone).toLocalDate()
                dailyByDate.getOrPut(localDate) { mutableListOf() }
                    .add(ProviderValue(result.provider, daily))
            }
        }

        return dailyByDate.entries
            .filter { (date, _) -> !date.isBefore(today) }
            .sortedBy { it.key }
            .take(7)
            .map { (localDate, entries) ->
                buildDailySnapshot(
                    date = localDate.atStartOfDay(zone).toEpochSecond(),
                    values = entries,
                    weights = weights,
                )
            }
    }

    private fun buildSnapshot(
        values: List<ProviderValue<NormalizedWeatherSnapshot>>,
        weights: ProviderWeights,
        allResults: List<ProviderWeatherResult>,
        supplementary: WeatherDetails,
    ): ConsensusSnapshot {
        val excluded = allResults
            .filter { !it.isSuccess }
            .associate { it.provider to (it.errorMessage ?: "Gerade nicht verfügbar") }

        val filteredTemp = removeOutliers(values.mapNotNull { pv ->
            pv.value.temperatureC?.let { ProviderValue(pv.provider, it) }
        })
        val filteredWind = removeOutliers(values.mapNotNull { pv ->
            pv.value.windKmh?.let { ProviderValue(pv.provider, it) }
        })
        val filteredPrecip = removeOutliers(values.mapNotNull { pv ->
            pv.value.precipitationMm?.let { ProviderValue(pv.provider, it) }
        })
        val filteredPrecipProb = removeOutliers(values.mapNotNull { pv ->
            pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
        })
        val filteredHumidity = removeOutliers(values.mapNotNull { pv ->
            pv.value.humidityPercent?.let { ProviderValue(pv.provider, it) }
        })
        val filteredFeelsLike = removeOutliers(values.mapNotNull { pv ->
            pv.value.feelsLikeC?.let { ProviderValue(pv.provider, it) }
        })

        val numericScores = listOf(
            agreementScore(filteredTemp),
            agreementScore(filteredWind),
            agreementScore(filteredPrecip),
            agreementScore(filteredHumidity),
        ).filter { it >= 0 }

        val conditionAgreement = conditionAgreement(values.map { it.value.condition })
        val confidenceScore = if (numericScores.isEmpty()) conditionAgreement
        else (numericScores.average() * 0.8) + (conditionAgreement * 0.2)

        val mergedDetails = mergeDetails(
            values.map { it.value.details },
            supplementary,
            weights,
            values.map { ProviderValue(it.provider, it.value.details) },
        )

        return ConsensusSnapshot(
            temperatureC = weightedAverage(filteredTemp, weights),
            feelsLikeC = weightedAverage(filteredFeelsLike, weights),
            windKmh = weightedAverage(filteredWind, weights),
            precipitationMm = weightedAverage(filteredPrecip, weights),
            precipitationProbabilityPercent = weightedAverage(filteredPrecipProb, weights),
            humidityPercent = weightedAverage(filteredHumidity, weights),
            condition = WeatherConditionMapper.consensusCondition(values.map { it.value.condition }),
            confidence = ConfidenceLevel.fromScore(confidenceScore),
            confidenceScore = confidenceScore,
            details = mergedDetails,
            providerContributions = values.associate { it.provider to it.value },
            excludedProviders = excluded,
        )
    }

    private fun mergeDetails(
        providerDetails: List<WeatherDetails>,
        supplementary: WeatherDetails,
        weights: ProviderWeights,
        weighted: List<ProviderValue<WeatherDetails>>,
    ): WeatherDetails {
        fun avg(selector: (WeatherDetails) -> Double?): Double? {
            val vals = weighted.mapNotNull { pv ->
                selector(pv.value)?.let { ProviderValue(pv.provider, it) }
            }
            return weightedAverage(vals, weights)
        }

        fun firstNonNull(selector: (WeatherDetails) -> String?): String? =
            providerDetails.firstNotNullOfOrNull(selector) ?: selector(supplementary)

        fun firstNonNullLong(selector: (WeatherDetails) -> Long?): Long? =
            providerDetails.firstNotNullOfOrNull(selector) ?: selector(supplementary)

        fun firstNonNullInt(selector: (WeatherDetails) -> Int?): Int? =
            providerDetails.firstNotNullOfOrNull(selector) ?: selector(supplementary)

        return WeatherDetails(
            uvIndex = avg { it.uvIndex },
            pressureHpa = avg { it.pressureHpa },
            visibilityKm = avg { it.visibilityKm },
            sunriseEpochSeconds = firstNonNullLong { it.sunriseEpochSeconds },
            sunsetEpochSeconds = firstNonNullLong { it.sunsetEpochSeconds },
            moonPhase = firstNonNull { it.moonPhase },
            moonIlluminationPercent = firstNonNullInt { it.moonIlluminationPercent },
            airQualityIndex = supplementary.airQualityIndex ?: firstNonNullInt { it.airQualityIndex },
            airQualityLabel = supplementary.airQualityLabel ?: firstNonNull { it.airQualityLabel },
            pollenLevel = supplementary.pollenLevel ?: firstNonNull { it.pollenLevel },
        )
    }

    private fun buildDailySnapshot(
        date: Long,
        values: List<ProviderValue<NormalizedDailyForecast>>,
        weights: ProviderWeights,
    ): ConsensusDailyForecast {
        val minTemps = removeOutliers(values.mapNotNull { pv ->
            pv.value.minTempC?.let { ProviderValue(pv.provider, it) }
        })
        val maxTemps = removeOutliers(values.mapNotNull { pv ->
            pv.value.maxTempC?.let { ProviderValue(pv.provider, it) }
        })
        val precipProb = removeOutliers(values.mapNotNull { pv ->
            pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
        })

        return ConsensusDailyForecast(
            dateEpochSeconds = date,
            minTempC = weightedAverage(minTemps, weights),
            maxTempC = weightedAverage(maxTemps, weights),
            condition = WeatherConditionMapper.consensusCondition(values.map { it.value.condition }),
            precipitationProbabilityPercent = weightedAverage(precipProb, weights),
            sunriseEpochSeconds = values.firstNotNullOfOrNull { it.value.sunriseEpochSeconds },
            sunsetEpochSeconds = values.firstNotNullOfOrNull { it.value.sunsetEpochSeconds },
        )
    }

    private fun buildHourlySnapshot(
        timestamp: Long,
        values: List<ProviderValue<NormalizedHourlyForecast>>,
        weights: ProviderWeights,
    ): ConsensusHourlyForecast {
        val filteredTemp = removeOutliers(values.mapNotNull { pv ->
            pv.value.temperatureC?.let { ProviderValue(pv.provider, it) }
        })
        val filteredWind = removeOutliers(values.mapNotNull { pv ->
            pv.value.windKmh?.let { ProviderValue(pv.provider, it) }
        })
        val filteredPrecip = removeOutliers(values.mapNotNull { pv ->
            pv.value.precipitationMm?.let { ProviderValue(pv.provider, it) }
        })
        val filteredPrecipProb = removeOutliers(values.mapNotNull { pv ->
            pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
        })
        val filteredHumidity = removeOutliers(values.mapNotNull { pv ->
            pv.value.humidityPercent?.let { ProviderValue(pv.provider, it) }
        })
        val filteredFeelsLike = removeOutliers(values.mapNotNull { pv ->
            pv.value.feelsLikeC?.let { ProviderValue(pv.provider, it) }
        })

        val scores = listOf(
            agreementScore(filteredTemp),
            agreementScore(filteredWind),
            agreementScore(filteredPrecip),
            agreementScore(filteredHumidity),
        ).filter { it >= 0 }
        val conditionAgreement = conditionAgreement(values.map { it.value.condition })
        val confidenceScore = if (scores.isEmpty()) conditionAgreement
        else (scores.average() * 0.8) + (conditionAgreement * 0.2)

        return ConsensusHourlyForecast(
            timestampEpochSeconds = timestamp,
            temperatureC = weightedAverage(filteredTemp, weights),
            feelsLikeC = weightedAverage(filteredFeelsLike, weights),
            windKmh = weightedAverage(filteredWind, weights),
            precipitationMm = weightedAverage(filteredPrecip, weights),
            precipitationProbabilityPercent = weightedAverage(filteredPrecipProb, weights),
            humidityPercent = weightedAverage(filteredHumidity, weights),
            condition = WeatherConditionMapper.consensusCondition(values.map { it.value.condition }),
            confidence = ConfidenceLevel.fromScore(confidenceScore),
            providerContributions = values.associate { it.provider to it.value },
        )
    }

    private fun <T : Number> removeOutliers(values: List<ProviderValue<T>>): List<ProviderValue<T>> {
        if (values.size <= 2) return values
        val doubles = values.map { it.value.toDouble() }
        val median = doubles.sorted().let { sorted ->
            val mid = sorted.size / 2
            if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
        }
        val deviations = doubles.map { abs(it - median) }.sorted()
        val mad = deviations[deviations.size / 2].coerceAtLeast(0.5)
        return values.filter { abs(it.value.toDouble() - median) <= 2.5 * mad }
    }

    private fun <T : Number> weightedAverage(
        values: List<ProviderValue<T>>,
        weights: ProviderWeights,
    ): Double? {
        if (values.isEmpty()) return null
        var weightedSum = 0.0
        var totalWeight = 0.0
        values.forEach { pv ->
            val w = weights.weightFor(pv.provider).toDouble()
            weightedSum += pv.value.toDouble() * w
            totalWeight += w
        }
        return if (totalWeight > 0) weightedSum / totalWeight else null
    }

    private fun <T : Number> agreementScore(values: List<ProviderValue<T>>): Double {
        if (values.size < 2) return if (values.size == 1) 1.0 else -1.0
        val nums = values.map { it.value.toDouble() }
        val mean = nums.average()
        val maxDeviation = nums.maxOf { abs(it - mean) }
        val range = (nums.maxOrNull() ?: 0.0) - (nums.minOrNull() ?: 0.0)
        if (range <= 0.001) return 1.0
        return (1.0 - (maxDeviation / range)).coerceIn(0.0, 1.0)
    }

    private fun conditionAgreement(conditions: List<WeatherCondition>): Double {
        if (conditions.isEmpty()) return 0.0
        if (conditions.size == 1) return 1.0
        val grouped = conditions.groupingBy { it }.eachCount()
        return grouped.maxOf { it.value }.toDouble() / conditions.size
    }
}
