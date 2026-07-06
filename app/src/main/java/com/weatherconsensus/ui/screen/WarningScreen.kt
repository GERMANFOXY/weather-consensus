package com.weatherconsensus.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WarningSeverity
import com.weatherconsensus.domain.model.WeatherWarning
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.components.WeatherLoadingState
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import com.weatherconsensus.ui.viewmodel.HomeViewModel

@Composable
fun WarningScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.weatherResult

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = UserCopy.WARNINGS_TITLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 8.dp),
            )
        }

        when {
            uiState.isLoadingWeather -> {
                item { WeatherLoadingState() }
            }
            result == null -> {
                item {
                    PremiumGlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = UserCopy.WARNINGS_NO_LOCATION,
                                color = PremiumColors.TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            !result.isInGermany -> {
                item {
                    PremiumGlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = UserCopy.WARNINGS_GERMANY_ONLY,
                            color = PremiumColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(28.dp),
                        )
                    }
                }
            }
            result.warningsLoadError != null -> {
                item {
                    PremiumGlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = result.warningsLoadError!!,
                            color = PremiumColors.TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(28.dp),
                        )
                    }
                }
            }
            result.weatherWarnings.isEmpty() -> {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 },
                    ) {
                        PremiumGlassSurface(modifier = Modifier.fillMaxWidth(), elevated = true) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = PremiumColors.TextMuted,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = UserCopy.WARNINGS_NONE,
                                    color = PremiumColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                items(result.weatherWarnings) { warning ->
                    WarningDetailCard(
                        warning = warning,
                        timezoneId = result.timezoneId,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun WarningDetailCard(
    warning: WeatherWarning,
    timezoneId: String?,
) {
    val accent = severityColor(warning.severity)
    val shape = RoundedCornerShape(24.dp)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.5f), shape)
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = warning.severityLabel,
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = warning.hazardType,
                    color = PremiumColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${UserCopy.WARNINGS_PERIOD}: ${UserCopy.formatWarningPeriod(
                        warning.effectiveEpochSeconds,
                        warning.expiresEpochSeconds,
                        timezoneId,
                    )}",
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = warning.description,
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                warning.instruction?.let { instruction ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = UserCopy.WARNINGS_RECOMMENDATION,
                        color = PremiumColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = instruction,
                        color = PremiumColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun severityColor(severity: WarningSeverity): Color = when (severity) {
    WarningSeverity.HINWEIS -> PremiumColors.WarningHint
    WarningSeverity.WARNUNG -> PremiumColors.WarningOfficial
    WarningSeverity.UNWETTER -> PremiumColors.WarningSevere
    WarningSeverity.EXTREME_UNWETTER -> PremiumColors.WarningExtreme
    WarningSeverity.UNKNOWN -> PremiumColors.WarningHint
}
