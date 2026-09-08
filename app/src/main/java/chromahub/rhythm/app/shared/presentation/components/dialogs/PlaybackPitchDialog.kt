/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import androidx.compose.runtime.Composable
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.PlaybackSpeedAndPitchBottomSheet

@Composable
fun PlaybackPitchDialog(
    currentPitch: Float,
    currentSpeed: Float = currentPitch,
    syncEnabled: Boolean = false,
    onSyncChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
    onSaveBoth: ((speed: Float, pitch: Float) -> Unit)? = null
) {
    PlaybackSpeedAndPitchBottomSheet(
        currentSpeed = currentSpeed,
        currentPitch = currentPitch,
        syncEnabled = syncEnabled,
        onSyncChange = onSyncChange,
        onDismiss = onDismiss,
        onSave = { speed, pitch ->
            if (onSaveBoth != null) {
                onSaveBoth(speed, pitch)
            } else {
                onSave(pitch)
            }
        }
    )
}
