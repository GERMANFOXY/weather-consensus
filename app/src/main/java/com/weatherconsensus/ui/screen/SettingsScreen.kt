package com.weatherconsensus.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weatherconsensus.domain.model.ServiceTrustLevel
import com.weatherconsensus.domain.model.WeatherProvider
import com.weatherconsensus.ui.components.PremiumButton
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors
import com.weatherconsensus.ui.viewmodel.AppUpdateViewModel
import com.weatherconsensus.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

private const val UPDATE_SECTION_INDEX = 1
private const val UPDATE_MESSAGE_VISIBLE_MS = 5_000L

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    appUpdateViewModel: AppUpdateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val appUpdateState by appUpdateViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val updateStatusMessage = appUpdateState.message
    val saveMessage = uiState.saveMessage

    LaunchedEffect(updateStatusMessage) {
        if (updateStatusMessage.isNullOrBlank()) return@LaunchedEffect
        listState.animateScrollToItem(UPDATE_SECTION_INDEX)
        delay(UPDATE_MESSAGE_VISIBLE_MS)
        appUpdateViewModel.clearMessage()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = UserCopy.SETTINGS_TITLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
        }

        item {
            UpdateSectionCard(
                installedVersionName = appUpdateState.installedVersionName,
                checking = appUpdateState.checking,
                statusMessage = updateStatusMessage,
                onCheckForUpdate = { appUpdateViewModel.checkForUpdate(showNoUpdateMessage = true) },
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(
                    text = UserCopy.SERVICES_SETTINGS_TITLE,
                    color = PremiumColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = UserCopy.SERVICES_SETTINGS_HINT,
                    color = PremiumColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        items(WeatherProvider.all) { provider ->
            ServiceTrustCard(
                providerName = provider.userDisplayName,
                selectedLevel = uiState.weights.trustLevelFor(provider),
                onLevelSelected = { viewModel.updateTrustLevel(provider, it) },
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumButton(
                    text = UserCopy.SAVE,
                    onClick = viewModel::saveWeights,
                    modifier = Modifier.fillMaxWidth(),
                )
                PremiumButton(
                    text = UserCopy.RESET,
                    onClick = viewModel::resetWeights,
                    modifier = Modifier.fillMaxWidth(),
                    secondary = true,
                )
                if (!saveMessage.isNullOrBlank()) {
                    Text(
                        text = saveMessage,
                        color = PremiumColors.AccentCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        item {
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun UpdateSectionCard(
    installedVersionName: String,
    checking: Boolean,
    statusMessage: String?,
    onCheckForUpdate: () -> Unit,
) {
    PremiumGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = UserCopy.UPDATE_SECTION_TITLE,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = UserCopy.installedVersionLabel(installedVersionName),
                color = PremiumColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = UserCopy.UPDATE_SECTION_HINT,
                color = PremiumColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            PremiumButton(
                text = if (checking) UserCopy.UPDATE_CHECKING else UserCopy.UPDATE_CHECK,
                onClick = { if (!checking) onCheckForUpdate() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!statusMessage.isNullOrBlank()) {
                PremiumGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                ) {
                    Text(
                        text = statusMessage,
                        color = PremiumColors.AccentCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceTrustCard(
    providerName: String,
    selectedLevel: ServiceTrustLevel,
    onLevelSelected: (ServiceTrustLevel) -> Unit,
) {
    PremiumGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = providerName,
                color = PremiumColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ServiceTrustLevel.entries.forEach { level ->
                TrustOptionRow(
                    label = level.labelDe,
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                )
            }
        }
    }
}

@Composable
private fun TrustOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PremiumGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        cornerRadius = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) PremiumColors.AccentCyan else PremiumColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .padding(2.dp),
                ) {
                    Text(
                        text = "●",
                        color = PremiumColors.AccentCyan,
                    )
                }
            }
        }
    }
}
