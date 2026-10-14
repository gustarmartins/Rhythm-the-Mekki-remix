package chromahub.rhythm.app.shared.presentation.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.CanvasNetworkMode
import chromahub.rhythm.app.util.MediaManagementAccess

/** Offered once after new-user onboarding, and available again from Library settings. */
@Composable
fun ArtworkDownloadSettingsDialog(appSettings: AppSettings, firstSetup: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var covers by rememberSaveable { mutableStateOf(appSettings.autoFetchArtwork.value) }
    var coverMobile by rememberSaveable { mutableStateOf(appSettings.artworkAllowMobileData.value) }
    var motion by rememberSaveable { mutableStateOf(appSettings.appleCanvasEnabled.value) }
    var motionMobile by rememberSaveable { mutableStateOf(appSettings.appleCanvasNetworkMode.value == CanvasNetworkMode.BOTH) }
    var mediaReady by remember { mutableStateOf(MediaManagementAccess.canWriteWithoutConfirmation(context)) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        mediaReady = MediaManagementAccess.canWriteWithoutConfirmation(context)
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        mediaReady = MediaManagementAccess.canWriteWithoutConfirmation(context)
        if (MediaManagementAccess.isGranted(context) && !MediaManagementAccess.hasMediaLocationAccess(context)) {
            locationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }
    fun dismiss() {
        if (firstSetup) appSettings.completeArtworkSetup()
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text(stringResource(R.string.artwork_download_choices)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.artwork_download_choices_desc))
                ArtworkChoice(stringResource(R.string.artwork_download_covers), covers) { covers = it }
                ArtworkChoice(stringResource(R.string.artwork_download_cover_mobile), coverMobile) { coverMobile = it }
                HorizontalDivider()
                ArtworkChoice(stringResource(R.string.artwork_download_motion), motion) { motion = it }
                ArtworkChoice(stringResource(R.string.artwork_download_motion_mobile), motionMobile) { motionMobile = it }
                Text(stringResource(R.string.artwork_download_wifi_note), style = MaterialTheme.typography.bodySmall)
                if (MediaManagementAccess.isAvailable()) {
                    HorizontalDivider()
                    Text(stringResource(R.string.artwork_media_access_desc), style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !mediaReady, onClick = {
                        val intent = MediaManagementAccess.createRequestIntent(context)
                        if (intent != null) runCatching { mediaLauncher.launch(intent) }.onFailure {
                            Toast.makeText(context, R.string.artwork_media_settings_unavailable, Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text(stringResource(if (mediaReady) R.string.artwork_media_access_granted else R.string.artwork_media_access_open))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                appSettings.setAutoFetchArtwork(covers)
                appSettings.setArtworkAllowMobileData(coverMobile)
                appSettings.setAppleCanvasEnabled(motion)
                appSettings.setAppleCanvasNetworkMode(if (motionMobile) CanvasNetworkMode.BOTH else CanvasNetworkMode.WIFI_ONLY)
                appSettings.completeArtworkSetup()
                onDismiss()
            }) { Text(stringResource(R.string.artwork_download_save)) }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss) {
                Text(stringResource(if (firstSetup) R.string.artwork_download_keep_defaults else android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun ArtworkChoice(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
