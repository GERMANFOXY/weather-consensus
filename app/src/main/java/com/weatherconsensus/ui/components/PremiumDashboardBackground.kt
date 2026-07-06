package com.weatherconsensus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun PremiumDashboardBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dashboard-bg")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PremiumColors.BackgroundDeep,
                            Color(0xFF0A1228),
                            PremiumColors.BackgroundDeep,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(320.dp)
                .offset(x = (-80 + drift * 40).dp, y = (60 + drift * 30).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PremiumColors.GlowBlue,
                            PremiumColors.GlowBlue.copy(alpha = 0f),
                        ),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (200 - drift * 30).dp, y = (300 + drift * 20).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PremiumColors.GlowViolet,
                            PremiumColors.GlowViolet.copy(alpha = 0f),
                        ),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(200.dp)
                .offset(x = (40 + drift * 20).dp, y = (520 - drift * 15).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PremiumColors.AccentCyan.copy(alpha = 0.12f),
                            PremiumColors.AccentCyan.copy(alpha = 0f),
                        ),
                    ),
                    CircleShape,
                ),
        )
    }
}
