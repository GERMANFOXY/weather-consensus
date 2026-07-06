package com.weatherconsensus.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.weatherconsensus.ui.viewmodel.HomeViewModel

/** @deprecated Use [WeatherHomeScreen] via bottom navigation. */
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    WeatherHomeScreen(
        viewModel = viewModel,
        contentPadding = PaddingValues(0.dp),
    )
}
