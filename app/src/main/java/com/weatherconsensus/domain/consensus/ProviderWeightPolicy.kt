package com.weatherconsensus.domain.consensus

import com.weatherconsensus.domain.location.isInAlpineRegion
import com.weatherconsensus.domain.location.isInGermany
import com.weatherconsensus.domain.location.isInUnitedKingdom
import com.weatherconsensus.domain.model.ConfidenceLevel
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherProvider

object ProviderWeightPolicy {
    private const val DWD_GERMANY_BOOST = 1.5f
    private const val DWD_UNCERTAINTY_BOOST = 1.2f
    private const val OPEN_METEO_ALPINE_BOOST = 1.25f
    private const val OPEN_METEO_UK_BOOST = 1.15f
    private const val MAX_WEIGHT = 2f

    fun effectiveWeights(location: GeoLocation, userWeights: ProviderWeights): ProviderWeights {
        return applyRegionalSpecialists(location, userWeights)
    }

    fun withUncertaintyDwdBoost(
        weights: ProviderWeights,
        confidence: ConfidenceLevel,
        dwdAvailable: Boolean,
    ): ProviderWeights {
        if (confidence != ConfidenceLevel.UNCERTAIN || !dwdAvailable) return weights
        val current = weights.weightFor(WeatherProvider.DWD)
        return weights.withWeight(
            WeatherProvider.DWD,
            (current * DWD_UNCERTAINTY_BOOST).coerceAtMost(MAX_WEIGHT),
        )
    }

    private fun applyRegionalSpecialists(
        location: GeoLocation,
        userWeights: ProviderWeights,
    ): ProviderWeights {
        var weights = userWeights

        if (location.isInGermany()) {
            val boosted = weights.weightFor(WeatherProvider.DWD) * DWD_GERMANY_BOOST
            weights = weights.withWeight(WeatherProvider.DWD, boosted.coerceAtMost(MAX_WEIGHT))
        }

        if (location.isInAlpineRegion()) {
            val boosted = weights.weightFor(WeatherProvider.OPEN_METEO) * OPEN_METEO_ALPINE_BOOST
            weights = weights.withWeight(WeatherProvider.OPEN_METEO, boosted.coerceAtMost(MAX_WEIGHT))
        }

        if (location.isInUnitedKingdom()) {
            val boosted = weights.weightFor(WeatherProvider.OPEN_METEO) * OPEN_METEO_UK_BOOST
            weights = weights.withWeight(WeatherProvider.OPEN_METEO, boosted.coerceAtMost(MAX_WEIGHT))
        }

        return weights
    }
}
