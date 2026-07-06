package com.weatherconsensus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun WeatherLoadingState(
    modifier: Modifier = Modifier,
    message: String = UserCopy.LOADING,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    PremiumGlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WeatherIcon(
                condition = WeatherCondition.PARTLY_CLOUDY,
                size = 56.dp,
                modifier = Modifier
                    .scale(pulse)
                    .alpha(alpha),
                tint = PremiumColors.AccentCyan,
            )
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.Text(
                text = message,
                color = PremiumColors.TextSecondary,
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun WeatherEmptyState(
    modifier: Modifier = Modifier,
    onUseLocation: () -> Unit,
) {
    PremiumGlassSurface(modifier = modifier.fillMaxWidth(), elevated = true) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WeatherIcon(
                condition = WeatherCondition.PARTLY_CLOUDY,
                size = 64.dp,
                tint = PremiumColors.AccentViolet,
            )
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.Text(
                text = UserCopy.EMPTY_TITLE,
                color = PremiumColors.TextPrimary,
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = UserCopy.EMPTY_SUBTITLE,
                color = PremiumColors.TextMuted,
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PremiumButton(text = UserCopy.USE_LOCATION, onClick = onUseLocation)
        }
    }
}

@Composable
fun WeatherErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Text(
                text = message,
                color = PremiumColors.TextPrimary,
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(18.dp))
            PremiumButton(text = UserCopy.RETRY, onClick = onRetry, secondary = true)
        }
    }
}
