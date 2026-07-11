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
import com.weatherconsensus.domain.location.ForecastDateUtils
import com.weatherconsensus.domain.normalization.WeatherConditionMapper
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

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
        accuracyMultipliers: Map<WeatherProvider, Float> = emptyMap(),
    ): WeatherConsensusResult {
        val successful = providerResults.filter { it.isSuccess }
        val dwdAvailable = successful.any { it.provider == WeatherProvider.DWD }
        val currentSnapshots = successful.mapNotNull { result ->
            result.current?.let { ProviderValue(result.provider, it) }
        }

        var currentConsensus = buildSnapshot(
            values = currentSnapshots,
            weights = weights,
            allResults = providerResults,
            supplementary = supplementaryDetails,
            accuracyMultipliers = accuracyMultipliers,
        )

        if (isInGermany && dwdAvailable && currentConsensus.confidence == ConfidenceLevel.UNCERTAIN) {
            val boostedWeights = ProviderWeightPolicy.withUncertaintyDwdBoost(
                weights = weights,
                confidence = currentConsensus.confidence,
                dwdAvailable = true,
            )
            currentConsensus = buildSnapshot(
                values = currentSnapshots,
                weights = boostedWeights,
                allResults = providerResults,
                supplementary = supplementaryDetails,
                accuracyMultipliers = accuracyMultipliers,
            )
        }

        val hourlyGroups = ConsensusAggregation.groupHourlyForecasts(successful)
        val hourlyConsensus = hourlyGroups.map { (timestamp, entries) ->
            buildHourlySnapshot(
                timestamp = timestamp,
                values = entries,
                weights = weights,
                accuracyMultipliers = accuracyMultipliers,
            )
        }

        val dailyConsensus = buildDailyConsensus(
            successful = successful,
            weights = weights,
            timezoneId = timezoneId,
            accuracyMultipliers = accuracyMultipliers,
        )

        currentConsensus = enrichCurrentRainChance(
            current = currentConsensus,
            hourly = hourlyConsensus,
            daily = dailyConsensus,
        )

        currentConsensus = currentConsensus.copy(
            ensembleHints = ConsensusAggregation.buildEnsembleHints(
                successful = successful,
                timezoneId = timezoneId,
                consensusCondition = currentConsensus.condition,
                rainProbabilityPercent = currentConsensus.precipitationProbabilityPercent,
            ),
        )

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

    private fun enrichCurrentRainChance(
        current: ConsensusSnapshot,
        hourly: List<ConsensusHourlyForecast>,
        daily: List<ConsensusDailyForecast>,
    ): ConsensusSnapshot {
        val direct = current.precipitationProbabilityPercent?.let(ProviderRainResolver::normalizePrecipPercent)
        val nearestHourly = hourly.firstOrNull()?.precipitationProbabilityPercent?.let(ProviderRainResolver::normalizePrecipPercent)
        val todayDaily = daily.firstOrNull()?.precipitationProbabilityPercent?.let(ProviderRainResolver::normalizePrecipPercent)

        val resolved = sequenceOf(nearestHourly, direct, todayDaily)
            .filterNotNull()
            .firstOrNull { it.roundToInt() > 0 }
            ?: todayDaily
            ?: nearestHourly
            ?: direct

        val updated = if (resolved != null && resolved != direct) {
            current.copy(precipitationProbabilityPercent = resolved)
        } else {
            current
        }

        return updated
    }

    private fun buildDailyConsensus(
        successful: List<ProviderWeatherResult>,
        weights: ProviderWeights,
        timezoneId: String?,
        accuracyMultipliers: Map<WeatherProvider, Float>,
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
                    accuracyMultipliers = accuracyMultipliers,
                )
            }
    }

    private fun buildSnapshot(
        values: List<ProviderValue<NormalizedWeatherSnapshot>>,
        weights: ProviderWeights,
        allResults: List<ProviderWeatherResult>,
        supplementary: WeatherDetails,
        accuracyMultipliers: Map<WeatherProvider, Float>,
    ): ConsensusSnapshot {
        val unavailable = allResults
            .filter { !it.isSuccess }
            .associate { it.provider to (it.errorMessage ?: "Gerade nicht verfügbar") }

        val tempAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.temperatureC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val windAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.windKmh?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val precipAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.precipitationMm?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val precipProbAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv ->
                pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
            },
            weights,
            accuracyMultipliers,
        )
        val humidityAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.humidityPercent?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val feelsLikeAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.feelsLikeC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )

        val statisticalOutliers = ConsensusAggregation.mergeOutlierMaps(
            tempAgg.outliers,
            windAgg.outliers,
            precipAgg.outliers,
            precipProbAgg.outliers,
            humidityAgg.outliers,
            feelsLikeAgg.outliers,
        )

        val numericScores = listOf(
            ConsensusAggregation.agreementScore(tempAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(windAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(precipAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(humidityAgg.filtered.map { ProviderValue(it.provider, it.value) }),
        ).filter { it >= 0 }

        val conditionValues = values.map { ProviderValue(it.provider, it.value.condition) }
        val conditionAgreement = ConsensusAggregation.conditionAgreement(conditionValues.map { it.value })
        val confidenceScore = if (numericScores.isEmpty()) {
            conditionAgreement
        } else {
            (numericScores.average() * 0.8) + (conditionAgreement * 0.2)
        }

        val mergedDetails = mergeDetails(
            values.map { it.value.details },
            supplementary,
            tempAgg.weights,
            values.map { ProviderValue(it.provider, it.value.details) },
            accuracyMultipliers,
        )

        val condition = WeatherConditionMapper.weightedConsensusCondition(conditionValues, tempAgg.weights)
        val rainProbability = ConsensusAggregation.aggregateRainProbability(precipProbAgg)

        return ConsensusSnapshot(
            temperatureC = ConsensusAggregation.weightedAverage(tempAgg.filtered, tempAgg.weights),
            feelsLikeC = ConsensusAggregation.weightedAverage(feelsLikeAgg.filtered, feelsLikeAgg.weights),
            windKmh = ConsensusAggregation.weightedAverage(windAgg.filtered, windAgg.weights),
            precipitationMm = ConsensusAggregation.aggregatePrecipitationMm(precipAgg, confidenceScore),
            precipitationProbabilityPercent = rainProbability,
            humidityPercent = ConsensusAggregation.weightedAverage(humidityAgg.filtered, humidityAgg.weights),
            condition = condition,
            confidence = ConfidenceLevel.fromScore(confidenceScore),
            confidenceScore = confidenceScore,
            details = mergedDetails,
            providerContributions = values.associate { it.provider to it.value },
            excludedProviders = unavailable,
            statisticalOutliers = statisticalOutliers,
            ensembleHints = emptyList(),
        )
    }

    private fun mergeDetails(
        providerDetails: List<WeatherDetails>,
        supplementary: WeatherDetails,
        weights: ProviderWeights,
        weighted: List<ProviderValue<WeatherDetails>>,
        accuracyMultipliers: Map<WeatherProvider, Float>,
    ): WeatherDetails {
        fun avg(selector: (WeatherDetails) -> Double?): Double? {
            val vals = weighted.mapNotNull { pv ->
                selector(pv.value)?.let { ProviderValue(pv.provider, it) }
            }
            val aggregation = ConsensusAggregation.analyzeValues(vals, weights, accuracyMultipliers)
            return ConsensusAggregation.weightedAverage(aggregation.filtered, aggregation.weights)
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
        accuracyMultipliers: Map<WeatherProvider, Float>,
    ): ConsensusDailyForecast {
        val minTemps = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.minTempC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val maxTemps = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.maxTempC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val precipProb = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv ->
                pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
            },
            weights,
            accuracyMultipliers,
        )

        val minValues = minTemps.filtered.map { ProviderValue(it.provider, it.value) }
        val maxValues = maxTemps.filtered.map { ProviderValue(it.provider, it.value) }

        return ConsensusDailyForecast(
            dateEpochSeconds = date,
            minTempC = ConsensusAggregation.conservativeMin(minValues),
            maxTempC = ConsensusAggregation.conservativeMax(maxValues),
            condition = WeatherConditionMapper.weightedConsensusCondition(
                values.map { ProviderValue(it.provider, it.value.condition) },
                weights,
            ),
            precipitationProbabilityPercent = ConsensusAggregation.aggregateRainProbability(precipProb),
            sunriseEpochSeconds = values.firstNotNullOfOrNull { it.value.sunriseEpochSeconds },
            sunsetEpochSeconds = values.firstNotNullOfOrNull { it.value.sunsetEpochSeconds },
        )
    }

    private fun buildHourlySnapshot(
        timestamp: Long,
        values: List<ProviderValue<NormalizedHourlyForecast>>,
        weights: ProviderWeights,
        accuracyMultipliers: Map<WeatherProvider, Float>,
    ): ConsensusHourlyForecast {
        val tempAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.temperatureC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val windAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.windKmh?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val precipAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.precipitationMm?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val precipProbAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv ->
                pv.value.precipitationProbabilityPercent?.let { ProviderValue(pv.provider, it) }
            },
            weights,
            accuracyMultipliers,
        )
        val humidityAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.humidityPercent?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )
        val feelsLikeAgg = ConsensusAggregation.analyzeValues(
            values.mapNotNull { pv -> pv.value.feelsLikeC?.let { ProviderValue(pv.provider, it) } },
            weights,
            accuracyMultipliers,
        )

        val scores = listOf(
            ConsensusAggregation.agreementScore(tempAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(windAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(precipAgg.filtered.map { ProviderValue(it.provider, it.value) }),
            ConsensusAggregation.agreementScore(humidityAgg.filtered.map { ProviderValue(it.provider, it.value) }),
        ).filter { it >= 0 }

        val conditionValues = values.map { ProviderValue(it.provider, it.value.condition) }
        val conditionAgreement = ConsensusAggregation.conditionAgreement(conditionValues.map { it.value })
        val confidenceScore = if (scores.isEmpty()) {
            conditionAgreement
        } else {
            (scores.average() * 0.8) + (conditionAgreement * 0.2)
        }

        return ConsensusHourlyForecast(
            timestampEpochSeconds = timestamp,
            temperatureC = ConsensusAggregation.weightedAverage(tempAgg.filtered, tempAgg.weights),
            feelsLikeC = ConsensusAggregation.weightedAverage(feelsLikeAgg.filtered, feelsLikeAgg.weights),
            windKmh = ConsensusAggregation.weightedAverage(windAgg.filtered, windAgg.weights),
            precipitationMm = ConsensusAggregation.aggregatePrecipitationMm(precipAgg, confidenceScore),
            precipitationProbabilityPercent = ConsensusAggregation.aggregateRainProbability(precipProbAgg),
            humidityPercent = ConsensusAggregation.weightedAverage(humidityAgg.filtered, humidityAgg.weights),
            condition = WeatherConditionMapper.weightedConsensusCondition(conditionValues, tempAgg.weights),
            confidence = ConfidenceLevel.fromScore(confidenceScore),
            providerContributions = values.associate { it.provider to it.value },
        )
    }
}
