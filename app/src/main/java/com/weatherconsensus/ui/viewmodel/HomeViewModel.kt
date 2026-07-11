package com.weatherconsensus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherconsensus.WeatherConsensusApp
import com.weatherconsensus.data.preferences.FavoriteLocation
import com.weatherconsensus.data.repository.AllProvidersFailedException
import com.weatherconsensus.data.repository.MissingApiKeysException
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.widget.WeatherWidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class HomeUiState(
    val searchQuery: String = "",
    val searchResults: List<GeoLocation> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingWeather: Boolean = false,
    val weatherResult: WeatherConsensusResult? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val providerErrors: List<String> = emptyList(),
    val isOfflineMode: Boolean = false,
    val offlineMessage: String? = null,
    val favorites: List<FavoriteLocation> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WeatherConsensusApp
    private val repository = app.repository
    private val locationService = app.locationService

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var weatherJob: Job? = null

    init {
        viewModelScope.launch {
            app.favoriteLocationStore.favoritesFlow.collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
        viewModelScope.launch {
            val savedLocation = app.lastLocationStore.lastLocationFlow.first() ?: return@launch
            loadWeatherForLocation(savedLocation)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            runCatching { repository.searchCities(query) }
                .onSuccess { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearching = false,
                            snackbarMessage = UserCopy.ERROR_GENERIC,
                        )
                    }
                }
        }
    }

    fun selectLocation(location: GeoLocation, forceRefresh: Boolean = false) {
        loadWeatherForLocation(location, forceRefresh)
    }

    fun selectFavorite(location: GeoLocation) {
        loadWeatherForLocation(location)
    }

    fun toggleFavorite() {
        val location = _uiState.value.weatherResult?.location ?: return
        viewModelScope.launch {
            if (app.favoriteLocationStore.isFavorite(location, _uiState.value.favorites)) {
                app.favoriteLocationStore.removeFavorite(location)
                _uiState.update { it.copy(snackbarMessage = UserCopy.FAVORITE_REMOVED) }
            } else {
                app.favoriteLocationStore.addFavorite(location)
                _uiState.update { it.copy(snackbarMessage = UserCopy.FAVORITE_ADDED) }
            }
        }
    }

    fun isCurrentLocationFavorite(): Boolean {
        val location = _uiState.value.weatherResult?.location ?: return false
        return app.favoriteLocationStore.isFavorite(location, _uiState.value.favorites)
    }

    fun refresh() {
        val location = _uiState.value.weatherResult?.location ?: return
        loadWeatherForLocation(location, forceRefresh = true)
    }

    fun requestGpsLocation() {
        viewModelScope.launch {
            if (!locationService.hasLocationPermission()) {
                _uiState.update { it.copy(snackbarMessage = UserCopy.ERROR_LOCATION_PERMISSION) }
                return@launch
            }
            weatherJob?.cancel()
            _uiState.update { it.copy(isLoadingWeather = true, errorMessage = null) }
            runCatching {
                val rawLocation = locationService.getCurrentLocation()
                locationService.resolveLocationName(rawLocation)
            }.onSuccess { location ->
                loadWeatherForLocation(location)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        errorMessage = UserCopy.ERROR_LOCATION,
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun loadWeatherForLocation(location: GeoLocation, forceRefresh: Boolean = false) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            app.lastLocationStore.saveLocation(location)
            _uiState.update {
                it.copy(
                    isLoadingWeather = true,
                    errorMessage = null,
                    providerErrors = emptyList(),
                    searchResults = emptyList(),
                    searchQuery = location.displayName,
                    isOfflineMode = false,
                    offlineMessage = null,
                )
            }

            if (!forceRefresh && !app.networkMonitor.isCurrentlyOnline()) {
                repository.getStaleWeather(location)?.let { stale ->
                    presentWeather(stale, offline = true, weakNetwork = false)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        errorMessage = UserCopy.OFFLINE_NO_DATA,
                    )
                }
                return@launch
            }

            val weights = app.weightsStore.weightsFlow.first()
            runCatching { repository.fetchWeather(location, weights, forceRefresh) }
                .onSuccess { result ->
                    val failedMessages = result.providerResults.mapNotNull { it.errorMessage }
                    presentWeather(
                        result = result,
                        offline = false,
                        providerErrors = failedMessages,
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    val stale = repository.getStaleWeather(location)
                    when {
                        stale != null && (error is IOException || !app.networkMonitor.isCurrentlyOnline()) -> {
                            presentWeather(
                                result = stale,
                                offline = true,
                                weakNetwork = error is IOException && app.networkMonitor.isCurrentlyOnline(),
                            )
                        }
                        error is MissingApiKeysException || error is AllProvidersFailedException -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingWeather = false,
                                    errorMessage = UserCopy.ERROR_ALL_FAILED,
                                    providerErrors = (error as? AllProvidersFailedException)?.errors.orEmpty(),
                                )
                            }
                        }
                        error is IOException -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingWeather = false,
                                    errorMessage = UserCopy.ERROR_NETWORK,
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingWeather = false,
                                    errorMessage = UserCopy.ERROR_GENERIC,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun presentWeather(
        result: WeatherConsensusResult,
        offline: Boolean,
        weakNetwork: Boolean = false,
        providerErrors: List<String> = emptyList(),
    ) {
        WeatherWidgetUpdater.updateAll(getApplication())
        _uiState.update {
            it.copy(
                isLoadingWeather = false,
                weatherResult = result,
                providerErrors = providerErrors,
                isOfflineMode = offline,
                offlineMessage = if (offline) {
                    UserCopy.offlineDataMessage(result.fetchedAtEpochMs, result.timezoneId, weakNetwork)
                } else {
                    null
                },
                searchQuery = result.location.displayName,
                errorMessage = null,
            )
        }
    }
}
