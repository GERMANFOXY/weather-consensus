package com.weatherconsensus.domain.consensus

import com.weatherconsensus.domain.location.ForecastDateUtils
import com.weatherconsensus.domain.model.NormalizedDailyForecast
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.domain.model.WeatherCondition
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

object ProviderRainResolver {

    /**
     * All provider normalizers store rain chance as 0–100 (percent).
     * Do not multiply small integers (e.g. DWD 1 = 1%) — that wrongly became 100%.
     */
    fun normalizePrecipPercent(value: Double): Double =
        value.coerceIn(0.0, 100.0)

    private fun todayNormalizedDaily(
        dailyForecast: List<NormalizedDailyForecast>,
        timezoneId: String?,
    ): NormalizedDailyForecast? {
        val zone = ForecastDateUtils.resolveZone(timezoneId)
        val today = LocalDate.now(zone)
        return dailyForecast.firstOrNull {
            Instant.ofEpochSecond(it.dateEpochSeconds).atZone(zone).toLocalDate() == today
        }
    }

    fun todayDailyRain(
        result: ProviderWeatherResult,
        timezoneId: String?,
    ): Double? {
        val daily = todayNormalizedDaily(result.dailyForecast, timezoneId) ?: return null
        return daily.precipitationProbabilityPercent?.let(::normalizePrecipPercent)
    }

    /** Best available rain chance: today daily → nearest hour → current. */
    fun displayRainChance(
        result: ProviderWeatherResult,
        timezoneId: String?,
    ): Double? {
        todayDailyRain(result, timezoneId)?.let { return it }
        result.hourlyForecast.firstOrNull()?.precipitationProbabilityPercent?.let {
            return normalizePrecipPercent(it)
        }
        return result.current?.precipitationProbabilityPercent?.let(::normalizePrecipPercent)
    }

    fun todayDailyCondition(
        result: ProviderWeatherResult,
        timezoneId: String?,
    ): WeatherCondition? =
        todayNormalizedDaily(result.dailyForecast, timezoneId)?.condition
            ?: result.current?.condition

    fun isMeaningfulRain(percent: Double?): Boolean =
        (percent ?: 0.0).roundToInt() > 0
}
