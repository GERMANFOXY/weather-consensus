package com.weatherconsensus.domain.consensus

import com.weatherconsensus.domain.location.isInGermany
import com.weatherconsensus.domain.model.ConfidenceLevel
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherProvider

object ProviderWeightPolicy {
    private const val DWD_GERMANY_BOOST = 1.5f
    private const val DWD_UNCERTAINTY_BOOST = 1.2f

    fun effectiveWeights(location: GeoLocation, userWeights: ProviderWeights): ProviderWeights {
        if (!location.isInGermany()) return userWeights
        val boosted = userWeights.weightFor(WeatherProvider.DWD) * DWD_GERMANY_BOOST
        return userWeights.withWeight(WeatherProvider.DWD, boosted.coerceAtMost(2f))
    }

    fun withUncertaintyDwdBoost(
        weights: ProviderWeights,
        confidence: ConfidenceLevel,
        dwdAvailable: Boolean,
    ): ProviderWeights {
        if (confidence != ConfidenceLevel.UNCERTAIN || !dwdAvailable) return weights
        val current = weights.weightFor(WeatherProvider.DWD)
        return weights.withWeight(WeatherProvider.DWD, (current * DWD_UNCERTAINTY_BOOST).coerceAtMost(2f))
    }
}
