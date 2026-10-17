/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import androidx.compose.runtime.Composable
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.PlaybackSpeedAndPitchBottomSheet

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    currentPitch: Float = currentSpeed,
    syncEnabled: Boolean = false,
    showPitchControls: Boolean = true,
    onSyncChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
    onSaveBoth: ((speed: Float, pitch: Float) -> Unit)? = null
) {
    PlaybackSpeedAndPitchBottomSheet(
        currentSpeed = currentSpeed,
        currentPitch = currentPitch,
        syncEnabled = syncEnabled,
        showPitchControls = showPitchControls,
        onDismiss = onDismiss,
        onSave = { speed, pitch, synced ->
            onSyncChange(synced)
            if (onSaveBoth != null) {
                onSaveBoth(speed, pitch)
            } else {
                onSave(speed)
            }
        }
    )
}
