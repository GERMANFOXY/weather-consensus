package com.weatherconsensus.ui.components.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.ConsensusSnapshot
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import kotlin.math.roundToInt

@Composable
fun SafetyScoreCard(
    current: ConsensusSnapshot,
    modifier: Modifier = Modifier,
) {
    val targetScore = (current.confidenceScore * 100).coerceIn(0.0, 100.0).toFloat()
    var animatedTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetScore) {
        animatedTarget = targetScore
    }
    val animatedScore by animateFloatAsState(
        targetValue = animatedTarget,
        animationSpec = tween(durationMillis = 1200),
        label = "safety-score",
    )

    val title = UserCopy.safetyTitle(current.confidence)
    val description = UserCopy.safetyDescription(
        confidence = current.confidence,
        outlierCount = current.statisticalOutliers.size,
    )

    PremiumGlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SafetyDonutRing(
                score = animatedScore,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PremiumColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SafetyDonutRing(
    score: Float,
    modifier: Modifier = Modifier,
) {
    val ringBrush = Brush.sweepGradient(
        colors = listOf(
            PremiumColors.AccentCyan,
            PremiumColors.AccentBlue,
            PremiumColors.AccentViolet,
            PremiumColors.AccentCyan,
        ),
    )
    val trackColor = PremiumColors.GlassBorder

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = size.minDimension * 0.12f
            val arcSize = size.minDimension - strokeWidth
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = ringBrush,
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${score.roundToInt()}%",
            color = PremiumColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
