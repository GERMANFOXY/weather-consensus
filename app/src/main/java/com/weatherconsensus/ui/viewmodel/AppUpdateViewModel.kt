package com.weatherconsensus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherconsensus.WeatherConsensusApp
import com.weatherconsensus.data.update.AppUpdateInfo
import com.weatherconsensus.data.update.AppUpdateRepository
import com.weatherconsensus.ui.copy.UserCopy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class AppUpdateUiState(
    val installedVersionName: String = "",
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val availableUpdate: AppUpdateInfo? = null,
    val showDialog: Boolean = false,
    val message: String? = null,
)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppUpdateRepository = (application as WeatherConsensusApp).appUpdateRepository

    private val _uiState = MutableStateFlow(
        AppUpdateUiState(installedVersionName = repository.installedVersionName),
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate(showNoUpdateMessage: Boolean = false) {
        if (_uiState.value.checking || _uiState.value.downloading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    checking = true,
                    message = null,
                    installedVersionName = repository.installedVersionName,
                )
            }
            runCatching { repository.checkForUpdate() }
                .onSuccess { update ->
                    if (update != null) {
                        _uiState.update {
                            it.copy(
                                checking = false,
                                availableUpdate = update,
                                showDialog = true,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                checking = false,
                                availableUpdate = null,
                                showDialog = false,
                                message = if (showNoUpdateMessage) UserCopy.UPDATE_UP_TO_DATE else null,
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            checking = false,
                            message = if (showNoUpdateMessage) {
                                UserCopy.UPDATE_CHECK_FAILED
                            } else {
                                null
                            },
                        )
                    }
                }
        }
    }

    fun checkSilentlyOnStart() {
        if (_uiState.value.availableUpdate != null) return
        checkForUpdate(showNoUpdateMessage = false)
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun downloadAndInstall() {
        val update = _uiState.value.availableUpdate ?: return
        if (_uiState.value.downloading) return

        if (!repository.canInstallPackages()) {
            _uiState.update { it.copy(message = UserCopy.UPDATE_INSTALL_PERMISSION) }
            repository.openInstallPermissionSettings()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloading = true,
                    downloadProgress = 0f,
                    message = null,
                )
            }
            runCatching {
                repository.downloadApk(update.apkUrl) { progress ->
                    _uiState.update { state -> state.copy(downloadProgress = progress) }
                }
            }.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        downloadProgress = 1f,
                        showDialog = false,
                    )
                }
                repository.installApk(apkFile)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        message = when (error) {
                            is IOException -> UserCopy.UPDATE_DOWNLOAD_FAILED
                            else -> UserCopy.UPDATE_GENERIC_ERROR
                        },
                    )
                }
            }
        }
    }
}
