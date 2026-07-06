package com.weatherconsensus.data.client

import com.weatherconsensus.data.api.OpenMeteoAirQualityApi
import com.weatherconsensus.data.api.OpenMeteoPollenApi
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.WeatherDetails
import com.weatherconsensus.domain.normalization.WeatherDetailHelpers

class SupplementaryWeatherClient(
    private val airQualityApi: OpenMeteoAirQualityApi,
    private val pollenApi: OpenMeteoPollenApi,
) {
    suspend fun fetch(location: GeoLocation): WeatherDetails {
        val airQuality = runCatching {
            airQualityApi.current(location.latitude, location.longitude).current
        }.getOrNull()

        val pollen = runCatching {
            pollenApi.forecast(location.latitude, location.longitude).hourly
        }.getOrNull()

        val zoneId = runCatching { java.time.ZoneId.of("Europe/Berlin") }.getOrDefault(java.time.ZoneId.systemDefault())
        val aqi = airQuality?.us_aqi ?: airQuality?.european_aqi
        val maxPollen = pollen?.let { hourly ->
            val idx = hourly.time.indexOfLast { time ->
                runCatching {
                    com.weatherconsensus.domain.normalization.WeatherDetailHelpers
                        .parseOpenMeteoTime(time, zoneId) <= System.currentTimeMillis() / 1000
                }.getOrDefault(true)
            }.coerceAtLeast(0)
            listOfNotNull(
                hourly.grass_pollen.getOrNull(idx),
                hourly.birch_pollen.getOrNull(idx),
                hourly.olive_pollen.getOrNull(idx),
            ).maxOrNull()
        }

        return WeatherDetails(
            airQualityIndex = aqi,
            airQualityLabel = WeatherDetailHelpers.aqiLabel(aqi),
            pollenLevel = WeatherDetailHelpers.pollenLabel(maxPollen),
        )
    }
}
