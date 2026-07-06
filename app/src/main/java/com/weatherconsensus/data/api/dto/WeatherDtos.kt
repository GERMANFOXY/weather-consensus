package com.weatherconsensus.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenWeatherGeocodingResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val state: String? = null,
)

@Serializable
data class OpenWeatherOneCallResponse(
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind? = null,
    val weather: List<OpenWeatherCondition> = emptyList(),
    val rain: OpenWeatherRain? = null,
    val snow: OpenWeatherSnow? = null,
    val visibility: Int? = null,
    val dt: Long,
    val sys: OpenWeatherSys? = null,
)

@Serializable
data class OpenWeatherSys(
    val sunrise: Long? = null,
    val sunset: Long? = null,
)

@Serializable
data class OpenWeatherMain(
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double? = null,
    val pressure: Int? = null,
    val humidity: Int,
)

@Serializable
data class OpenWeatherWind(val speed: Double)

@Serializable
data class OpenWeatherCondition(val main: String, val description: String)

@Serializable
data class OpenWeatherRain(@SerialName("1h") val oneHour: Double? = null)

@Serializable
data class OpenWeatherSnow(@SerialName("1h") val oneHour: Double? = null)

@Serializable
data class OpenWeatherForecastResponse(val list: List<OpenWeatherForecastItem> = emptyList())

@Serializable
data class OpenWeatherForecastItem(
    val dt: Long,
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind? = null,
    val weather: List<OpenWeatherCondition> = emptyList(),
    val pop: Double? = null,
    val rain: OpenWeatherRain? = null,
    val snow: OpenWeatherSnow? = null,
)

@Serializable
data class WeatherApiForecastResponse(
    val location: WeatherApiLocation,
    val current: WeatherApiCurrent,
    val forecast: WeatherApiForecast,
)

@Serializable
data class WeatherApiLocation(
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val lat: Double,
    val lon: Double,
)

@Serializable
data class WeatherApiCurrent(
    val temp_c: Double,
    val feelslike_c: Double? = null,
    val wind_kph: Double,
    val humidity: Int,
    val precip_mm: Double,
    val uv: Double? = null,
    val pressure_mb: Double? = null,
    val vis_km: Double? = null,
    val condition: WeatherApiCondition,
    val air_quality: WeatherApiAirQuality? = null,
)

@Serializable
data class WeatherApiAirQuality(
    @SerialName("us-epa-index") val usEpaIndex: Int? = null,
    @SerialName("gb-defra-index") val gbDefraIndex: Int? = null,
)

@Serializable
data class WeatherApiCondition(val text: String, val code: Int)

@Serializable
data class WeatherApiForecast(val forecastday: List<WeatherApiForecastDay> = emptyList())

@Serializable
data class WeatherApiForecastDay(
    val date: String,
    val date_epoch: Long,
    val day: WeatherApiDay,
    val hour: List<WeatherApiHour> = emptyList(),
    val astro: WeatherApiAstro? = null,
)

@Serializable
data class WeatherApiDay(
    val maxtemp_c: Double,
    val mintemp_c: Double,
    val daily_chance_of_rain: Int,
    val uv: Double? = null,
    val condition: WeatherApiCondition,
)

@Serializable
data class WeatherApiAstro(
    val sunrise: String,
    val sunset: String,
    val moon_phase: String,
    val moon_illumination: String,
)

@Serializable
data class WeatherApiHour(
    val time: String? = null,
    val time_epoch: Long,
    val temp_c: Double,
    val feelslike_c: Double? = null,
    val wind_kph: Double,
    val humidity: Int,
    val precip_mm: Double,
    val chance_of_rain: Int,
    val condition: WeatherApiCondition,
)

@Serializable
data class TomorrowForecastResponse(val timelines: TomorrowTimelines? = null)

@Serializable
data class TomorrowTimelines(val hourly: List<TomorrowTimelineEntry>? = null)

