package com.weatherconsensus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherconsensus.WeatherConsensusApp
import com.weatherconsensus.domain.model.ProviderWeights
import com.weatherconsensus.domain.model.ServiceTrustLevel
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.ui.copy.UserCopy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val weights: ProviderWeights = ProviderWeights.default,
    val saveMessage: String? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val weightsStore = (application as WeatherConsensusApp).weightsStore

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            weightsStore.weightsFlow.collect { weights ->
                _uiState.update { it.copy(weights = weights) }
            }
        }
    }

    fun updateTrustLevel(provider: WeatherProvider, level: ServiceTrustLevel) {
        _uiState.update {
            it.copy(weights = it.weights.withTrustLevel(provider, level))
        }
    }

    fun saveWeights() {
        viewModelScope.launch {
            weightsStore.saveWeights(_uiState.value.weights)
            _uiState.update { it.copy(saveMessage = UserCopy.SAVED) }
        }
    }

    fun resetWeights() {
        viewModelScope.launch {
            weightsStore.resetToDefaults()
            _uiState.update {
                it.copy(
                    weights = ProviderWeights.default,
                    saveMessage = UserCopy.RESET_DONE,
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }
}
