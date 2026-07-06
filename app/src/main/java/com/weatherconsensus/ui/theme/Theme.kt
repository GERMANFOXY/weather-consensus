package com.weatherconsensus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkScheme = darkColorScheme(
    primary = PremiumColors.AccentCyan,
    onPrimary = PremiumColors.BackgroundDeep,
    primaryContainer = PremiumColors.GlassFillElevated,
    onPrimaryContainer = PremiumColors.TextPrimary,
    secondary = PremiumColors.AccentViolet,
    onSecondary = PremiumColors.TextPrimary,
    background = PremiumColors.BackgroundDeep,
    onBackground = PremiumColors.TextPrimary,
    surface = PremiumColors.GlassFill,
    onSurface = PremiumColors.TextPrimary,
    onSurfaceVariant = PremiumColors.TextSecondary,
    surfaceVariant = PremiumColors.GlassFillElevated,
    outline = PremiumColors.GlassBorder,
    outlineVariant = PremiumColors.GlassBorderBright,
    error = PremiumColors.Warning,
    onError = PremiumColors.BackgroundDeep,
    errorContainer = Color(0x33FF9F5A),
    onErrorContainer = PremiumColors.TextPrimary,
)

@Composable
fun WeatherConsensusTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PremiumDarkScheme,
        typography = WeatherTypography,
        content = content,
    )
}
