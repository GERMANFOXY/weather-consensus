package com.weatherconsensus.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weatherconsensus.data.api.BrightSkyApi
import com.weatherconsensus.data.api.OpenMeteoAirQualityApi
import com.weatherconsensus.data.api.OpenMeteoForecastApi
import com.weatherconsensus.data.api.OpenMeteoGeocodingApi
import com.weatherconsensus.data.api.OpenMeteoPollenApi
import com.weatherconsensus.data.api.OpenWeatherMapApi
import com.weatherconsensus.data.api.TomorrowIoApi
import com.weatherconsensus.data.api.WeatherApiComApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "WeatherConsensus/1.0 (Android)")
                .build(),
        )
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            android.util.Log.d("WeatherConsensus-HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val openWeatherMapApi: OpenWeatherMapApi by lazy {
        retrofit("https://api.openweathermap.org/").create(OpenWeatherMapApi::class.java)
    }

    val weatherApiComApi: WeatherApiComApi by lazy {
        retrofit("https://api.weatherapi.com/").create(WeatherApiComApi::class.java)
    }

    val tomorrowIoApi: TomorrowIoApi by lazy {
        retrofit("https://api.tomorrow.io/").create(TomorrowIoApi::class.java)
    }

    val openMeteoGeocodingApi: OpenMeteoGeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(OpenMeteoGeocodingApi::class.java)
    }

    val openMeteoForecastApi: OpenMeteoForecastApi by lazy {
        retrofit("https://api.open-meteo.com/").create(OpenMeteoForecastApi::class.java)
    }

    val openMeteoAirQualityApi: OpenMeteoAirQualityApi by lazy {
        retrofit("https://air-quality-api.open-meteo.com/").create(OpenMeteoAirQualityApi::class.java)
    }

    val openMeteoPollenApi: OpenMeteoPollenApi by lazy {
        retrofit("https://pollen-api.open-meteo.com/").create(OpenMeteoPollenApi::class.java)
    }

    val brightSkyApi: BrightSkyApi by lazy {
        retrofit("https://api.brightsky.dev/").create(BrightSkyApi::class.java)
    }
}
