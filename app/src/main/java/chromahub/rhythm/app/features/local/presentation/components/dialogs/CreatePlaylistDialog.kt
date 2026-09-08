/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    song: Song? = null,
    onConfirmWithSong: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var playlistName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var addSong by remember { mutableStateOf(song != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = RhythmIcons.AddToPlaylist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.playlist_create),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { 
                        playlistName = it
                        isError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.playlist_name_field)) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = context.getString(R.string.playlist_name_empty),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (song != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = addSong,
                            onCheckedChange = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                addSong = it
                            }
                        )
                        
                        Text(
                            text = context.getString(R.string.dialog_add_song_to_playlist, song.title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isBlank()) {
                        isError = true
                    } else {
                        if (song != null && addSong) {
                            onConfirmWithSong(playlistName)
                        } else {
                            onConfirm(playlistName)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = RhythmIcons.AddToPlaylist,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Icon(
                    imageVector = RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun QueueActionDialog(
    song: Song,
    queueSize: Int,
    onDismiss: () -> Unit,
    onClearAndPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = RhythmIcons.Playlist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.playlist_queue_not_empty),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.dialog_songs_in_queue, queueSize, queueSize),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = context.getString(R.string.dialog_play_song_how, song.title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Option 1: Clear & Play
                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onClearAndPlay()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.Playlist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.dialog_clear_and_play),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = context.getString(R.string.dialog_clear_and_play_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Option 2: Add to Queue
                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onAddToQueue()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.AddToPlaylist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.dialog_add_to_queue),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = context.getString(R.string.dialog_add_to_queue_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                    onDismiss()
                }
            ) {
                Icon(
                    imageVector = RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
} 

@Composable
fun QueueListActionDialog(
    queueSize: Int,
    incomingCount: Int,
    sourceLabel: String?,
    onDismiss: () -> Unit,
    onReplaceQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToEnd: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = RhythmIcons.Playlist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.list_queue_action_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.list_queue_action_dialog_body,
                        queueSize,
                        queueSize,
                        incomingCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!sourceLabel.isNullOrBlank()) {
                    Text(
                        text = context.getString(R.string.list_queue_action_source, sourceLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onReplaceQueue()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.Playlist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.list_queue_action_replace_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = context.getString(R.string.list_queue_action_replace_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onPlayNext()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.Play,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.list_queue_action_play_next_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = context.getString(R.string.list_queue_action_play_next_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onAddToEnd()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.AddToPlaylist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.list_queue_action_add_end_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = context.getString(R.string.list_queue_action_add_end_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                    onDismiss()
                }
            ) {
                Icon(
                    imageVector = RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(context.getString(R.string.ui_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
