/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo

/**
 * Representation of device posture (e.g., standard phone, Flex Mode tabletop, book fold).
 */
sealed interface DevicePosture {
    object Normal : DevicePosture

    data class TableTop(
        val hingeBounds: Rect
    ) : DevicePosture

    data class Book(
        val hingeBounds: Rect
    ) : DevicePosture

    data class Separated(
        val hingeBounds: Rect,
        val isHorizontal: Boolean
    ) : DevicePosture
}

/**
 * Helper to extract Activity from Context.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Remembers the current [DevicePosture] for foldables (Flex Mode tabletop posture, Book fold, etc.).
 */
@Composable
fun rememberDevicePosture(): State<DevicePosture> {
    val context = LocalContext.current
    val activity = context.findActivity()

    return produceState<DevicePosture>(initialValue = DevicePosture.Normal, activity) {
        if (activity == null) {
            value = DevicePosture.Normal
            return@produceState
        }

        val windowInfoTracker = WindowInfoTracker.getOrCreate(context)
        windowInfoTracker.windowLayoutInfo(activity).collect { layoutInfo ->
            val foldingFeature = layoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()

            value = if (foldingFeature != null) {
                val isHalfOpened = foldingFeature.state == FoldingFeature.State.HALF_OPENED
                val isHorizontal = foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
                val isVertical = foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL

                when {
                    isHalfOpened && isHorizontal -> {
                        DevicePosture.TableTop(foldingFeature.bounds)
                    }

                    isHalfOpened && isVertical -> {
                        DevicePosture.Book(foldingFeature.bounds)
                    }

                    foldingFeature.isSeparating -> {
                        DevicePosture.Separated(
                            hingeBounds = foldingFeature.bounds,
                            isHorizontal = isHorizontal
                        )
                    }

                    else -> DevicePosture.Normal
                }
            } else {
                DevicePosture.Normal
            }
        }
    }
}
