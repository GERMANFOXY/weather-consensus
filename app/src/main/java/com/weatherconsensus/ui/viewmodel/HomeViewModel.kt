package com.weatherconsensus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherconsensus.WeatherConsensusApp
import com.weatherconsensus.data.repository.AllProvidersFailedException
import com.weatherconsensus.data.repository.MissingApiKeysException
import com.weatherconsensus.domain.model.GeoLocation
import com.weatherconsensus.domain.model.WeatherConsensusResult
import com.weatherconsensus.ui.copy.UserCopy
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
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as WeatherConsensusApp
    private val repository = app.repository
    private val locationService = app.locationService

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingWeather = true,
                    errorMessage = null,
                    providerErrors = emptyList(),
                    searchResults = emptyList(),
                    searchQuery = location.displayName,
                )
            }
            val weights = app.weightsStore.weightsFlow.first()
            runCatching { repository.fetchWeather(location, weights, forceRefresh) }
                .onSuccess { result ->
                    val failedMessages = result.providerResults
                        .mapNotNull { it.errorMessage }
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            weatherResult = result,
                            providerErrors = failedMessages,
                            snackbarMessage = failedMessages.firstOrNull(),
                        )
                    }
                }
                .onFailure { error ->
                    when (error) {
                        is MissingApiKeysException, is AllProvidersFailedException -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingWeather = false,
                                    errorMessage = UserCopy.ERROR_ALL_FAILED,
                                    providerErrors = (error as? AllProvidersFailedException)?.errors.orEmpty(),
                                )
                            }
                        }
                        is IOException -> {
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

    fun refresh() {
        val location = _uiState.value.weatherResult?.location ?: return
        selectLocation(location, forceRefresh = true)
    }

    fun requestGpsLocation() {
        viewModelScope.launch {
            if (!locationService.hasLocationPermission()) {
                _uiState.update { it.copy(snackbarMessage = UserCopy.ERROR_LOCATION_PERMISSION) }
                return@launch
            }
            _uiState.update { it.copy(isLoadingWeather = true, errorMessage = null) }
            runCatching {
                val rawLocation = locationService.getCurrentLocation()
                locationService.resolveLocationName(rawLocation)
            }.onSuccess { location ->
                selectLocation(location)
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
}
