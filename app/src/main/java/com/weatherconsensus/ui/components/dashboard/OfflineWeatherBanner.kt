package com.weatherconsensus.ui.components.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun OfflineWeatherBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Text(
            text = message,
            color = PremiumColors.AccentWarm,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
