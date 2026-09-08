/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun RhythmGuardCard(
    rhythmGuardMode: String,
    rhythmGuardRecommendedMinutes: Int,
    todayListeningMinutes: Int,
    isGuardTimeoutActive: Boolean,
    guardTimeoutRemainingMs: Long,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic countdown timer state
    var remainingMs by remember(guardTimeoutRemainingMs) { mutableLongStateOf(guardTimeoutRemainingMs) }
    
    LaunchedEffect(isGuardTimeoutActive, guardTimeoutRemainingMs) {
        if (isGuardTimeoutActive && guardTimeoutRemainingMs > 0L) {
            remainingMs = guardTimeoutRemainingMs
            while (remainingMs > 0L) {
                delay(1000L)
                remainingMs = (remainingMs - 1000L).coerceAtLeast(0L)
            }
        } else {
            remainingMs = 0L
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val guardModeLabel = when (rhythmGuardMode) {
        AppSettings.RHYTHM_GUARD_MODE_AUTO -> "Adaptive Guard"
        AppSettings.RHYTHM_GUARD_MODE_MANUAL -> "Manual Guard"
        else -> "Guard Off"
    }

    val progressFraction = if (rhythmGuardRecommendedMinutes > 0) {
        (todayListeningMinutes.toFloat() / rhythmGuardRecommendedMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnimation"
    )

    // Standard Material 3 Expressive Container to match Rhythm Stats cards
    Surface(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
            onCardClick()
        },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Vibrant M3 Circular Progress Ring
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.12f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Foreground Progress Arc
                val arcColor = if (isGuardTimeoutActive) {
                    MaterialTheme.colorScheme.error
                } else if (todayListeningMinutes > rhythmGuardRecommendedMinutes) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center Icon / Percentage
                if (isGuardTimeoutActive) {
                    Icon(
                        imageVector = RhythmIcons.Player.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right: Content details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_rhythm_guard),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Material 3 Chip-like Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGuardTimeoutActive) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = if (isGuardTimeoutActive) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Text(
                            text = guardModeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGuardTimeoutActive) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (isGuardTimeoutActive) {
                    val formattedTime = remember(remainingMs) {
                        val totalSecs = remainingMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        String.format(Locale.ROOT, "%02d:%02d", mins, secs)
                    }
                    Text(
                        text = stringResource(id = R.string.streaming_home_guard_break_active, formattedTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(id = R.string.settings_rhythm_guard_snapshot_widget_above_limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val statusText = if (todayListeningMinutes > rhythmGuardRecommendedMinutes) {
                        stringResource(id = R.string.settings_rhythm_guard_snapshot_widget_above_limit)
                    } else {
                        stringResource(id = R.string.settings_rhythm_guard_snapshot_widget_within_limit)
                    }

                    Text(
                        text = stringResource(
                            id = R.string.streaming_home_guard_exposure,
                            todayListeningMinutes,
                            rhythmGuardRecommendedMinutes
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
