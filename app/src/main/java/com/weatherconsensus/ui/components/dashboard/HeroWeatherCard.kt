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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.components.WeatherIcon
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import kotlin.math.roundToInt

@Composable
fun HeroWeatherCard(
    current: ConsensusSnapshot,
    condition: WeatherCondition,
    maxTempC: Double? = null,
    minTempC: Double? = null,
    rainChancePercent: Double? = null,
    modifier: Modifier = Modifier,
) {
    val displayRainChance = rainChancePercent ?: current.precipitationProbabilityPercent

    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp, elevated = true) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = UserCopy.CURRENT,
                color = PremiumColors.TextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (maxTempC != null || minTempC != null) {
                            DailyMinMaxColumn(maxTempC = maxTempC, minTempC = minTempC)
                            Spacer(Modifier.width(16.dp))
                        }
                        Text(
                            text = formatTemp(current.temperatureC),
                            color = PremiumColors.TextPrimary,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-2).sp,
                            lineHeight = 72.sp,
                        )
                    }
                    Text(
                        text = condition.labelDe,
                        color = PremiumColors.TextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    current.feelsLikeC?.let { feels ->
                        Text(
                            text = "${UserCopy.FEELS_LIKE} ${feels.roundToInt()}°",
                            color = PremiumColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                WeatherIcon(
                    condition = condition,
                    size = 88.dp,
                    tint = PremiumColors.AccentWarm,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HeroMetric(
                    icon = Icons.Outlined.WaterDrop,
                    value = formatPercent(displayRainChance),
                    label = UserCopy.RAIN_CHANCE,
                )
                HeroMetric(
                    icon = Icons.Outlined.Air,
                    value = formatWind(current.windKmh),
                    label = UserCopy.WIND,
                )
                HeroMetric(
                    icon = Icons.Outlined.Opacity,
                    value = formatPercent(current.humidityPercent),
                    label = UserCopy.HUMIDITY,
                )
                HeroMetric(
                    icon = Icons.Outlined.Speed,
                    value = formatPressure(current.details.pressureHpa),
                    label = UserCopy.PRESSURE,
                )
            }
        }
    }
}

@Composable
private fun DailyMinMaxColumn(
    maxTempC: Double?,
    minTempC: Double?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = maxTempC?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
            color = PremiumColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .width(28.dp)
                .height(1.dp)
                .background(PremiumColors.TextMuted.copy(alpha = 0.5f)),
        )
        Text(
            text = minTempC?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
            color = PremiumColors.TextMuted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroMetric(
    icon: ImageVector,
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumColors.AccentBlue,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = PremiumColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            color = PremiumColors.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun formatTemp(temp: Double?): String =
    temp?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE

private fun formatPercent(value: Double?): String =
    value?.let { "${it.roundToInt()}%" } ?: UserCopy.NOT_AVAILABLE

private fun formatWind(kmh: Double?): String =
    kmh?.let { "${it.roundToInt()} km/h" } ?: UserCopy.NOT_AVAILABLE

private fun formatPressure(hpa: Double?): String =
    hpa?.let { "${it.roundToInt()} hPa" } ?: UserCopy.NOT_AVAILABLE
