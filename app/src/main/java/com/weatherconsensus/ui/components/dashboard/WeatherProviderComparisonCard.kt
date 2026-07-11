package com.weatherconsensus.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.consensus.ProviderRainResolver
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.components.WeatherIcon
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun WeatherProviderComparisonCard(
    providerResults: List<ProviderWeatherResult>,
    consensusTemp: Double?,
    timezoneId: String? = null,
    modifier: Modifier = Modifier,
) {
    val successful = providerResults.filter { it.isSuccess }
    if (successful.isEmpty()) return

    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = UserCopy.COMPARISON_TITLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            successful.forEachIndexed { index, result ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                ProviderComparisonRow(
                    result = result,
                    consensusTemp = consensusTemp,
                    timezoneId = timezoneId,
                )
            }
        }
    }
}

@Composable
private fun ProviderComparisonRow(
    result: ProviderWeatherResult,
    consensusTemp: Double?,
    timezoneId: String?,
) {
    val provider = result.provider
    val current = result.current
    val temp = current?.temperatureC
    val precip = ProviderRainResolver.displayRainChance(result, timezoneId) ?: 0.0
    val deviation = if (temp != null && consensusTemp != null) {
        abs(temp - consensusTemp)
    } else 0.0
    val barProgress = (precip / 100.0).coerceIn(0.0, 1.0).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(providerAccentColor(provider).copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            current?.condition?.let { condition ->
                WeatherIcon(
                    condition = condition,
                    size = 28.dp,
                    tint = providerAccentColor(provider),
                    glow = false,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.userDisplayName,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = providerSubtitle(provider),
                color = PremiumColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PremiumColors.GlassBorder),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(providerAccentColor(provider)),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = temp?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${precip.roundToInt()}%",
                color = PremiumColors.AccentBlue,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun providerAccentColor(provider: WeatherProvider): Color = when (provider) {
    WeatherProvider.DWD -> Color(0xFF3E85F7)
    WeatherProvider.OPENWEATHERMAP -> Color(0xFFFF8C42)
    WeatherProvider.WEATHERAPI -> Color(0xFF4CAF50)
    WeatherProvider.TOMORROW_IO -> Color(0xFF9C6ADE)
    WeatherProvider.OPEN_METEO -> Color(0xFF00BCD4)
}

private fun providerSubtitle(provider: WeatherProvider): String = when (provider) {
    WeatherProvider.DWD -> "DWD"
    WeatherProvider.OPENWEATHERMAP -> "OpenWeather"
    WeatherProvider.WEATHERAPI -> "WeatherAPI"
    WeatherProvider.TOMORROW_IO -> "Tomorrow.io"
    WeatherProvider.OPEN_METEO -> "Open-Meteo"
}
