package com.weatherconsensus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.weatherconsensus.MainActivity
import com.weatherconsensus.data.cache.WeatherCache
import com.weatherconsensus.ui.copy.UserCopy
import kotlin.math.roundToInt

class WeatherGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WeatherWidgetContent(context)
        }
    }
}

@Composable
private fun WeatherWidgetContent(context: Context) {
    val snapshot = WeatherCache(context.applicationContext).getLastSnapshot()
    val launchAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
    )

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF050A18)))
                .padding(16.dp)
                .clickable(launchAction),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            if (snapshot == null) {
                Text(
                    text = UserCopy.WIDGET_EMPTY,
                    style = TextStyle(color = ColorProvider(Color(0xFF9BA8C4)), fontSize = 14.sp),
                )
            } else {
                val (location, result) = snapshot
                val temp = result.current.temperatureC?.roundToInt()
                Text(
                    text = location.shortName,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9BA8C4)),
                        fontSize = 13.sp,
                    ),
                )
                Text(
                    text = temp?.let { "$it°" } ?: UserCopy.NOT_AVAILABLE,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF8FAFF)),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp),
                )
                Text(
                    text = result.current.condition.labelDe,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF00F2FF)),
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherGlanceWidget()
}
