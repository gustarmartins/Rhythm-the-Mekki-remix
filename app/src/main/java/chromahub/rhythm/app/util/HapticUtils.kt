/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.shared.data.model.AppSettings

/**
 * Types of haptic feedback intensities.
 */
enum class HapticType {
    LIGHT,       // For subtle feedback (toggles, drag ticks, reordering, typing/text selection)
    MEDIUM,      // For standard button clicks, navigation clicks
    HEAVY,       // For long press, destructive actions, errors
    DOUBLE_CLICK // For play/pause click, double click gestures
}

/**
 * Utility object for haptic feedback that respects user settings and supports different intensities.
 * 
 * IMPORTANT: This utility uses the current cached value of haptic settings
 * to avoid blocking the main thread. The value is read from StateFlow's current
 * value which is always available synchronously.
 */
object HapticUtils {
    
    /**
     * Performs haptic feedback only if enabled in settings, using the device Vibrator for precise intensities.
     */
    fun performHapticFeedback(
        context: Context,
        hapticFeedback: HapticFeedback,
        type: HapticType
    ) {
        val appSettings = AppSettings.getInstance(context)
        val isEnabled = appSettings.isHapticEnabled.value
        
        if (isEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effectId = when (type) {
                            HapticType.LIGHT -> VibrationEffect.EFFECT_TICK
                            HapticType.MEDIUM -> VibrationEffect.EFFECT_CLICK
                            HapticType.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                            HapticType.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
                        }
                        try {
                            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
                        } catch (e: Exception) {
                            fallbackVibrate(vibrator, type)
                        }
                    } else {
                        fallbackVibrate(vibrator, type)
                    }
                } else {
                    // Fallback to standard Compose haptic feedback
                    val composeType = when (type) {
                        HapticType.LIGHT -> HapticFeedbackType.TextHandleMove
                        HapticType.MEDIUM -> HapticFeedbackType.LongPress
                        HapticType.HEAVY -> HapticFeedbackType.LongPress
                        HapticType.DOUBLE_CLICK -> HapticFeedbackType.LongPress
                    }
                    hapticFeedback.performHapticFeedback(composeType)
                }
            } catch (e: SecurityException) {
                android.util.Log.w("HapticUtils", "Haptic feedback failed due to security exception: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w("HapticUtils", "Haptic feedback failed: ${e.message}")
            }
        }
    }

    private fun fallbackVibrate(vibrator: Vibrator, type: HapticType) {
        try {
            @Suppress("DEPRECATION")
            when (type) {
                HapticType.LIGHT -> vibrator.vibrate(10L)
                HapticType.MEDIUM -> vibrator.vibrate(25L)
                HapticType.HEAVY -> vibrator.vibrate(55L)
                HapticType.DOUBLE_CLICK -> vibrator.vibrate(longArrayOf(0, 15, 45, 15), -1)
            }
        } catch (e: Exception) {
            android.util.Log.w("HapticUtils", "Fallback vibration failed: ${e.message}")
        }
    }

    /**
     * Backward compatibility helper for Compose's standard HapticFeedbackType
     */
    fun performHapticFeedback(
        context: Context,
        hapticFeedback: HapticFeedback,
        type: HapticFeedbackType
    ) {
        val mappedType = when (type) {
            HapticFeedbackType.LongPress -> HapticType.HEAVY
            HapticFeedbackType.TextHandleMove -> HapticType.LIGHT
            else -> HapticType.MEDIUM
        }
        performHapticFeedback(context, hapticFeedback, mappedType)
    }
}

/**
 * Composable extension function for easier haptic feedback in Compose
 */
@Composable
fun HapticFeedback.PerformIfEnabled(type: HapticType) {
    val context = LocalContext.current
    HapticUtils.performHapticFeedback(context, this, type)
}

/**
 * Backward compatibility extension for Compose's standard HapticFeedbackType
 */
@Composable
fun HapticFeedback.PerformIfEnabled(type: HapticFeedbackType) {
    val context = LocalContext.current
    HapticUtils.performHapticFeedback(context, this, type)
}

