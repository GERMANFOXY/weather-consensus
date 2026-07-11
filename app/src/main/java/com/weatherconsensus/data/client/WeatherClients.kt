package com.weatherconsensus.data.client



import com.weatherconsensus.data.api.BrightSkyApi

import com.weatherconsensus.data.api.OpenMeteoForecastApi

import com.weatherconsensus.data.api.OpenMeteoGeocodingApi

import com.weatherconsensus.data.api.OpenWeatherMapApi

import com.weatherconsensus.data.api.TomorrowIoApi

import com.weatherconsensus.data.api.WeatherApiComApi

import com.weatherconsensus.data.config.ApiKeyConfig

import com.weatherconsensus.data.network.WeatherLogger

import com.weatherconsensus.domain.location.isInGermany

import com.weatherconsensus.domain.model.GeoLocation

import com.weatherconsensus.domain.model.ProviderWeatherResult

import com.weatherconsensus.domain.model.WeatherProvider

import com.weatherconsensus.domain.normalization.DwdNormalizer

import com.weatherconsensus.domain.normalization.OpenMeteoNormalizer

import com.weatherconsensus.domain.normalization.OpenWeatherNormalizer

import com.weatherconsensus.domain.normalization.TomorrowIoNormalizer

import com.weatherconsensus.domain.normalization.WeatherApiNormalizer

import com.weatherconsensus.ui.copy.UserCopy

import kotlinx.coroutines.async

import kotlinx.coroutines.coroutineScope

import kotlinx.serialization.SerializationException

import retrofit2.HttpException

import java.io.IOException

import java.time.LocalDate

import java.time.format.DateTimeFormatter



class GeocodingClient(

    private val openMeteoGeocodingApi: OpenMeteoGeocodingApi,

    private val openWeatherMapApi: OpenWeatherMapApi,

) {

    suspend fun searchCities(query: String): List<GeoLocation> {

        val trimmed = query.trim()

        if (trimmed.isBlank()) return emptyList()



        val openMeteoResults = runCatching {

            openMeteoGeocodingApi.search(trimmed).results.orEmpty().map { result ->

                GeoLocation(

                    name = result.name,

                    latitude = result.latitude,

                    longitude = result.longitude,

                    country = result.country,

                    adminArea = result.admin1,

                )

            }

        }.onFailure { error ->

            WeatherLogger.logError(WeatherProvider.OPEN_METEO, "geocoding", error)

        }.getOrDefault(emptyList())



        if (openMeteoResults.isNotEmpty()) return openMeteoResults.distinctBy { "${it.latitude},${it.longitude}" }



        val owmKey = ApiKeyConfig.keyFor(WeatherProvider.OPENWEATHERMAP) ?: return emptyList()

        return runCatching {

            openWeatherMapApi.geocode(trimmed, apiKey = owmKey).map { result ->

                GeoLocation(

                    name = result.name,

                    latitude = result.lat,

                    longitude = result.lon,

                    country = result.country,

                    adminArea = result.state,

                )

            }

        }.getOrDefault(emptyList())

    }

}



