package com.weatherconsensus.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherconsensus.data.preferences.FavoriteLocation
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun FavoriteLocationsRow(
    favorites: List<FavoriteLocation>,
    selectedLocation: GeoLocation?,
    onFavoriteSelected: (GeoLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (favorites.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        favorites.forEach { favorite ->
            val selected = selectedLocation?.let { current ->
                "%.4f".format(current.latitude) == "%.4f".format(favorite.location.latitude) &&
                    "%.4f".format(current.longitude) == "%.4f".format(favorite.location.longitude)
            } ?: false
            val bg = if (selected) {
                PremiumColors.AccentBlue.copy(alpha = 0.28f)
            } else {
                PremiumColors.GlassFill
            }
            Text(
                text = favorite.location.shortName,
                color = if (selected) PremiumColors.AccentCyan else PremiumColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .clickable { onFavoriteSelected(favorite.location) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
