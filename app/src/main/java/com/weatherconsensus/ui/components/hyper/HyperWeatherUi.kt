package com.weatherconsensus.ui.components.hyper

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherconsensus.domain.model.ConsensusDailyForecast
import com.weatherconsensus.domain.model.ConsensusHourlyForecast
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HyperHeroSection(result: WeatherConsensusResult) {
    val snapshot = result.current
    val details = snapshot.details
    val zone = result.timezoneId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = result.location.shortName,
            color = PremiumColors.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = UserCopy.formatDateTime(result.fetchedAtEpochMs, zone),
            color = PremiumColors.TextMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = snapshot.temperatureC?.let { "${it.toInt()}°" } ?: UserCopy.NOT_AVAILABLE,
                    color = PremiumColors.TextPrimary,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-3).sp,
                    lineHeight = 88.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = snapshot.condition.labelDe,
                    color = PremiumColors.TextSecondary,
                    fontSize = 18.sp,
                )
                snapshot.feelsLikeC?.let { feels ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${UserCopy.FEELS_LIKE} ${feels.toInt()}°",
                        color = PremiumColors.TextMuted,
                        fontSize = 15.sp,
                    )
                }
            }
            AnimatedWeatherIcon(
                condition = snapshot.condition,
                size = 72.dp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = UserCopy.confidenceMessage(snapshot.confidence),
            color = PremiumColors.TextMuted.copy(alpha = 0.8f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(28.dp))
        HyperMetricsGrid(snapshot)
    }
}

@Composable
private fun HyperMetricsGrid(snapshot: ConsensusSnapshot) {
    val d = snapshot.details
    val items: List<Pair<String, String>> = listOf(
        UserCopy.RAIN to formatRain(snapshot),
        UserCopy.HUMIDITY to (snapshot.humidityPercent?.let { "${it.toInt()} %" } ?: UserCopy.NOT_AVAILABLE),
        UserCopy.WIND to (snapshot.windKmh?.let { "${it.toInt()} km/h" } ?: UserCopy.NOT_AVAILABLE),
        UserCopy.UV to (d.uvIndex?.let { "%.0f".format(it) } ?: UserCopy.NOT_AVAILABLE),
        UserCopy.PRESSURE to (d.pressureHpa?.let { "${it.toInt()} hPa" } ?: UserCopy.NOT_AVAILABLE),
        UserCopy.VISIBILITY to (d.visibilityKm?.let { "%.0f".format(it) + " km" } ?: UserCopy.NOT_AVAILABLE),
    )

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    MetricCell(label, value, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 14.dp, horizontal = 4.dp),
    ) {
        Text(label, color = PremiumColors.TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = PremiumColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Normal)
    }
}

private fun formatRain(snapshot: ConsensusSnapshot): String {
    snapshot.precipitationProbabilityPercent?.let {
        return if (it <= 0) "Keiner" else "${it.toInt()} %"
    }
    snapshot.precipitationMm?.let {
        return if (it <= 0) "Keiner" else "${"%.1f".format(it)} mm"
    }
    return UserCopy.NOT_AVAILABLE
}

@Composable
fun HyperHourlySection(hours: List<ConsensusHourlyForecast>, timezoneId: String?) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(
        timezoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault(),
    )
    Column {
        SectionTitle(UserCopy.HOURLY)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(hours) { hour ->
                PremiumGlassSurface(cornerRadius = 20.dp) {
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            formatter.format(Instant.ofEpochSecond(hour.timestampEpochSeconds)),
                            color = PremiumColors.TextMuted,
                            fontSize = 12.sp,
                        )
                        AnimatedWeatherIcon(hour.condition, size = 24.dp, animated = false)
                        Text(
                            hour.temperatureC?.let { "${it.toInt()}°" } ?: "–",
                            color = PremiumColors.TextPrimary,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HyperDailySection(days: List<ConsensusDailyForecast>, timezoneId: String?) {
    Column {
        SectionTitle(UserCopy.DAILY)
        Spacer(Modifier.height(14.dp))
        PremiumGlassSurface {
            Column(Modifier.padding(vertical = 8.dp)) {
                days.forEachIndexed { index, day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            UserCopy.formatDay(day.dateEpochSeconds, timezoneId),
                            color = if (index == 0) PremiumColors.AccentCyan else PremiumColors.TextSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.width(44.dp),
                        )
                        AnimatedWeatherIcon(day.condition, size = 28.dp, animated = false)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            day.condition.labelDe,
                            color = PremiumColors.TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${day.minTempC?.toInt() ?: "–"}° / ${day.maxTempC?.toInt() ?: "–"}°",
                            color = PremiumColors.TextPrimary,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HyperInsightCards(result: WeatherConsensusResult) {
    val details = result.current.details
    val zone = result.timezoneId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InsightCard(
            title = UserCopy.SUN_MOON,
            lines = listOf(
                "${UserCopy.SUNRISE}: ${UserCopy.formatTime(details.sunriseEpochSeconds, zone)}",
                "${UserCopy.SUNSET}: ${UserCopy.formatTime(details.sunsetEpochSeconds, zone)}",
                details.moonPhase?.let { "${UserCopy.MOON}: $it" },
            ).filterNotNull(),
            modifier = Modifier.weight(1f),
        )
        InsightCard(
            title = UserCopy.AIR_QUALITY,
            lines = listOf(
                details.airQualityLabel ?: UserCopy.NOT_AVAILABLE,
                details.airQualityIndex?.let { "Index $it" },
            ).filterNotNull(),
            modifier = Modifier.weight(1f),
        )
        InsightCard(
            title = UserCopy.POLLEN,
            lines = listOf(details.pollenLevel ?: "Keine Daten"),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InsightCard(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    PremiumGlassSurface(modifier = modifier, cornerRadius = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = PremiumColors.TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            lines.forEach { line ->
                Text(line, color = PremiumColors.TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PremiumColors.TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun AnimatedWeatherIcon(
    condition: com.weatherconsensus.domain.model.WeatherCondition,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "icon")
    val floatY by transition.animateFloat(
        0f, -6f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "float",
    )
    val alpha by transition.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )

    com.weatherconsensus.ui.components.WeatherIcon(
        condition = condition,
        size = size,
        modifier = modifier
            .then(if (animated) Modifier
                .alpha(alpha)
                .offset { IntOffset(0, floatY.toInt()) }
            else Modifier),
        glow = true,
        tint = PremiumColors.AccentCyan,
    )
}
