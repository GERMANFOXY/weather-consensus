package com.weatherconsensus.domain.consensus

import com.weatherconsensus.domain.model.EnsembleAgreement
import com.weatherconsensus.domain.model.NormalizedHourlyForecast
import com.weatherconsensus.domain.model.ProviderValue
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.domain.model.WeatherProvider
import java.time.Instant
import kotlin.math.abs

object ConsensusAggregation {

    const val HOURLY_BUCKET_SECONDS = 30 * 60L
    private const val MAD_MULTIPLIER = 2.5
    private const val MIN_MAD = 0.5
    const val RAIN_AGREEMENT_THRESHOLD_PERCENT = 30.0
    private const val LOW_CONFIDENCE_THRESHOLD = 0.5

    data class NumericAggregation<T : Number>(
        val filtered: List<ProviderValue<T>>,
        val outliers: Map<WeatherProvider, String>,
        val weights: ProviderWeights,
    )

    fun <T : Number> analyzeValues(
        values: List<ProviderValue<T>>,
        baseWeights: ProviderWeights,
        accuracyMultipliers: Map<WeatherProvider, Float> = emptyMap(),
    ): NumericAggregation<T> {
        if (values.isEmpty()) {
            return NumericAggregation(emptyList(), emptyMap(), baseWeights)
        }

        val outliers = mutableMapOf<WeatherProvider, String>()
        val filtered = if (values.size <= 2) {
            values
        } else {
            val doubles = values.map { it.value.toDouble() }
            val med = median(doubles)
            val mad = medianAbsoluteDeviation(doubles, med).coerceAtLeast(MIN_MAD)
            values.filter { providerValue ->
                val deviation = abs(providerValue.value.toDouble() - med)
                if (deviation > MAD_MULTIPLIER * mad) {
                    outliers[providerValue.provider] = "Wert weicht stark ab"
                    false
                } else {
                    true
                }
            }
        }

        val effective = applyDynamicDeviationWeights(filtered, baseWeights, accuracyMultipliers)
        return NumericAggregation(filtered, outliers, effective)
    }

    fun <T : Number> weightedAverage(
        values: List<ProviderValue<T>>,
        weights: ProviderWeights,
    ): Double? {
        if (values.isEmpty()) return null
        var weightedSum = 0.0
        var totalWeight = 0.0
        values.forEach { providerValue ->
            val weight = weights.weightFor(providerValue.provider).toDouble()
            weightedSum += providerValue.value.toDouble() * weight
            totalWeight += weight
        }
        return if (totalWeight > 0) weightedSum / totalWeight else null
    }

    fun weightedMax(
        values: List<ProviderValue<Double>>,
        weights: ProviderWeights,
    ): Double? {
        if (values.isEmpty()) return null
        return values.maxByOrNull { it.value * weights.weightFor(it.provider) }?.value
    }

