package com.weatherconsensus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weatherconsensus.ui.components.PremiumDashboardBackground
import com.weatherconsensus.ui.components.dashboard.MainTab
import com.weatherconsensus.ui.components.dashboard.PremiumBottomNav
import com.weatherconsensus.ui.components.update.AppUpdateDialog
import com.weatherconsensus.ui.screen.SettingsScreen
import com.weatherconsensus.ui.screen.WarningScreen
import com.weatherconsensus.ui.screen.WeatherHomeScreen
import com.weatherconsensus.ui.theme.WeatherConsensusTheme
import com.weatherconsensus.ui.viewmodel.AppUpdateViewModel
import com.weatherconsensus.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WeatherConsensusTheme {
                PremiumSystemBars()
                WeatherConsensusAppShell()
            }
        }
    }
}

@Composable
private fun PremiumSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as ComponentActivity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        onDispose { }
    }
}

@Composable
private fun WeatherConsensusAppShell() {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.WEATHER) }
    val homeViewModel: HomeViewModel = viewModel()
    val appUpdateViewModel: AppUpdateViewModel = viewModel()
    val appUpdateState by appUpdateViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        appUpdateViewModel.checkSilentlyOnStart()
    }

    Box(Modifier.fillMaxSize()) {
        PremiumDashboardBackground()

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                PremiumBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
        ) { padding ->
            when (selectedTab) {
                MainTab.WEATHER -> WeatherHomeScreen(
                    viewModel = homeViewModel,
                    contentPadding = padding,
                )
                MainTab.WARNINGS -> WarningScreen(
                    viewModel = homeViewModel,
                    contentPadding = padding,
                )
                MainTab.SETTINGS -> SettingsScreen(
                    contentPadding = padding,
                    appUpdateViewModel = appUpdateViewModel,
                )
            }
        }

        val availableUpdate = appUpdateState.availableUpdate
        if (appUpdateState.showDialog && availableUpdate != null) {
            AppUpdateDialog(
                update = availableUpdate,
                downloading = appUpdateState.downloading,
                downloadProgress = appUpdateState.downloadProgress,
                onDismiss = appUpdateViewModel::dismissDialog,
                onConfirm = appUpdateViewModel::downloadAndInstall,
            )
        }
    }
}
