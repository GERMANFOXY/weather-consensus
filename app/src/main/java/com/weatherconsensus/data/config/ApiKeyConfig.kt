package com.weatherconsensus.data.config

import com.weatherconsensus.BuildConfig
import com.weatherconsensus.domain.model.ApiKeyStatus
import com.weatherconsensus.domain.model.WeatherProvider

object ApiKeyConfig {

    fun keyFor(provider: WeatherProvider): String? = when (provider) {
        WeatherProvider.OPENWEATHERMAP -> BuildConfig.OPENWEATHERMAP_API_KEY.takeIf { it.isNotBlank() }
        WeatherProvider.WEATHERAPI -> BuildConfig.WEATHERAPI_API_KEY.takeIf { it.isNotBlank() }
        WeatherProvider.TOMORROW_IO -> BuildConfig.TOMORROW_IO_API_KEY.takeIf { it.isNotBlank() }
        WeatherProvider.OPEN_METEO, WeatherProvider.DWD -> null
    }

    fun isConfigured(provider: WeatherProvider): Boolean =
        !provider.requiresApiKey || !keyFor(provider).isNullOrBlank()

    fun missingKeyProviders(): List<WeatherProvider> =
        WeatherProvider.all.filter { it.requiresApiKey && !isConfigured(it) }

    fun allStatuses(): List<ApiKeyStatus> =
        WeatherProvider.all.map { provider ->
            ApiKeyStatus(provider, isConfigured(provider))
        }
}
