package com.weatherconsensus.ui.theme

import androidx.compose.ui.graphics.Color

object PremiumColors {
    val BackgroundDeep = Color(0xFF050A18)
    val BackgroundCard = Color(0xFF121B2D)
    val TextPrimary = Color(0xFFF8FAFF)
    val TextSecondary = Color(0xFF9BA8C4)
    val TextMuted = Color(0xFF5C677D)
    val AccentBlue = Color(0xFF3E85F7)
    val AccentCyan = Color(0xFF00F2FF)
    val AccentCyanSoft = Color(0xFF5BC0EB)
    val AccentViolet = Color(0xFF7B6CF6)
    val AccentWarm = Color(0xFFE8A87C)
    val WarningHint = Color(0xFFFFD54F)
    val WarningOfficial = Color(0xFFFF9800)
    val WarningSevere = Color(0xFFE53935)
    val WarningExtreme = Color(0xFF7B1FA2)
    val Warning = WarningOfficial
    val GlassFill = Color(0x66121B2D)
    val GlassFillElevated = Color(0x80121B2D)
    val GlassBorder = Color(0x18FFFFFF)
    val GlassBorderBright = Color(0x403E85F7)
    val GlowBlue = Color(0x333E85F7)
    val GlowViolet = Color(0x337B6CF6)
    val IconTint = Color(0xFFB8C7FF)
    val IconGlow = Color(0x665BC0EB)

    // Weather palettes
    val ClearTop = Color(0xFF0A1628)
    val ClearBottom = Color(0xFF1A2844)
    val ClearGlow = Color(0xFFE8A87C)

    val RainTop = Color(0xFF0A1018)
    val RainBottom = Color(0xFF1A2438)
    val RainGlow = Color(0xFF4A6A8A)

    val StormTop = Color(0xFF0A0818)
    val StormBottom = Color(0xFF1A1430)
    val StormGlow = Color(0xFF6B4FA0)

    val SnowTop = Color(0xFF0C1420)
    val SnowBottom = Color(0xFF1A2A3A)
    val SnowGlow = Color(0xFF8ECAE6)

    val NightTop = Color(0xFF020408)
    val NightBottom = Color(0xFF0A1020)
    val NightGlow = Color(0xFF2A3A6A)

    val CloudTop = Color(0xFF0A0E18)
    val CloudBottom = Color(0xFF141C2A)
}

data class WeatherPalette(
    val top: Color,
    val bottom: Color,
    val glow: Color,
)

fun paletteFor(condition: com.weatherconsensus.domain.model.WeatherCondition, isNight: Boolean): WeatherPalette {
    if (isNight) return WeatherPalette(PremiumColors.NightTop, PremiumColors.NightBottom, PremiumColors.NightGlow)
    return when (condition) {
        com.weatherconsensus.domain.model.WeatherCondition.CLEAR ->
            WeatherPalette(PremiumColors.ClearTop, PremiumColors.ClearBottom, PremiumColors.ClearGlow)
        com.weatherconsensus.domain.model.WeatherCondition.RAIN,
        com.weatherconsensus.domain.model.WeatherCondition.DRIZZLE ->
            WeatherPalette(PremiumColors.RainTop, PremiumColors.RainBottom, PremiumColors.RainGlow)
        com.weatherconsensus.domain.model.WeatherCondition.THUNDERSTORM ->
            WeatherPalette(PremiumColors.StormTop, PremiumColors.StormBottom, PremiumColors.StormGlow)
        com.weatherconsensus.domain.model.WeatherCondition.SNOW ->
            WeatherPalette(PremiumColors.SnowTop, PremiumColors.SnowBottom, PremiumColors.SnowGlow)
        com.weatherconsensus.domain.model.WeatherCondition.FOG ->
            WeatherPalette(PremiumColors.CloudTop, PremiumColors.CloudBottom, PremiumColors.RainGlow)
        else ->
            WeatherPalette(PremiumColors.CloudTop, PremiumColors.CloudBottom, PremiumColors.AccentViolet)
    }
}

enum class WeatherEffect { NONE, RAIN, SNOW, FOG, CLOUDS, STARS }

fun effectFor(condition: com.weatherconsensus.domain.model.WeatherCondition, isNight: Boolean): WeatherEffect = when {
    isNight -> WeatherEffect.STARS
    condition == com.weatherconsensus.domain.model.WeatherCondition.RAIN ||
        condition == com.weatherconsensus.domain.model.WeatherCondition.DRIZZLE -> WeatherEffect.RAIN
    condition == com.weatherconsensus.domain.model.WeatherCondition.SNOW -> WeatherEffect.SNOW
    condition == com.weatherconsensus.domain.model.WeatherCondition.FOG -> WeatherEffect.FOG
    condition == com.weatherconsensus.domain.model.WeatherCondition.CLOUDY ||
        condition == com.weatherconsensus.domain.model.WeatherCondition.PARTLY_CLOUDY -> WeatherEffect.CLOUDS
    else -> WeatherEffect.NONE
}
