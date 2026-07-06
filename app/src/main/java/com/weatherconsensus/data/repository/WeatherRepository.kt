package com.weatherconsensus.data.repository

import com.weatherconsensus.data.cache.WeatherCache
import com.weatherconsensus.data.client.GeocodingClient
import com.weatherconsensus.data.client.SupplementaryWeatherClient
import com.weatherconsensus.data.client.WeatherProviderClient
import com.weatherconsensus.data.config.ApiKeyConfig
import com.weatherconsensus.domain.consensus.ConsensusEngine
import com.weatherconsensus.domain.consensus.ProviderWeightPolicy
import com.weatherconsensus.domain.location.isInGermany
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.ui.copy.UserCopy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class WeatherRepository(
    private val geocodingClient: GeocodingClient,
    private val providerClient: WeatherProviderClient,
    private val supplementaryClient: SupplementaryWeatherClient,
    private val consensusEngine: ConsensusEngine,
    private val cache: WeatherCache,
) {
    suspend fun searchCities(query: String): List<GeoLocation> =
        geocodingClient.searchCities(query)

    suspend fun fetchWeather(
        location: GeoLocation,
        weights: ProviderWeights,
        forceRefresh: Boolean = false,
    ): WeatherConsensusResult {
        if (!forceRefresh) {
            cache.get(location)?.let { return it }
        }

        val missingKeys = ApiKeyConfig.missingKeyProviders()
        val activeProviders = WeatherProvider.all.filter { provider ->
            when {
                provider == WeatherProvider.DWD -> location.isInGermany()
                provider.requiresApiKey -> ApiKeyConfig.isConfigured(provider)
                else -> true
            }
        }

        if (activeProviders.isEmpty()) {
            throw MissingApiKeysException(missingKeys)
        }

        val (providerResults, supplementary) = coroutineScope {
            val providersDeferred = activeProviders.map { provider ->
                async { providerClient.fetch(provider, location) }
            }
            val supplementaryDeferred = async { supplementaryClient.fetch(location) }
            providersDeferred.map { it.await() } to supplementaryDeferred.await()
        }

        val successfulCount = providerResults.count { it.isSuccess }
        if (successfulCount == 0) {
            throw AllProvidersFailedException(providerResults.mapNotNull { it.errorMessage })
        }

        val timezoneId = providerResults.firstNotNullOfOrNull { it.timezoneId }
        val effectiveWeights = ProviderWeightPolicy.effectiveWeights(location, weights)
        val dwdResult = providerResults.find { it.provider == WeatherProvider.DWD }
        val warnings = dwdResult?.weatherWarnings.orEmpty()
        val warningsLoadError = if (location.isInGermany() && dwdResult?.warningsLoadFailed == true) {
            UserCopy.WARNINGS_LOAD_ERROR
        } else {
            null
        }

        val result = consensusEngine.compute(
            location = location,
            providerResults = providerResults,
            weights = effectiveWeights,
            missingApiKeys = missingKeys,
            supplementaryDetails = supplementary,
            timezoneId = timezoneId,
            weatherWarnings = warnings,
            warningsLoadError = warningsLoadError,
            isInGermany = location.isInGermany(),
        )

        cache.put(location, result)
        return result
    }

    fun clearCache() = cache.clear()
}

class MissingApiKeysException(val missingProviders: List<WeatherProvider>) :
    Exception("Keine API-Keys konfiguriert")

class AllProvidersFailedException(val errors: List<String>) :
    Exception("Alle Wetterquellen fehlgeschlagen")
