package com.weatherconsensus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WeatherCondition
import com.weatherconsensus.ui.theme.WeatherEffect
import com.weatherconsensus.ui.theme.effectFor
import com.weatherconsensus.ui.theme.paletteFor
import kotlin.random.Random

@Composable
fun DynamicWeatherBackground(
    condition: WeatherCondition?,
    isNight: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(condition ?: WeatherCondition.PARTLY_CLOUDY, isNight)
    val effect = effectFor(condition ?: WeatherCondition.PARTLY_CLOUDY, isNight)

    val transition = rememberInfiniteTransition(label = "bg")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(palette.top, palette.bottom))),
        )
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-50 + drift * 30).dp, y = (40 + drift * 20).dp)
                .background(palette.glow.copy(alpha = 0.12f), CircleShape),
        )
        WeatherEffectsOverlay(effect = effect, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        startY = 600f,
                    ),
                ),
        )
    }
}

@Composable
private fun WeatherEffectsOverlay(effect: WeatherEffect, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fx")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )

    when (effect) {
        WeatherEffect.RAIN -> RainEffect(modifier, phase)
        WeatherEffect.SNOW -> SnowEffect(modifier, phase)
        WeatherEffect.FOG -> FogEffect(modifier, phase)
        WeatherEffect.CLOUDS -> CloudEffect(modifier, phase)
        WeatherEffect.STARS -> StarsEffect(modifier)
        WeatherEffect.NONE -> Unit
    }
}

@Composable
private fun RainEffect(modifier: Modifier, phase: Float) {
    val drops = remember { List(40) { Random.nextFloat() } }
    Canvas(modifier) {
        drops.forEachIndexed { i, x ->
            val startX = x * size.width
            val startY = ((i * 37 + phase * size.height) % size.height)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(startX, startY),
                end = Offset(startX - 8f, startY + 24f),
                strokeWidth = 1.5f,
            )
        }
    }
}

@Composable
private fun SnowEffect(modifier: Modifier, phase: Float) {
    val flakes = remember { List(30) { Pair(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier) {
        flakes.forEachIndexed { i, (x, speed) ->
            val px = x * size.width
            val py = ((i * 50 + phase * size.height * (0.5f + speed)) % size.height)
            drawCircle(Color.White.copy(alpha = 0.15f), radius = 2f + speed * 2f, center = Offset(px, py))
        }
    }
}

@Composable
private fun FogEffect(modifier: Modifier, phase: Float) {
    Canvas(modifier) {
        drawRect(Color.White.copy(alpha = 0.04f + phase * 0.02f))
    }
}

@Composable
private fun CloudEffect(modifier: Modifier, phase: Float) {
    Canvas(modifier) {
        val y = size.height * 0.15f
        drawCircle(Color.White.copy(alpha = 0.04f), radius = 80f, center = Offset(phase * size.width * 0.3f + 100f, y))
        drawCircle(Color.White.copy(alpha = 0.03f), radius = 60f, center = Offset(phase * size.width * 0.3f + 200f, y + 20f))
    }
}

@Composable
private fun StarsEffect(modifier: Modifier) {
    val stars = remember { List(50) { Pair(Random.nextFloat(), Random.nextFloat()) } }
    val twinkle = rememberInfiniteTransition(label = "stars")
    val alpha by twinkle.animateFloat(
        0.2f, 0.7f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "tw",
    )
    Canvas(modifier) {
        stars.forEach { (x, y) ->
            drawCircle(
                Color.White.copy(alpha = alpha * (0.3f + y * 0.7f)),
                radius = 1f + x * 1.5f,
                center = Offset(x * size.width, y * size.height * 0.5f),
            )
        }
    }
}
