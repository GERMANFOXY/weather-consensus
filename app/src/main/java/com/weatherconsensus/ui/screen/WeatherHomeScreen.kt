package com.weatherconsensus.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.domain.location.ForecastDateUtils
import com.weatherconsensus.ui.components.CompactSearchBar
import com.weatherconsensus.ui.components.SearchResultChip
import com.weatherconsensus.ui.components.WeatherEmptyState
import com.weatherconsensus.ui.components.WeatherErrorState
import com.weatherconsensus.ui.components.WeatherLoadingState
import com.weatherconsensus.ui.components.dashboard.DashboardTopBar
import com.weatherconsensus.ui.components.dashboard.FavoriteLocationsRow
import com.weatherconsensus.ui.components.dashboard.OfflineWeatherBanner
import com.weatherconsensus.ui.components.dashboard.DailyForecastCard
import com.weatherconsensus.ui.components.dashboard.EnsembleHintCard
import com.weatherconsensus.ui.components.dashboard.HeroWeatherCard
import com.weatherconsensus.ui.components.dashboard.HourlyForecastCard
import com.weatherconsensus.ui.components.dashboard.SafetyScoreCard
import com.weatherconsensus.ui.components.dashboard.WeatherProviderComparisonCard
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import com.weatherconsensus.ui.viewmodel.HomeViewModel

@Composable
fun WeatherHomeScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.requestGpsLocation()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val result = uiState.weatherResult

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box {
                DashboardTopBar(
                    locationName = result?.location?.shortName,
                    dateLabel = result?.let {
                        UserCopy.formatTodayDate(it.fetchedAtEpochMs, it.timezoneId)
                    },
                    onMenuClick = { showMenu = true },
                    onSearchClick = { showSearch = !showSearch },
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (result != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (viewModel.isCurrentLocationFavorite()) {
                                        UserCopy.REMOVE_FAVORITE
                                    } else {
                                        UserCopy.ADD_FAVORITE
                                    },
                                    color = PremiumColors.TextPrimary,
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.toggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    if (viewModel.isCurrentLocationFavorite()) {
                                        Icons.Outlined.Star
                                    } else {
                                        Icons.Outlined.StarOutline
                                    },
                                    null,
                                    tint = PremiumColors.AccentWarm,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(UserCopy.REFRESH_WEATHER, color = PremiumColors.TextPrimary)
                            },
                            onClick = {
                                showMenu = false
                                viewModel.refresh()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Refresh, null, tint = PremiumColors.TextSecondary)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(UserCopy.USE_LOCATION, color = PremiumColors.TextPrimary)
                        },
                        onClick = {
                            showMenu = false
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, null, tint = PremiumColors.TextSecondary)
                        },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { data ->
                Snackbar(
                    data,
                    containerColor = PremiumColors.GlassFillElevated,
                    contentColor = PremiumColors.TextPrimary,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (uiState.favorites.isNotEmpty()) {
                    item {
                        FavoriteLocationsRow(
                            favorites = uiState.favorites,
                            selectedLocation = result?.location,
                            onFavoriteSelected = { viewModel.selectFavorite(it) },
                        )
                    }
                }

                if (uiState.isOfflineMode && uiState.offlineMessage != null) {
                    item {
                        OfflineWeatherBanner(message = uiState.offlineMessage!!)
                    }
                }

                if (showSearch || result == null) {
                    item {
                        CompactSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onLocationClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                        )
                    }
                }

                if (uiState.isSearching) {
                    item { WeatherLoadingState(message = UserCopy.SEARCHING) }
                }

                if (uiState.searchResults.isNotEmpty()) {
                    items(uiState.searchResults) { loc ->
                        SearchResultChip(
                            label = loc.displayName,
                            onClick = {
                                showSearch = false
                                viewModel.selectLocation(loc)
                            },
                        )
                    }
                }

                if (uiState.isLoadingWeather) {
                    item { WeatherLoadingState() }
                }

                if (uiState.errorMessage != null && result == null && !uiState.isLoadingWeather) {
                    item {
                        WeatherErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = {
                                viewModel.clearError()
                                if (uiState.searchQuery.isNotBlank()) {
                                    uiState.searchResults.firstOrNull()?.let { viewModel.selectLocation(it) }
                                        ?: viewModel.onSearchQueryChange(uiState.searchQuery)
                                }
                            },
                        )
                    }
                }

                if (!uiState.isLoadingWeather && result == null &&
                    uiState.searchResults.isEmpty() && uiState.errorMessage == null
                ) {
                    item {
                        WeatherEmptyState {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    }
                }

                result?.let { weather ->
                    item {
                        DashboardWeatherContent(weather)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DashboardWeatherContent(result: WeatherConsensusResult) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 6 },
        exit = fadeOut(tween(300)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val todayForecast = ForecastDateUtils.todayForecast(result.dailyForecast, result.timezoneId)
            HeroWeatherCard(
                current = result.current,
                condition = result.current.condition,
                maxTempC = todayForecast?.maxTempC,
                minTempC = todayForecast?.minTempC,
                rainChancePercent = todayForecast?.precipitationProbabilityPercent
                    ?: result.hourlyForecast.firstOrNull()?.precipitationProbabilityPercent
                    ?: result.current.precipitationProbabilityPercent,
            )
            SafetyScoreCard(current = result.current)
            EnsembleHintCard(current = result.current)
            WeatherProviderComparisonCard(
                providerResults = result.providerResults,
                consensusTemp = result.current.temperatureC,
                timezoneId = result.timezoneId,
            )
            if (result.hourlyForecast.isNotEmpty()) {
                HourlyForecastCard(
                    hourlyForecast = result.hourlyForecast,
                    timezoneId = result.timezoneId,
                )
            }
            if (result.dailyForecast.isNotEmpty()) {
                DailyForecastCard(
                    dailyForecast = result.dailyForecast,
                    timezoneId = result.timezoneId,
                )
            }
        }
    }
}
