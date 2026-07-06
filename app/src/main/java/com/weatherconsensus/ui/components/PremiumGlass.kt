package com.weatherconsensus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun PremiumGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    val fill = if (elevated) PremiumColors.GlassFillElevated else PremiumColors.GlassFill
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = fill,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    PremiumColors.GlassBorderBright,
                    PremiumColors.GlassBorder,
                    Color.Transparent,
                ),
            ),
        ),
        shadowElevation = if (elevated) 12.dp else 6.dp,
        tonalElevation = 0.dp,
    ) {
        Box { content() }
    }
}

@Composable
fun PremiumAccentLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        PremiumColors.AccentCyan.copy(alpha = 0.7f),
                        PremiumColors.AccentViolet.copy(alpha = 0.5f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
