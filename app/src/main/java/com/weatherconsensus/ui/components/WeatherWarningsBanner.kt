package com.weatherconsensus.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WarningSeverity
import com.weatherconsensus.domain.model.WeatherWarning
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun WeatherWarningsBanner(
    warnings: List<WeatherWarning>,
    loadError: String?,
    isInGermany: Boolean,
    timezoneId: String?,
    modifier: Modifier = Modifier,
) {
    if (!isInGermany) return

    when {
        loadError != null -> SubtleWarningsMessage(loadError, modifier)
        warnings.isEmpty() -> SubtleWarningsMessage(UserCopy.WARNINGS_NONE, modifier)
        else -> ActiveWarningsBanner(warnings, timezoneId, modifier)
    }
}

@Composable
private fun SubtleWarningsMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.padding(horizontal = 4.dp),
        color = PremiumColors.TextMuted.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ActiveWarningsBanner(
    warnings: List<WeatherWarning>,
    timezoneId: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val primary = warnings.first()
    val accent = severityColor(primary.severity)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.55f), shape)
                .clickable { expanded = !expanded }
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = primary.severityLabel,
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) UserCopy.WARNINGS_HIDE_DETAILS else UserCopy.WARNINGS_SHOW_DETAILS,
                        tint = PremiumColors.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Text(
                    text = primary.hazardType,
                    color = PremiumColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = UserCopy.formatWarningPeriod(
                        primary.effectiveEpochSeconds,
                        primary.expiresEpochSeconds,
                        timezoneId,
                    ),
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )

                if (!expanded) {
                    Text(
                        text = primary.description,
                        color = PremiumColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )
                    if (warnings.size > 1) {
                        Text(
                            text = "+ ${warnings.size - 1} ${UserCopy.WARNINGS_MORE}",
                            color = accent.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                } else {
                    warnings.forEachIndexed { index, warning ->
                        if (index > 0) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(PremiumColors.GlassBorder),
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        WarningDetailCard(warning, timezoneId)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = UserCopy.WARNINGS_HIDE_DETAILS,
                        color = PremiumColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningDetailCard(warning: WeatherWarning, timezoneId: String?) {
    val accent = severityColor(warning.severity)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = warning.severityLabel,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = warning.hazardType,
            color = PremiumColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "${UserCopy.WARNINGS_PERIOD}: ${UserCopy.formatWarningPeriod(
                warning.effectiveEpochSeconds,
                warning.expiresEpochSeconds,
                timezoneId,
            )}",
            color = PremiumColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = warning.description,
            color = PremiumColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        warning.instruction?.let { instruction ->
            Text(
                text = UserCopy.WARNINGS_RECOMMENDATION,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.labelMedium,
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

private fun severityColor(severity: WarningSeverity): Color = when (severity) {
    WarningSeverity.HINWEIS -> PremiumColors.WarningHint
    WarningSeverity.WARNUNG -> PremiumColors.WarningOfficial
    WarningSeverity.UNWETTER -> PremiumColors.WarningSevere
    WarningSeverity.EXTREME_UNWETTER -> PremiumColors.WarningExtreme
    WarningSeverity.UNKNOWN -> PremiumColors.WarningHint
}
