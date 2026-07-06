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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.location.ForecastDateUtils
import com.weatherconsensus.domain.model.ConsensusDailyForecast
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.components.WeatherIcon
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import kotlin.math.roundToInt

@Composable
fun DailyForecastCard(
    dailyForecast: List<ConsensusDailyForecast>,
    timezoneId: String?,
    modifier: Modifier = Modifier,
) {
    if (dailyForecast.isEmpty()) return

    val days = ForecastDateUtils.upcomingDays(dailyForecast, timezoneId)

    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Text(
                text = UserCopy.DAILY,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            days.forEachIndexed { index, day ->
                DailyForecastRow(
                    day = day,
                    timezoneId = timezoneId,
                    isToday = ForecastDateUtils.isToday(day.dateEpochSeconds, timezoneId),
                )
                if (index < days.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(1.dp)
                            .background(PremiumColors.GlassBorder),
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    day: ConsensusDailyForecast,
    timezoneId: String?,
    isToday: Boolean,
) {
    val rowBackground = if (isToday) {
        PremiumColors.AccentBlue.copy(alpha = 0.12f)
    } else {
        PremiumColors.BackgroundCard.copy(alpha = 0f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = UserCopy.formatDailyLabel(day.dateEpochSeconds, timezoneId, isToday),
            color = if (isToday) PremiumColors.AccentCyan else PremiumColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.width(52.dp),
        )
        WeatherIcon(
            condition = day.condition,
            size = 36.dp,
            tint = if (isToday) PremiumColors.AccentWarm else PremiumColors.IconTint,
            glow = isToday,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.condition.labelDe,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            day.precipitationProbabilityPercent?.takeIf { it > 0 }?.let { rain ->
                Text(
                    text = "${rain.roundToInt()}% ${UserCopy.RAIN_CHANCE_LABEL}",
                    color = PremiumColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        DailyTempRange(
            maxTempC = day.maxTempC,
            minTempC = day.minTempC,
        )
    }
}

@Composable
private fun DailyTempRange(
    maxTempC: Double?,
    minTempC: Double?,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = maxTempC?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
            color = PremiumColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(24.dp)
                .height(1.dp)
                .background(PremiumColors.TextMuted.copy(alpha = 0.45f)),
        )
        Text(
            text = minTempC?.let { "${it.roundToInt()}°" } ?: UserCopy.NOT_AVAILABLE,
            color = PremiumColors.TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}