@Serializable
data class TomorrowTimelineEntry(val time: String, val values: TomorrowValues)

@Serializable
data class TomorrowValues(
    val temperature: Double? = null,
    val temperatureApparent: Double? = null,
    val windSpeed: Double? = null,
    val humidity: Double? = null,
    val rainIntensity: Double? = null,
    val precipitationProbability: Double? = null,
    val weatherCode: Int? = null,
    val uvIndex: Double? = null,
    val visibility: Double? = null,
)

@Serializable
data class OpenMeteoGeocodingResponse(val results: List<OpenMeteoGeocodingResult>? = null)

@Serializable
data class OpenMeteoGeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

@Serializable
data class OpenMeteoForecastResponse(
    val timezone: String? = null,
    val current: OpenMeteoCurrent,
    val hourly: OpenMeteoHourly,
    val daily: OpenMeteoDaily? = null,
)

@Serializable
data class OpenMeteoCurrent(
    val time: String,
    val temperature_2m: Double,
    val apparent_temperature: Double? = null,
    val relative_humidity_2m: Int,
    val wind_speed_10m: Double,
    val precipitation: Double,
    val weather_code: Int,
    val uv_index: Double? = null,
    val surface_pressure: Double? = null,
    val visibility: Double? = null,
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    val temperature_2m: List<Double> = emptyList(),
    val apparent_temperature: List<Double> = emptyList(),
    val relative_humidity_2m: List<Int> = emptyList(),
    val wind_speed_10m: List<Double> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    val precipitation_probability: List<Int> = emptyList(),
    val weather_code: List<Int> = emptyList(),
    val uv_index: List<Double> = emptyList(),
    val surface_pressure: List<Double> = emptyList(),
    val visibility: List<Double> = emptyList(),
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    val weather_code: List<Int> = emptyList(),
    val temperature_2m_max: List<Double> = emptyList(),
    val temperature_2m_min: List<Double> = emptyList(),
    val precipitation_probability_max: List<Int> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    val uv_index_max: List<Double> = emptyList(),
)

@Serializable
data class OpenMeteoAirQualityResponse(val current: OpenMeteoAirQualityCurrent? = null)

@Serializable
data class OpenMeteoAirQualityCurrent(
    val us_aqi: Int? = null,
    val european_aqi: Int? = null,
)

@Serializable
data class OpenMeteoPollenResponse(val hourly: OpenMeteoPollenHourly? = null)

@Serializable
data class OpenMeteoPollenHourly(
    val time: List<String> = emptyList(),
    val grass_pollen: List<Double> = emptyList(),
    val birch_pollen: List<Double> = emptyList(),
    val olive_pollen: List<Double> = emptyList(),
)

// Bright Sky – REST-Zugriff auf DWD Open Data (Synop, MOSMIX, Warnungen)
@Serializable
data class BrightSkyCurrentWeatherResponse(
    val weather: BrightSkyWeatherRecord? = null,
)

@Serializable
data class BrightSkyForecastResponse(
    val weather: List<BrightSkyWeatherRecord> = emptyList(),
)

@Serializable
data class BrightSkyWeatherRecord(
    val timestamp: String,
    val temperature: Double? = null,
    val relative_humidity: Int? = null,
    val wind_speed: Double? = null,
    val wind_direction: Int? = null,
    val precipitation: Double? = null,
    val precipitation_probability: Int? = null,
    val pressure_msl: Double? = null,
    val visibility: Int? = null,
    val cloud_cover: Int? = null,
    val condition: String? = null,
    val icon: String? = null,
)

@Serializable
data class BrightSkyAlertsResponse(
    val alerts: List<BrightSkyAlert> = emptyList(),
)

@Serializable
data class BrightSkyAlert(
    val event: String? = null,
    val headline: String? = null,
    val description: String? = null,
    val instruction: String? = null,
    val level: Int? = null,
    val effective: String? = null,
    val expires: String? = null,
)
