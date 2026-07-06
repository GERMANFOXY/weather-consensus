package com.weatherconsensus.data.api

import com.weatherconsensus.data.api.dto.BrightSkyAlertsResponse
import com.weatherconsensus.data.api.dto.BrightSkyCurrentWeatherResponse
import com.weatherconsensus.data.api.dto.BrightSkyForecastResponse
import com.weatherconsensus.data.api.dto.OpenMeteoAirQualityResponse
import com.weatherconsensus.data.api.dto.OpenMeteoForecastResponse
import com.weatherconsensus.data.api.dto.OpenMeteoGeocodingResponse
import com.weatherconsensus.data.api.dto.OpenMeteoPollenResponse
import com.weatherconsensus.data.api.dto.OpenWeatherForecastResponse
import com.weatherconsensus.data.api.dto.OpenWeatherGeocodingResponse
import com.weatherconsensus.data.api.dto.OpenWeatherOneCallResponse
import com.weatherconsensus.data.api.dto.TomorrowForecastResponse
import com.weatherconsensus.data.api.dto.WeatherApiForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapApi {
    @GET("geo/1.0/direct")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String,
    ): List<OpenWeatherGeocodingResponse>

    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
    ): OpenWeatherOneCallResponse

    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
    ): OpenWeatherForecastResponse
}

interface WeatherApiComApi {
    @GET("v1/forecast.json")
    suspend fun forecastByCoordinates(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("days") days: Int = 7,
        @Query("aqi") aqi: String = "yes",
        @Query("alerts") alerts: String = "no",
    ): WeatherApiForecastResponse
}

interface TomorrowIoApi {
    @GET("v4/weather/forecast")
    suspend fun forecast(
        @Query("location") location: String,
        @Query("apikey") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("timesteps") timesteps: String = "1h",
    ): TomorrowForecastResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "de",
        @Query("format") format: String = "json",
    ): OpenMeteoGeocodingResponse
}

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,precipitation,weather_code,uv_index,surface_pressure,visibility",
        @Query("hourly") hourly: String =
            "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,precipitation,precipitation_probability,weather_code,uv_index,surface_pressure,visibility",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
    ): OpenMeteoForecastResponse
}

interface OpenMeteoAirQualityApi {
    @GET("v1/air-quality")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi",
    ): OpenMeteoAirQualityResponse
}

interface OpenMeteoPollenApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "grass_pollen,birch_pollen,olive_pollen",
        @Query("forecast_days") forecastDays: Int = 1,
    ): OpenMeteoPollenResponse
}

/** Bright Sky liefert DWD Open Data (Synop, MOSMIX, Warnungen) als REST-Schnittstelle. */
interface BrightSkyApi {
    @GET("current_weather")
    suspend fun currentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): BrightSkyCurrentWeatherResponse

    @GET("weather")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("date") date: String,
        @Query("last_date") lastDate: String,
    ): BrightSkyForecastResponse

    @GET("alerts")
    suspend fun alerts(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): BrightSkyAlertsResponse
}