    fun percentile75(values: List<ProviderValue<Double>>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.map { it.value }.sorted()
        val index = ((sorted.size - 1) * 0.75).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    fun conservativeMin(values: List<ProviderValue<out Number>>): Double? =
        values.minOfOrNull { it.value.toDouble() }

    fun conservativeMax(values: List<ProviderValue<out Number>>): Double? =
        values.maxOfOrNull { it.value.toDouble() }

    fun aggregatePrecipitationMm(
        aggregation: NumericAggregation<out Number>,
        confidenceScore: Double,
    ): Double? {
        val values = aggregation.filtered.map { ProviderValue(it.provider, it.value.toDouble()) }
        if (values.isEmpty()) return null
        return if (confidenceScore < LOW_CONFIDENCE_THRESHOLD) {
            conservativeMax(values)
        } else {
            weightedAverage(values, aggregation.weights)
        }
    }

    fun aggregateRainProbability(
        aggregation: NumericAggregation<out Number>,
    ): Double? {
        val values = aggregation.filtered
            .map { ProviderValue(it.provider, ProviderRainResolver.normalizePrecipPercent(it.value.toDouble())) }
            .filter { ProviderRainResolver.isMeaningfulRain(it.value) }
        if (values.isEmpty()) return null
        return weightedMax(values, aggregation.weights) ?: percentile75(values)
    }

    fun agreementScore(values: List<ProviderValue<out Number>>): Double {
        if (values.size < 2) return if (values.size == 1) 1.0 else -1.0
        val nums = values.map { it.value.toDouble() }
        val mean = nums.average()
        val maxDeviation = nums.maxOf { abs(it - mean) }
        val range = (nums.maxOrNull() ?: 0.0) - (nums.minOrNull() ?: 0.0)
        if (range <= 0.001) return 1.0
        return (1.0 - (maxDeviation / range)).coerceIn(0.0, 1.0)
    }

    fun conditionAgreement(conditions: List<WeatherCondition>): Double {
        if (conditions.isEmpty()) return 0.0
        if (conditions.size == 1) return 1.0
        val grouped = conditions.groupingBy { it }.eachCount()
        return grouped.maxOf { it.value }.toDouble() / conditions.size
    }

    fun bucketTimestamp(epochSeconds: Long): Long {
        val halfBucket = HOURLY_BUCKET_SECONDS / 2
        return ((epochSeconds + halfBucket) / HOURLY_BUCKET_SECONDS) * HOURLY_BUCKET_SECONDS
    }

    fun groupHourlyForecasts(
        successful: List<ProviderWeatherResult>,
        maxBuckets: Int = 24,
    ): Map<Long, List<ProviderValue<NormalizedHourlyForecast>>> {
        val now = Instant.now().epochSecond
        val raw = mutableMapOf<Long, MutableList<Pair<Long, ProviderValue<NormalizedHourlyForecast>>>>()

        successful.forEach { result ->
            result.hourlyForecast.forEach { hour ->
                val bucket = bucketTimestamp(hour.timestampEpochSeconds)
                raw.getOrPut(bucket) { mutableListOf() }
                    .add(hour.timestampEpochSeconds to ProviderValue(result.provider, hour))
            }
        }

        return raw
            .mapValues { (_, entries) ->
                entries
                    .groupBy { it.second.provider }
                    .map { (provider, providerEntries) ->
                        val bucket = bucketTimestamp(providerEntries.first().first)
                        providerEntries.minBy { abs(it.first - bucket) }.second
                    }
            }
            .filterKeys { it >= now - HOURLY_BUCKET_SECONDS / 2 }
            .toSortedMap()
            .entries
            .take(maxBuckets)
            .associate { it.key to it.value }
    }

    fun buildEnsembleHints(
        successful: List<ProviderWeatherResult>,
        timezoneId: String?,
        consensusCondition: WeatherCondition,
        rainProbabilityPercent: Double?,
    ): List<EnsembleAgreement> {
        if (successful.size < 2) return emptyList()

        val hints = mutableListOf<EnsembleAgreement>()
        val conditionAgree = successful.count { result ->
            ProviderRainResolver.todayDailyCondition(result, timezoneId) == consensusCondition
        }
        hints += EnsembleAgreement(
            agreeingProviders = conditionAgree,
            totalProviders = successful.size,
            type = EnsembleAgreement.TYPE_CONDITION,
            label = consensusCondition.labelDe,
        )

        val rainAgree = successful.count { result ->
            val rain = ProviderRainResolver.displayRainChance(result, timezoneId) ?: 0.0
            rain >= RAIN_AGREEMENT_THRESHOLD_PERCENT
        }
        if (rainAgree > 0 || (rainProbabilityPercent ?: 0.0) >= RAIN_AGREEMENT_THRESHOLD_PERCENT) {
            hints += EnsembleAgreement(
                agreeingProviders = rainAgree,
                totalProviders = successful.size,
                type = EnsembleAgreement.TYPE_RAIN,
                label = null,
            )
        }

        return hints
    }

    fun mergeOutlierMaps(vararg maps: Map<WeatherProvider, String>): Map<WeatherProvider, String> {
        val merged = mutableMapOf<WeatherProvider, String>()
        maps.forEach { map ->
            map.forEach { (provider, reason) ->
                merged.putIfAbsent(provider, reason)
            }
        }
        return merged
    }

    private fun <T : Number> applyDynamicDeviationWeights(
        values: List<ProviderValue<T>>,
        baseWeights: ProviderWeights,
        accuracyMultipliers: Map<WeatherProvider, Float>,
    ): ProviderWeights {
        if (values.isEmpty()) return baseWeights

        var result = baseWeights
        if (values.size == 1) {
            val provider = values.first().provider
            val accuracy = accuracyMultipliers[provider] ?: 1f
            return result.withWeight(
                provider,
                (baseWeights.weightFor(provider) * accuracy).coerceIn(0f, 2f),
            )
        }

        val nums = values.map { it.value.toDouble() }
        val med = median(nums)
        val mad = medianAbsoluteDeviation(nums, med).coerceAtLeast(MIN_MAD)
        val maxDeviation = MAD_MULTIPLIER * mad

        values.forEach { providerValue ->
            val deviation = abs(providerValue.value.toDouble() - med)
            val deviationFactor = if (maxDeviation <= 0) {
                1f
            } else {
                (1.0 - (deviation / maxDeviation)).coerceIn(0.2, 1.0).toFloat()
            }
            val accuracy = accuracyMultipliers[providerValue.provider] ?: 1f
            val weight = (baseWeights.weightFor(providerValue.provider) * deviationFactor * accuracy)
                .coerceIn(0f, 2f)
            result = result.withWeight(providerValue.provider, weight)
        }
        return result
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }

    private fun medianAbsoluteDeviation(values: List<Double>, med: Double): Double {
        val deviations = values.map { abs(it - med) }.sorted()
        return deviations[deviations.size / 2]
    }
}
