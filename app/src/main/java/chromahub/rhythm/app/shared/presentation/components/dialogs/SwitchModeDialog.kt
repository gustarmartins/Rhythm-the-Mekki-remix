/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun SwitchModeDialog(
    targetMode: String, // "LOCAL" or "STREAMING"
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (targetMode == "STREAMING") {
        stringResource(R.string.dialog_switch_to_go)
    } else {
        stringResource(R.string.dialog_switch_to_local)
    }
    val text = if (targetMode == "STREAMING") {
        stringResource(R.string.dialog_switch_to_go_desc)
    } else {
        stringResource(R.string.dialog_switch_to_local_desc)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = if (targetMode == "STREAMING") MaterialSymbolIcon("cloud", filled = true) else RhythmIcons.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onConfirm()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = if (targetMode == "STREAMING") MaterialSymbolIcon("cloud", filled = true) else RhythmIcons.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.switchmodedialog_switch_play))
                }

                OutlinedButton(
                    onClick = {
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_cancel))
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    )
}
