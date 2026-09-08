/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Returns the current window container width in dp.
 *
 * This is the lint-preferred replacement for the deprecated
 * `Configuration.screenWidthDp` access, resolving the size from
 * [androidx.compose.ui.platform.LocalWindowInfo].
 */
@Composable
fun windowScreenWidthDp(): Int = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp().value.roundToInt()
}

/**
 * Returns the current window container height in dp.
 *
 * This is the lint-preferred replacement for the deprecated
 * `Configuration.screenHeightDp` access, resolving the size from
 * [androidx.compose.ui.platform.LocalWindowInfo].
 */
@Composable
fun windowScreenHeightDp(): Int = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.height.toDp().value.roundToInt()
}
