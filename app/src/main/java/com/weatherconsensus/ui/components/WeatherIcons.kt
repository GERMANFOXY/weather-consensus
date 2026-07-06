package com.weatherconsensus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.ui.theme.PremiumColors

fun weatherIconFor(condition: WeatherCondition): ImageVector = when (condition) {
    WeatherCondition.CLEAR -> Icons.Outlined.WbSunny
    WeatherCondition.PARTLY_CLOUDY -> Icons.Outlined.CloudQueue
    WeatherCondition.CLOUDY -> Icons.Outlined.Cloud
    WeatherCondition.FOG -> Icons.Outlined.Cloud
    WeatherCondition.DRIZZLE -> Icons.Outlined.Grain
    WeatherCondition.RAIN -> Icons.Outlined.WaterDrop
    WeatherCondition.SNOW -> Icons.Outlined.AcUnit
    WeatherCondition.THUNDERSTORM -> Icons.Outlined.Thunderstorm
    WeatherCondition.UNKNOWN -> Icons.Outlined.CloudQueue
}

@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = PremiumColors.IconTint,
    glow: Boolean = true,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (glow) {
            Icon(
                imageVector = weatherIconFor(condition),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .drawBehind {
                        drawCircle(
                            color = PremiumColors.IconGlow,
                            radius = this.size.minDimension * 0.55f,
                        )
                    },
                tint = Color.Transparent,
            )
        }
        Icon(
            imageVector = weatherIconFor(condition),
            contentDescription = contentDescription ?: condition.labelDe,
            modifier = Modifier.size(size * 0.72f),
            tint = tint,
        )
    }
}
