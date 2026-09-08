/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.dialogs

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.UserAudioDevice
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

/**
 * Dialog that suggests applying an AutoEQ profile when a recognized device is detected
 */
@Composable
fun AutoEQSuggestionDialog(
    deviceName: String,
    savedDevice: UserAudioDevice,
    equalizerEnabled: Boolean,
    onApplyProfile: () -> Unit,
    onDismiss: () -> Unit,
    onDontAskAgain: () -> Unit,
    onConfigureDevice: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = MaterialSymbolIcon("headset_mic", filled = true),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.autoeqsuggestiondialog_audio_device_detected),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = MaterialSymbolIcon("graphic_eq", filled = true),
                                        contentDescription = null,
                                        
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.autoeqsuggestiondialog_saved_configuration),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (savedDevice.autoEQProfileName != null) {
                                    Text(
                                        text = savedDevice.autoEQProfileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("auto_mode", filled = true),
                                contentDescription = null,
                                
                                modifier = Modifier.size(16.dp)
                            )
                             Text(
                                 text = "${savedDevice.type.displayName} • ${if (savedDevice.brand.isNotEmpty()) savedDevice.brand else stringResource(R.string.autoeq_no_brand)}",
                                 style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Status message
                Text(
                    text = if (savedDevice.autoEQProfileName != null) {
                        "This device has an AutoEQ profile configured. Would you like to apply it now?"
                    } else {
                        "This device is configured but has no AutoEQ profile. Configure one now?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (!equalizerEnabled && savedDevice.autoEQProfileName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.autoeqsuggestiondialog_note_equalizer_is_currently),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (savedDevice.autoEQProfileName != null) {
                    Button(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            onApplyProfile()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("graphic_eq", filled = true),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.autoeqsuggestiondialog_apply_profile))
                    }
                }
                
                FilledTonalButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        onConfigureDevice()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("headset_mic", filled = true),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                     Text(if (savedDevice.autoEQProfileName != null) stringResource(R.string.autoeq_change_profile) else stringResource(R.string.autoeq_configure_device))
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                            onDontAskAgain()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("not_interested", filled = true),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.autoeqsuggestiondialog_dont_ask), style = MaterialTheme.typography.labelMedium)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.autoeqsuggestiondialog_not_now), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
