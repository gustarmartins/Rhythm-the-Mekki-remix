package chromahub.rhythm.app.shared.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings

/** Capture the selected output on open so a route change cannot edit a different profile. */
@Composable
internal fun ListeningControlsDialog(appSettings: AppSettings, onDismiss: () -> Unit) {
    val device = remember { appSettings.currentBluetoothDisplayDevice.value }
    val initial = remember { appSettings.effectiveBluetoothLyricsProfile(device) }
    val displayLyrics by appSettings.bluetoothLyricsEnabled.collectAsState()
    val playerLyrics by appSettings.showLyrics.collectAsState()
    val integrations by appSettings.integrationsEnabled.collectAsState()
    var showTextMode by remember { mutableStateOf(false) }
    BluetoothLyricsTuningDialog(
        currentOffsetMs = initial.offsetMs,
        deviceLabel = device?.name,
        currentMaxChunkChars = initial.maxChunkChars,
        currentScrollCharsPerSecond = initial.scrollCharsPerSecond,
        currentMinChunkHoldMs = initial.minChunkHoldMs,
        currentMetadataUpdateIntervalMs = initial.metadataUpdateIntervalMs,
        onApply = { offset, width, scroll, hold, interval ->
            val latest = appSettings.effectiveBluetoothLyricsProfile(device)
            appSettings.setBluetoothLyricsProfile(device, latest.copy(
                offsetMs = offset, maxChunkChars = width, scrollCharsPerSecond = scroll,
                minChunkHoldMs = hold, metadataUpdateIntervalMs = interval
            ))
        },
        onDismiss = onDismiss,
        quickControls = {
            ListeningToggle(stringResource(R.string.bluetooth_lyrics_enabled), displayLyrics,
                appSettings::setBluetoothLyricsEnabled)
            ListeningToggle(stringResource(R.string.settings_show_lyrics), playerLyrics,
                appSettings::setShowLyrics)
            ListeningToggle(stringResource(R.string.exp_third_party_integrations), integrations,
                appSettings::setIntegrationsEnabled)
            TextButton(onClick = { showTextMode = true }) {
                Text(stringResource(R.string.bluetooth_lyrics_text_mode))
            }
        }
    )
    if (showTextMode) {
        BluetoothLyricsTextModeDialog(
            currentMode = appSettings.effectiveBluetoothLyricsProfile(device).textMode,
            onSelect = { mode ->
                appSettings.setBluetoothLyricsProfile(device,
                    appSettings.effectiveBluetoothLyricsProfile(device).copy(textMode = mode))
            },
            onDismiss = { showTextMode = false }
        )
    }
}

@Composable
private fun ListeningToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
