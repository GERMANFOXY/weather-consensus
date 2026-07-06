package com.weatherconsensus.domain.normalization

import com.weatherconsensus.data.api.dto.BrightSkyAlert
import com.weatherconsensus.data.api.dto.BrightSkyCurrentWeatherResponse
import com.weatherconsensus.data.api.dto.BrightSkyForecastResponse
import com.weatherconsensus.data.api.dto.BrightSkyWeatherRecord
import com.weatherconsensus.data.api.dto.OpenMeteoForecastResponse
import com.weatherconsensus.data.api.dto.OpenWeatherForecastItem
import com.weatherconsensus.data.api.dto.OpenWeatherOneCallResponse
import com.weatherconsensus.data.api.dto.TomorrowTimelineEntry
import com.weatherconsensus.data.api.dto.WeatherApiForecastResponse
import com.weatherconsensus.data.api.dto.WeatherApiHour
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.NormalizedDailyForecast
import com.weatherconsensus.domain.model.NormalizedHourlyForecast
import com.weatherconsensus.domain.model.NormalizedWeatherSnapshot
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.domain.model.WeatherDetails
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.domain.model.WarningSeverity
import com.weatherconsensus.domain.model.WeatherWarning
import com.weatherconsensus.ui.copy.UserCopy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object WeatherDetailHelpers {
    fun aqiLabel(index: Int?): String? = when (index) {
        null -> null
        in 0..50 -> "Gut"
        in 51..100 -> "Mäßig"
        in 101..150 -> "Empfindlich"
        in 151..200 -> "Ungesund"
        else -> "Sehr ungesund"
    }

    fun pollenLabel(maxValue: Double?): String? = when {
        maxValue == null -> null
        maxValue <= 10 -> "Niedrig"
        maxValue <= 50 -> "Mäßig"
        maxValue <= 100 -> "Hoch"
        else -> "Sehr hoch"
    }

    fun parseIsoEpoch(iso: String): Long = Instant.parse(iso).epochSecond

    fun parseOpenMeteoTime(iso: String, zoneId: ZoneId): Long = runCatching {
        if (iso.contains('+') || iso.endsWith('Z')) {
            Instant.parse(iso).epochSecond
        } else if (iso.length <= 10) {
            LocalDate.parse(iso).atStartOfDay(zoneId).toEpochSecond()
        } else {
            LocalDateTime.parse(iso).atZone(zoneId).toEpochSecond()
        }
    }.getOrElse {
        throw IllegalArgumentException("Zeit konnte nicht gelesen werden: $iso (Zone: $zoneId)", it)
    }

    fun parseAstroTime(date: String, time12h: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? =
        runCatching {
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val localTime = LocalTime.parse(time12h.trim(), formatter)
            val localDate = LocalDate.parse(date)
            localDate.atTime(localTime).atZone(zoneId).toEpochSecond()
        }.getOrNull()
}

object OpenWeatherNormalizer {
    fun normalizeCurrent(location: GeoLocation, response: OpenWeatherOneCallResponse): ProviderWeatherResult {
        val precipitation = response.rain?.oneHour ?: response.snow?.oneHour ?: 0.0
        val condition = response.weather.firstOrNull()?.main?.let {
            WeatherConditionMapper.fromOpenWeatherMain(it)
        } ?: WeatherCondition.UNKNOWN

        val details = WeatherDetails(
            pressureHpa = response.main.pressure?.toDouble(),
            visibilityKm = response.visibility?.div(1000.0),
            sunriseEpochSeconds = response.sys?.sunrise,
            sunsetEpochSeconds = response.sys?.sunset,
        )

        return ProviderWeatherResult(
            provider = WeatherProvider.OPENWEATHERMAP,
            location = location,
            current = NormalizedWeatherSnapshot(
                temperatureC = response.main.temp,
                feelsLikeC = response.main.feelsLike,
                windKmh = (response.wind?.speed ?: 0.0) * 3.6,
                precipitationMm = precipitation,
                precipitationProbabilityPercent = null,
                humidityPercent = response.main.humidity.toDouble(),
                condition = condition,
                details = details,
            ),
            hourlyForecast = emptyList(),
        )
    }

    fun normalizeForecast(location: GeoLocation, items: List<OpenWeatherForecastItem>): List<NormalizedHourlyForecast> {
        val now = Instant.now().epochSecond
        return items.filter { it.dt >= now }.take(24).map { item ->
            val precipitation = item.rain?.oneHour ?: item.snow?.oneHour ?: 0.0
            NormalizedHourlyForecast(
                timestampEpochSeconds = item.dt,
                temperatureC = item.main.temp,
                feelsLikeC = item.main.feelsLike,
                windKmh = (item.wind?.speed ?: 0.0) * 3.6,
                precipitationMm = precipitation,
                precipitationProbabilityPercent = item.pop?.times(100),
                humidityPercent = item.main.humidity.toDouble(),
                condition = item.weather.firstOrNull()?.main?.let {
                    WeatherConditionMapper.fromOpenWeatherMain(it)
                } ?: WeatherCondition.UNKNOWN,
            )
        }
    }
}

object WeatherApiNormalizer {
    fun normalize(location: GeoLocation, response: WeatherApiForecastResponse): ProviderWeatherResult {
        val current = response.current
        val firstDay = response.forecast.forecastday.firstOrNull()
        val zoneId = firstDay?.hour?.firstOrNull()?.let { zoneIdFromWeatherApiHour(it) }
            ?: ZoneId.systemDefault()
        val today = response.forecast.forecastday.firstOrNull()
        val astro = today?.astro

        val details = WeatherDetails(
            uvIndex = current.uv,
            pressureHpa = current.pressure_mb,
            visibilityKm = current.vis_km,
            sunriseEpochSeconds = astro?.let {
                WeatherDetailHelpers.parseAstroTime(today.date, it.sunrise, zoneId)
            },
            sunsetEpochSeconds = astro?.let {
                WeatherDetailHelpers.parseAstroTime(today.date, it.sunset, zoneId)
            },
            moonPhase = astro?.moon_phase,
            moonIlluminationPercent = astro?.moon_illumination?.replace("%", "")?.toIntOrNull(),
            airQualityIndex = current.air_quality?.usEpaIndex,
            airQualityLabel = WeatherDetailHelpers.aqiLabel(current.air_quality?.usEpaIndex),
        )

        val hourly = response.forecast.forecastday
            .flatMap { it.hour }
            .filter { it.time_epoch >= Instant.now().epochSecond }
            .take(24)
            .map(::mapHour)

        val daily = response.forecast.forecastday.take(7).map { day ->
            NormalizedDailyForecast(
                dateEpochSeconds = WeatherDetailHelpers.parseOpenMeteoTime(day.date, zoneId),
                minTempC = day.day.mintemp_c,
                maxTempC = day.day.maxtemp_c,
                condition = WeatherConditionMapper.fromWeatherApiCode(day.day.condition.code),
                precipitationProbabilityPercent = day.day.daily_chance_of_rain.toDouble(),
                sunriseEpochSeconds = day.astro?.let {
                    WeatherDetailHelpers.parseAstroTime(day.date, it.sunrise, zoneId)
                },
                sunsetEpochSeconds = day.astro?.let {
                    WeatherDetailHelpers.parseAstroTime(day.date, it.sunset, zoneId)
                },
            )
        }

        return ProviderWeatherResult(
            provider = WeatherProvider.WEATHERAPI,
            location = location,
            current = NormalizedWeatherSnapshot(
                temperatureC = current.temp_c,
                feelsLikeC = current.feelslike_c,
                windKmh = current.wind_kph,
                precipitationMm = current.precip_mm,
                precipitationProbabilityPercent = null,
                humidityPercent = current.humidity.toDouble(),
                condition = WeatherConditionMapper.fromWeatherApiCode(current.condition.code),
                details = details,
            ),
            hourlyForecast = hourly,
            dailyForecast = daily,
            timezoneId = zoneId.id,
        )
    }

    private fun zoneIdFromWeatherApiHour(hour: WeatherApiHour): ZoneId {
        val time = hour.time ?: return ZoneId.systemDefault()
        val local = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val offsetSeconds = (hour.time_epoch - local.toEpochSecond(ZoneOffset.UTC)).toInt()
        return ZoneId.ofOffset("WeatherAPI", ZoneOffset.ofTotalSeconds(offsetSeconds))
    }

    private fun mapHour(hour: WeatherApiHour) = NormalizedHourlyForecast(
        timestampEpochSeconds = hour.time_epoch,
        temperatureC = hour.temp_c,
        feelsLikeC = hour.feelslike_c,
        windKmh = hour.wind_kph,
        precipitationMm = hour.precip_mm,
        precipitationProbabilityPercent = hour.chance_of_rain.toDouble(),
        humidityPercent = hour.humidity.toDouble(),
        condition = WeatherConditionMapper.fromWeatherApiCode(hour.condition.code),
    )
}

object TomorrowIoNormalizer {
    fun normalize(location: GeoLocation, entries: List<TomorrowTimelineEntry>): ProviderWeatherResult {
        if (entries.isEmpty()) {
            return ProviderWeatherResult(
                provider = WeatherProvider.TOMORROW_IO,
                location = location,
                current = null,
                hourlyForecast = emptyList(),
                errorMessage = UserCopy.serviceUnavailable(WeatherProvider.TOMORROW_IO),
            )
        }

        val now = Instant.now()
        val sorted = entries.sortedBy { Instant.parse(it.time) }
        val currentEntry = sorted.firstOrNull { Instant.parse(it.time) >= now } ?: sorted.first()
        val hourly = sorted.filter { Instant.parse(it.time) >= now }.take(24).map { entry ->
            val values = entry.values
            NormalizedHourlyForecast(
                timestampEpochSeconds = Instant.parse(entry.time).epochSecond,
                temperatureC = values.temperature,
                feelsLikeC = values.temperatureApparent,
                windKmh = values.windSpeed,
                precipitationMm = values.rainIntensity,
                precipitationProbabilityPercent = values.precipitationProbability,
                humidityPercent = values.humidity,
                condition = WeatherConditionMapper.fromTomorrowCode(values.weatherCode),
            )
        }

        val currentValues = currentEntry.values
        return ProviderWeatherResult(
            provider = WeatherProvider.TOMORROW_IO,
            location = location,
            current = NormalizedWeatherSnapshot(
                temperatureC = currentValues.temperature,
                feelsLikeC = currentValues.temperatureApparent,
                windKmh = currentValues.windSpeed,
                precipitationMm = currentValues.rainIntensity,
                precipitationProbabilityPercent = currentValues.precipitationProbability,
                humidityPercent = currentValues.humidity,
                condition = WeatherConditionMapper.fromTomorrowCode(currentValues.weatherCode),
                details = WeatherDetails(
                    uvIndex = currentValues.uvIndex,
                    visibilityKm = currentValues.visibility?.div(1000.0),
                ),
            ),
            hourlyForecast = hourly,
        )
    }
}

object OpenMeteoNormalizer {
    fun normalize(location: GeoLocation, response: OpenMeteoForecastResponse): ProviderWeatherResult {
        val zoneId = response.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
        val current = response.current
        val daily = response.daily
        val todaySunrise = daily?.sunrise?.firstOrNull()?.let {
            WeatherDetailHelpers.parseOpenMeteoTime(it, zoneId)
        }
        val todaySunset = daily?.sunset?.firstOrNull()?.let {
            WeatherDetailHelpers.parseOpenMeteoTime(it, zoneId)
        }

        val details = WeatherDetails(
            uvIndex = current.uv_index ?: daily?.uv_index_max?.firstOrNull(),
            pressureHpa = current.surface_pressure,
            visibilityKm = current.visibility?.div(1000.0),
            sunriseEpochSeconds = todaySunrise,
            sunsetEpochSeconds = todaySunset,
        )

        return ProviderWeatherResult(
            provider = WeatherProvider.OPEN_METEO,
            location = location,
            current = NormalizedWeatherSnapshot(
                temperatureC = current.temperature_2m,
                feelsLikeC = current.apparent_temperature,
                windKmh = current.wind_speed_10m,
                precipitationMm = current.precipitation,
                precipitationProbabilityPercent = null,
                humidityPercent = current.relative_humidity_2m.toDouble(),
                condition = WeatherConditionMapper.fromOpenMeteoCode(current.weather_code),
                details = details,
            ),
            hourlyForecast = buildHourly(response, zoneId),
            dailyForecast = buildDaily(response, zoneId),
            timezoneId = response.timezone,
        )
    }

    private fun buildHourly(response: OpenMeteoForecastResponse, zoneId: ZoneId): List<NormalizedHourlyForecast> {
        val hourly = response.hourly
        val now = Instant.now().epochSecond
        return hourly.time.mapIndexedNotNull { index, time ->
            val epoch = WeatherDetailHelpers.parseOpenMeteoTime(time, zoneId)
            if (epoch < now) return@mapIndexedNotNull null
            NormalizedHourlyForecast(
                timestampEpochSeconds = epoch,
                temperatureC = hourly.temperature_2m.getOrNull(index),
                feelsLikeC = hourly.apparent_temperature.getOrNull(index),
                windKmh = hourly.wind_speed_10m.getOrNull(index),
                precipitationMm = hourly.precipitation.getOrNull(index),
                precipitationProbabilityPercent = hourly.precipitation_probability.getOrNull(index)?.toDouble(),
                humidityPercent = hourly.relative_humidity_2m.getOrNull(index)?.toDouble(),
                condition = hourly.weather_code.getOrNull(index)?.let {
                    WeatherConditionMapper.fromOpenMeteoCode(it)
                } ?: WeatherCondition.UNKNOWN,
            )
        }.take(24)
    }

    private fun buildDaily(response: OpenMeteoForecastResponse, zoneId: ZoneId): List<NormalizedDailyForecast> {
        val daily = response.daily ?: return emptyList()
        return daily.time.mapIndexed { index, date ->
            NormalizedDailyForecast(
                dateEpochSeconds = WeatherDetailHelpers.parseOpenMeteoTime(date, zoneId),
                minTempC = daily.temperature_2m_min.getOrNull(index),
                maxTempC = daily.temperature_2m_max.getOrNull(index),
                condition = daily.weather_code.getOrNull(index)?.let {
                    WeatherConditionMapper.fromOpenMeteoCode(it)
                } ?: WeatherCondition.UNKNOWN,
                precipitationProbabilityPercent = daily.precipitation_probability_max.getOrNull(index)?.toDouble(),
                sunriseEpochSeconds = daily.sunrise.getOrNull(index)?.let {
                    WeatherDetailHelpers.parseOpenMeteoTime(it, zoneId)
                },
                sunsetEpochSeconds = daily.sunset.getOrNull(index)?.let {
                    WeatherDetailHelpers.parseOpenMeteoTime(it, zoneId)
                },
            )
        }.take(7)
    }
}

object DwdNormalizer {
    fun normalize(
        location: GeoLocation,
        currentResponse: BrightSkyCurrentWeatherResponse,
        forecastResponse: BrightSkyForecastResponse,
        alerts: List<BrightSkyAlert>,
    ): ProviderWeatherResult {
        val currentRecord = currentResponse.weather
            ?: forecastResponse.weather.firstOrNull()
            ?: throw IllegalStateException("Keine DWD-Wetterdaten verfügbar")

        val zoneId = ZoneId.of("Europe/Berlin")
        val hourly = buildHourly(forecastResponse.weather, zoneId)
        val daily = buildDaily(forecastResponse.weather, zoneId)
        val warnings = normalizeAlerts(alerts)

        val details = WeatherDetails(
            pressureHpa = currentRecord.pressure_msl,
            visibilityKm = currentRecord.visibility?.div(1000.0),
        )

        return ProviderWeatherResult(
            provider = WeatherProvider.DWD,
            location = location,
            current = NormalizedWeatherSnapshot(
                temperatureC = currentRecord.temperature,
                windKmh = currentRecord.wind_speed?.times(3.6),
                precipitationMm = currentRecord.precipitation,
                precipitationProbabilityPercent = currentRecord.precipitation_probability?.toDouble(),
                humidityPercent = currentRecord.relative_humidity?.toDouble(),
                condition = WeatherConditionMapper.fromDwdRecord(currentRecord),
                details = details,
            ),
            hourlyForecast = hourly,
            dailyForecast = daily,
            timezoneId = zoneId.id,
            weatherWarnings = warnings,
        )
    }

    private fun buildHourly(records: List<BrightSkyWeatherRecord>, zoneId: ZoneId): List<NormalizedHourlyForecast> {
        val now = Instant.now().epochSecond
        return records.mapNotNull { record ->
            val epoch = WeatherDetailHelpers.parseIsoEpoch(record.timestamp)
            if (epoch < now) return@mapNotNull null
            NormalizedHourlyForecast(
                timestampEpochSeconds = epoch,
                temperatureC = record.temperature,
                windKmh = record.wind_speed?.times(3.6),
                precipitationMm = record.precipitation,
                precipitationProbabilityPercent = record.precipitation_probability?.toDouble(),
                humidityPercent = record.relative_humidity?.toDouble(),
                condition = WeatherConditionMapper.fromDwdRecord(record),
            )
        }.take(24)
    }

    private fun buildDaily(records: List<BrightSkyWeatherRecord>, zoneId: ZoneId): List<NormalizedDailyForecast> {
        return records
            .groupBy { Instant.parse(it.timestamp).atZone(zoneId).toLocalDate() }
            .entries
            .sortedBy { it.key }
            .take(7)
            .map { (date, dayRecords) ->
                val temps = dayRecords.mapNotNull { it.temperature }
                val precipProb = dayRecords.mapNotNull { it.precipitation_probability }.maxOrNull()
                NormalizedDailyForecast(
                    dateEpochSeconds = date.atStartOfDay(zoneId).toEpochSecond(),
                    minTempC = temps.minOrNull(),
                    maxTempC = temps.maxOrNull(),
                    condition = WeatherConditionMapper.consensusCondition(
                        dayRecords.map { WeatherConditionMapper.fromDwdRecord(it) },
                    ),
                    precipitationProbabilityPercent = precipProb?.toDouble(),
                )
            }
    }

    fun normalizeAlerts(alerts: List<BrightSkyAlert>): List<WeatherWarning> =
        alerts.mapNotNull { toWarning(it) }
            .filter { it.isActive() }
            .sortedByDescending { it.severity.priority }

    private fun toWarning(alert: BrightSkyAlert): WeatherWarning? {
        val hazardType = formatHazardType(alert)
        val description = alert.description?.trim()?.takeIf { it.isNotBlank() }
            ?: alert.headline?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        val severity = WarningSeverity.fromDwdLevel(alert.level)

        return WeatherWarning(
            hazardType = hazardType,
            severity = severity,
            description = description,
            instruction = alert.instruction?.trim()?.takeIf { it.isNotBlank() },
            effectiveEpochSeconds = alert.effective?.let {
                runCatching { WeatherDetailHelpers.parseIsoEpoch(it) }.getOrNull()
            },
            expiresEpochSeconds = alert.expires?.let {
                runCatching { WeatherDetailHelpers.parseIsoEpoch(it) }.getOrNull()
            },
        )
    }

    private fun formatHazardType(alert: BrightSkyAlert): String {
        alert.event?.trim()?.takeIf { it.isNotBlank() }?.let { event ->
            return event.replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
        alert.headline?.trim()?.takeIf { it.isNotBlank() }?.let { headline ->
            val cleaned = headline
                .removePrefix("Amtliche WARNUNG vor ")
                .removePrefix("Amtliche Wetterwarnung vor ")
                .removePrefix("Wetterwarnung: ")
                .trim()
            if (cleaned.isNotBlank()) return cleaned
        }
        return "Wetterwarnung"
    }
}
