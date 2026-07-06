package com.weatherconsensus.ui.components.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.weatherconsensus.data.update.AppUpdateInfo
import com.weatherconsensus.ui.components.PremiumButton
import com.weatherconsensus.ui.components.PremiumGlassSurface
import com.weatherconsensus.ui.copy.UserCopy
import com.weatherconsensus.ui.theme.PremiumColors

@Composable
fun AppUpdateDialog(
    update: AppUpdateInfo,
    downloading: Boolean,
    downloadProgress: Float,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!downloading) onDismiss() }) {
        PremiumGlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevated = true) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = UserCopy.UPDATE_AVAILABLE_TITLE,
                    color = PremiumColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = UserCopy.updateAvailableSubtitle(update.versionName),
                    color = PremiumColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (update.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = update.releaseNotes,
                        color = PremiumColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = PremiumColors.AccentCyan,
                        trackColor = PremiumColors.GlassBorder,
                    )
                    Text(
                        text = UserCopy.UPDATE_DOWNLOADING,
                        color = PremiumColors.TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    PremiumButton(
                        text = UserCopy.UPDATE_DOWNLOAD_INSTALL,
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumButton(
                        text = UserCopy.UPDATE_LATER,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        secondary = true,
                    )
                }
            }
        }
    }
}