class WeatherProviderClient(

    private val openWeatherMapApi: OpenWeatherMapApi,

    private val weatherApiComApi: WeatherApiComApi,

    private val tomorrowIoApi: TomorrowIoApi,

    private val openMeteoForecastApi: OpenMeteoForecastApi,

    private val brightSkyApi: BrightSkyApi,

) {

    suspend fun fetch(provider: WeatherProvider, location: GeoLocation): ProviderWeatherResult {

        if (provider == WeatherProvider.DWD && !location.isInGermany()) {

            return skippedResult(provider, location)

        }



        if (provider.requiresApiKey && !ApiKeyConfig.isConfigured(provider)) {

            return errorResult(

                provider,

                location,

                UserCopy.serviceUnavailableShort(provider),

            )

        }



        return try {

            when (provider) {

                WeatherProvider.OPENWEATHERMAP -> fetchOpenWeather(location)

                WeatherProvider.WEATHERAPI -> fetchWeatherApi(location)

                WeatherProvider.TOMORROW_IO -> fetchTomorrowIo(location)

                WeatherProvider.OPEN_METEO -> fetchOpenMeteo(location)

                WeatherProvider.DWD -> fetchDwd(location)

            }

        } catch (e: HttpException) {

            WeatherLogger.logError(provider, "http-${e.code()}", e)

            errorResult(provider, location, UserCopy.serviceUnavailable(provider))

        } catch (e: IOException) {

            WeatherLogger.logError(provider, "network", e)

            errorResult(provider, location, UserCopy.serviceUnavailable(provider))

        } catch (e: SerializationException) {

            WeatherLogger.logError(provider, "json-parse", e)

            errorResult(provider, location, UserCopy.serviceUnavailable(provider))

        } catch (e: Exception) {

            WeatherLogger.logError(provider, "unknown", e)

            errorResult(provider, location, UserCopy.serviceUnavailable(provider))

        }

    }



    private suspend fun fetchOpenWeather(location: GeoLocation): ProviderWeatherResult {

        val apiKey = ApiKeyConfig.keyFor(WeatherProvider.OPENWEATHERMAP)!!

        val current = openWeatherMapApi.currentWeather(location.latitude, location.longitude, apiKey)

        val forecast = openWeatherMapApi.forecast(location.latitude, location.longitude, apiKey)

        val result = OpenWeatherNormalizer.normalizeCurrent(location, current)

        val hourly = OpenWeatherNormalizer.normalizeForecast(location, forecast.list)

        val enrichedCurrent = result.current?.let { snapshot ->
            if (snapshot.precipitationProbabilityPercent == null) {
                snapshot.copy(
                    precipitationProbabilityPercent = hourly.firstOrNull()?.precipitationProbabilityPercent,
                )
            } else {
                snapshot
            }
        }

        return result.copy(current = enrichedCurrent, hourlyForecast = hourly)

    }



    private suspend fun fetchWeatherApi(location: GeoLocation): ProviderWeatherResult {

        val apiKey = ApiKeyConfig.keyFor(WeatherProvider.WEATHERAPI)!!

        val query = "${location.latitude},${location.longitude}"

        val response = weatherApiComApi.forecastByCoordinates(apiKey, query)

        return WeatherApiNormalizer.normalize(location, response)

    }



    private suspend fun fetchTomorrowIo(location: GeoLocation): ProviderWeatherResult {

        val apiKey = ApiKeyConfig.keyFor(WeatherProvider.TOMORROW_IO)!!

        val locationQuery = "${location.latitude},${location.longitude}"

        val response = tomorrowIoApi.forecast(locationQuery, apiKey)

        val entries = response.timelines?.hourly.orEmpty()

        return TomorrowIoNormalizer.normalize(location, entries)

    }



    private suspend fun fetchOpenMeteo(location: GeoLocation): ProviderWeatherResult {

        WeatherLogger.logRequest(

            WeatherProvider.OPEN_METEO,

            "forecast lat=${location.latitude}, lon=${location.longitude}",

        )

        val response = openMeteoForecastApi.forecast(location.latitude, location.longitude)

        val result = OpenMeteoNormalizer.normalize(location, response)

        WeatherLogger.logSuccess(

            WeatherProvider.OPEN_METEO,

            "temp=${result.current?.temperatureC}, hourly=${result.hourlyForecast.size}",

        )

        return result

    }



    private suspend fun fetchDwd(location: GeoLocation): ProviderWeatherResult = coroutineScope {

        WeatherLogger.logRequest(

            WeatherProvider.DWD,

            "current+forecast lat=${location.latitude}, lon=${location.longitude}",

        )

        val today = LocalDate.now()

        val lastDay = today.plusDays(6)

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE



        val currentDeferred = async {

            runCatching { brightSkyApi.currentWeather(location.latitude, location.longitude) }

        }

        val forecastDeferred = async {

            brightSkyApi.forecast(

                lat = location.latitude,

                lon = location.longitude,

                date = today.format(formatter),

                lastDate = lastDay.format(formatter),

            )

        }

        val alertsDeferred = async {
            runCatching { brightSkyApi.alerts(location.latitude, location.longitude).alerts }
        }



        val currentResponse = currentDeferred.await().getOrElse { error ->

            WeatherLogger.logError(WeatherProvider.DWD, "current_weather", error)

            throw error

        }

        val forecastResponse = forecastDeferred.await()

        val alertsResult = alertsDeferred.await()
        val warningsLoadFailed = alertsResult.isFailure
        if (warningsLoadFailed) {
            WeatherLogger.logError(WeatherProvider.DWD, "alerts", alertsResult.exceptionOrNull()!!)
        }
        val alerts = alertsResult.getOrDefault(emptyList())

        val result = DwdNormalizer.normalize(location, currentResponse, forecastResponse, alerts)
            .copy(warningsLoadFailed = warningsLoadFailed)

        WeatherLogger.logSuccess(

            WeatherProvider.DWD,

            "temp=${result.current?.temperatureC}, warnings=${result.weatherWarnings.size}",

        )

        result

    }



    private fun skippedResult(provider: WeatherProvider, location: GeoLocation): ProviderWeatherResult =

        ProviderWeatherResult(

            provider = provider,

            location = location,

            current = null,

            errorMessage = null,

        )



    private fun errorResult(

        provider: WeatherProvider,

        location: GeoLocation,

        message: String,

    ): ProviderWeatherResult = ProviderWeatherResult(

        provider = provider,

        location = location,

        current = null,

        errorMessage = message,

    )

}


