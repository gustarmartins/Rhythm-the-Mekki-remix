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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon

@Composable
fun FdroidUpdateWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = MaterialSymbolIcon("security", filled = true),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.fdroid_update_warning_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = context.getString(R.string.fdroid_update_warning_message),
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
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("arrow_forward", filled = true),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.onboarding_continue))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("close", filled = true),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(context.getString(R.string.ui_cancel))
                }
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(28.dp)
    )
}
