package com.weatherconsensus.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.ConsensusHourlyForecast
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.components.WeatherIcon
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import kotlin.math.roundToInt

@Composable
fun HourlyForecastCard(
    hourlyForecast: List<ConsensusHourlyForecast>,
    timezoneId: String?,
    modifier: Modifier = Modifier,
) {
    if (hourlyForecast.isEmpty()) return

    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                text = UserCopy.HOURLY_OVERVIEW,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                hourlyForecast.take(24).forEachIndexed { index, hour ->
                    HourlyChip(
                        label = if (index == 0) UserCopy.NOW else UserCopy.formatTime(
                            hour.timestampEpochSeconds,
                            timezoneId,
                        ),
                        temperature = hour.temperatureC,
                        condition = hour.condition,
                        highlighted = index == 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyChip(
    label: String,
    temperature: Double?,
    condition: com.weatherconsensus.domain.model.WeatherCondition,
    highlighted: Boolean,
) {
    val bgColor = if (highlighted) {
        PremiumColors.AccentBlue.copy(alpha = 0.25f)
    } else {
        PremiumColors.GlassFill
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            text = label,
            color = if (highlighted) PremiumColors.AccentCyan else PremiumColors.TextMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(10.dp))
        WeatherIcon(
            condition = condition,
            size = 28.dp,
            tint = if (highlighted) PremiumColors.AccentWarm else PremiumColors.IconTint,
            glow = highlighted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = temperature?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
            color = PremiumColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
