package com.weatherconsensus.domain.location

import com.weatherconsensus.domain.model.ConsensusDailyForecast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ForecastDateUtils {

    fun resolveZone(timezoneId: String?): ZoneId =
        timezoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    fun toLocalDate(epochSeconds: Long, timezoneId: String?): LocalDate =
        Instant.ofEpochSecond(epochSeconds).atZone(resolveZone(timezoneId)).toLocalDate()

    fun startOfDayEpoch(localDate: LocalDate, timezoneId: String?): Long =
        localDate.atStartOfDay(resolveZone(timezoneId)).toEpochSecond()

    fun isToday(epochSeconds: Long, timezoneId: String?): Boolean {
        val zone = resolveZone(timezoneId)
        return toLocalDate(epochSeconds, timezoneId) == LocalDate.now(zone)
    }

    fun upcomingDays(
        dailyForecast: List<ConsensusDailyForecast>,
        timezoneId: String?,
        maxDays: Int = 7,
    ): List<ConsensusDailyForecast> {
        val zone = resolveZone(timezoneId)
        val today = LocalDate.now(zone)
        return dailyForecast
            .sortedBy { it.dateEpochSeconds }
            .filter { Instant.ofEpochSecond(it.dateEpochSeconds).atZone(zone).toLocalDate() >= today }
            .distinctBy { Instant.ofEpochSecond(it.dateEpochSeconds).atZone(zone).toLocalDate() }
            .take(maxDays)
    }

    fun todayForecast(
        dailyForecast: List<ConsensusDailyForecast>,
        timezoneId: String?,
    ): ConsensusDailyForecast? {
        val zone = resolveZone(timezoneId)
        val today = LocalDate.now(zone)
        return dailyForecast.firstOrNull {
            Instant.ofEpochSecond(it.dateEpochSeconds).atZone(zone).toLocalDate() == today
        }
    }
}
