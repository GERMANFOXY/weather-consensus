package com.weatherconsensus.domain.normalization

import com.weatherconsensus.domain.model.WeatherCondition

object WeatherConditionMapper {

    fun fromOpenWeatherMain(main: String): WeatherCondition = when (main.uppercase()) {
        "CLEAR" -> WeatherCondition.CLEAR
        "CLOUDS" -> WeatherCondition.CLOUDY
        "RAIN" -> WeatherCondition.RAIN
        "DRIZZLE" -> WeatherCondition.DRIZZLE
        "THUNDERSTORM" -> WeatherCondition.THUNDERSTORM
        "SNOW" -> WeatherCondition.SNOW
        "MIST", "FOG", "HAZE" -> WeatherCondition.FOG
        else -> WeatherCondition.UNKNOWN
    }

    fun fromWeatherApiCode(code: Int): WeatherCondition = when (code) {
        1000 -> WeatherCondition.CLEAR
        1003 -> WeatherCondition.PARTLY_CLOUDY
        1006, 1009 -> WeatherCondition.CLOUDY
        1030, 1135, 1147 -> WeatherCondition.FOG
        1063, 1150, 1153, 1180, 1183 -> WeatherCondition.DRIZZLE
        1186, 1189, 1192, 1195, 1240, 1243, 1246 -> WeatherCondition.RAIN
        1066, 1114, 1117, 1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258 -> WeatherCondition.SNOW
        1087, 1273, 1276, 1279, 1282 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    fun fromTomorrowCode(code: Int?): WeatherCondition = when (code) {
        1000 -> WeatherCondition.CLEAR
        1100, 1101 -> WeatherCondition.PARTLY_CLOUDY
        1102, 1001 -> WeatherCondition.CLOUDY
        2000, 2100 -> WeatherCondition.FOG
        4000, 4200 -> WeatherCondition.DRIZZLE
        4001, 4201 -> WeatherCondition.RAIN
        5000, 5001, 5100, 5101 -> WeatherCondition.SNOW
        8000 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    fun fromOpenMeteoCode(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.FOG
        51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    fun fromDwdRecord(record: com.weatherconsensus.data.api.dto.BrightSkyWeatherRecord): WeatherCondition {
        record.icon?.let { icon ->
            when {
                icon.contains("thunder") -> return WeatherCondition.THUNDERSTORM
                icon.contains("snow") -> return WeatherCondition.SNOW
                icon.contains("rain") || icon.contains("sleet") -> return WeatherCondition.RAIN
                icon.contains("fog") -> return WeatherCondition.FOG
                icon.contains("clear") -> return WeatherCondition.CLEAR
                icon.contains("partly") -> return WeatherCondition.PARTLY_CLOUDY
                icon.contains("cloud") -> return WeatherCondition.CLOUDY
            }
        }
        return when (record.condition?.lowercase()) {
            "dry" -> if ((record.cloud_cover ?: 0) >= 80) WeatherCondition.CLOUDY else WeatherCondition.CLEAR
            "fog" -> WeatherCondition.FOG
            "rain", "sleet", "hail" -> WeatherCondition.RAIN
            "snow" -> WeatherCondition.SNOW
            "thunderstorm" -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.UNKNOWN
        }
    }

    fun consensusCondition(conditions: List<WeatherCondition>): WeatherCondition {
        if (conditions.isEmpty()) return WeatherCondition.UNKNOWN
        return conditions
            .filter { it != WeatherCondition.UNKNOWN }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: conditions.first()
    }
}
