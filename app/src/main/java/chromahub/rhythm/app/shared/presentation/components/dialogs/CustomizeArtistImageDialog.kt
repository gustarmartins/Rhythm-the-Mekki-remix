/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import chromahub.rhythm.app.R
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons

@Composable
fun CustomizeArtistImageDialog(
    artistName: String,
    onDismiss: () -> Unit,
    onSelectImage: () -> Unit,
    onResetImage: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = RhythmIcons.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.customizeartistimage_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.customizeartistimage_body_format, artistName),
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
                        onSelectImage()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.customizeartistimage_choose_image))
                }

                OutlinedButton(
                    onClick = {
                        onResetImage()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
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
                    Text(stringResource(R.string.customizeartistimage_reset_to_default))
                }
                
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(28.dp)
    )
}
