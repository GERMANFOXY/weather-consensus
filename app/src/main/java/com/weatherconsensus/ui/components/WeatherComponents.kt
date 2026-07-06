package com.weatherconsensus.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherconsensus.domain.model.ConsensusHourlyForecast
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.domain.model.NormalizedWeatherSnapshot
import com.weatherconsensus.domain.model.ProviderWeatherResult
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
) {
    PremiumGlassSurface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .animateContentSize(),
        cornerRadius = 18.dp,
        elevated = !secondary,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 20.dp),
            textAlign = TextAlign.Center,
            color = if (secondary) PremiumColors.TextSecondary else PremiumColors.TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun HeroWeatherCard(
    cityName: String,
    snapshot: ConsensusSnapshot,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(modifier = modifier, elevated = true) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(
                text = cityName,
                color = PremiumColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
            Text(
                text = snapshot.condition.labelDe,
                color = PremiumColors.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snapshot.temperatureC?.let { "${it.toInt()}°" } ?: "–",
                        style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
                        color = PremiumColors.TextPrimary,
                        fontWeight = FontWeight.Thin,
                    )
                    snapshot.feelsLikeC?.let { feels ->
                        Text(
                            text = "${UserCopy.FEELS_LIKE} ${feels.toInt()}°",
                            color = PremiumColors.TextMuted,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                WeatherIcon(
                    condition = snapshot.condition,
                    size = 72.dp,
                    tint = PremiumColors.AccentCyan,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            PremiumAccentLine()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricGlassTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Umbrella,
                    label = UserCopy.RAIN,
                    value = formatRain(snapshot),
                )
                MetricGlassTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Air,
                    label = UserCopy.WIND,
                    value = snapshot.windKmh?.let { "${it.toInt()} km/h" } ?: "–",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = UserCopy.confidenceMessage(snapshot.confidence),
                color = PremiumColors.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MetricGlassTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(modifier = modifier, cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PremiumColors.AccentCyan.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = PremiumColors.TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = PremiumColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun formatRain(snapshot: ConsensusSnapshot): String {
    snapshot.precipitationProbabilityPercent?.let {
        return if (it <= 0.0) "Keiner" else "${it.toInt()} %"
    }
    snapshot.precipitationMm?.let {
        return if (it <= 0.0) "Keiner" else "${"%.1f".format(it)} mm"
    }
    return "–"
}

@Composable
fun HourlyForecastRow(
    hours: List<ConsensusHourlyForecast>,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    Column(modifier = modifier) {
        SectionLabel(UserCopy.HOURLY)
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(hours.size) { index ->
                HourlyGlassChip(hours[index], timeFormatter)
            }
        }
    }
}

@Composable
private fun HourlyGlassChip(
    hour: ConsensusHourlyForecast,
    formatter: DateTimeFormatter,
) {
    PremiumGlassSurface(cornerRadius = 22.dp) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatter.format(Instant.ofEpochSecond(hour.timestampEpochSeconds)),
                color = PremiumColors.TextMuted,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
            WeatherIcon(
                condition = hour.condition,
                size = 28.dp,
                glow = false,
                tint = PremiumColors.IconTint,
            )
            Text(
                text = hour.temperatureC?.let { "${it.toInt()}°" } ?: "–",
                color = PremiumColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun GermanyDwdHint(
    modifier: Modifier = Modifier,
) {
    Text(
        text = UserCopy.DWD_GERMANY_HINT,
        modifier = modifier.padding(horizontal = 4.dp),
        color = PremiumColors.TextMuted.copy(alpha = 0.85f),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        lineHeight = 18.sp,
    )
}

@Composable
fun ServiceComparisonSection(
    results: List<ProviderWeatherResult>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            SectionLabel(UserCopy.COMPARISON_TITLE)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = UserCopy.COMPARISON_SUBTITLE,
                color = PremiumColors.TextMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
        results.forEach { result ->
            ServiceComparisonCard(result)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = PremiumColors.TextPrimary,
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun ServiceComparisonCard(result: ProviderWeatherResult) {
    PremiumGlassSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (result.isSuccess && result.current != null) {
                WeatherIcon(
                    condition = result.current.condition,
                    size = 40.dp,
                    glow = false,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.provider.userDisplayName,
                        color = PremiumColors.TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatServiceLine(result.current),
                        color = PremiumColors.TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = result.current.temperatureC?.let { "${it.toInt()}°" } ?: "–",
                    color = PremiumColors.AccentCyan,
                    style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Light,
                )
            } else {
                Column {
                    Text(
                        text = result.provider.userDisplayName,
                        color = PremiumColors.TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = result.errorMessage ?: UserCopy.serviceUnavailableShort(result.provider),
                        color = PremiumColors.TextMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun formatServiceLine(snapshot: NormalizedWeatherSnapshot): String = buildString {
    append(snapshot.condition.labelDe)
    snapshot.windKmh?.let { append(" · Wind ${it.toInt()} km/h") }
    val rain = snapshot.precipitationProbabilityPercent?.let { "${it.toInt()} % Regen" }
        ?: snapshot.precipitationMm?.let { if (it > 0) "${"%.1f".format(it)} mm" else null }
    if (rain != null) append(" · $rain")
}

@Composable
fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(modifier = modifier, cornerRadius = 20.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = PremiumColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    color = PremiumColors.TextPrimary,
                ),
                singleLine = true,
                cursorBrush = SolidColor(PremiumColors.AccentCyan),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = UserCopy.SEARCH_PLACEHOLDER,
                            color = PremiumColors.TextMuted,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            PremiumGlassSurface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onLocationClick),
                cornerRadius = 14.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = UserCopy.USE_LOCATION,
                        tint = PremiumColors.AccentCyan,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            color = PremiumColors.TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
        )
    }
}

// Keep alias for state components
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = PremiumButton(text = text, onClick = onClick, modifier = modifier)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = PremiumGlassSurface(modifier = modifier, content = content)

@Composable
fun MainWeatherCard(
    cityName: String,
    snapshot: ConsensusSnapshot,
    modifier: Modifier = Modifier,
) = HeroWeatherCard(cityName = cityName, snapshot = snapshot, modifier = modifier)

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) = CompactSearchBar(
    query = query,
    onQueryChange = onQueryChange,
    onLocationClick = onLocationClick,
    modifier = modifier,
)
