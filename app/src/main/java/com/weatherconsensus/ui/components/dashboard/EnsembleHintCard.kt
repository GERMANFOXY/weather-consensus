package com.weatherconsensus.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.domain.model.EnsembleAgreement
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun EnsembleHintCard(
    current: ConsensusSnapshot,
    modifier: Modifier = Modifier,
) {
    val hints = current.ensembleHints
    if (hints.isEmpty()) return

    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = UserCopy.ENSEMBLE_TITLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            hints.forEach { hint ->
                Text(
                    text = formatHint(hint),
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun formatHint(hint: EnsembleAgreement): String = when (hint.type) {
    EnsembleAgreement.TYPE_CONDITION -> UserCopy.ensembleCondition(
        agreeing = hint.agreeingProviders,
        total = hint.totalProviders,
        label = hint.label.orEmpty(),
    )
    EnsembleAgreement.TYPE_RAIN -> UserCopy.ensembleRain(
        agreeing = hint.agreeingProviders,
        total = hint.totalProviders,
    )
    else -> ""
}
