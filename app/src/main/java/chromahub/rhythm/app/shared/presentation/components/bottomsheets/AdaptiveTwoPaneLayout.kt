/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.util.DevicePosture
import chromahub.rhythm.app.util.rememberDevicePosture
import chromahub.rhythm.app.util.windowScreenHeightDp
import chromahub.rhythm.app.util.windowScreenWidthDp

/**
 * Standard Two-Pane Master-Detail layout for tablets and foldable devices.
 *
 * In phone or portrait mode, items stack vertically.
 * In tablet landscape mode (or split foldable posture), panes render side-by-side with configurable weights.
 */
@Composable
fun AdaptiveTwoPaneLayout(
    pane1: @Composable () -> Unit,
    pane2: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pane1Weight: Float = 0.42f,
    pane2Weight: Float = 0.58f,
    spacing: Dp = 20.dp,
    forceStacked: Boolean = false
) {
    val screenWidth = windowScreenWidthDp()
    val screenHeight = windowScreenHeightDp()
    val postureState = rememberDevicePosture().value

    val isTablet = screenWidth >= 600 || postureState is DevicePosture.Book || postureState is DevicePosture.Separated
    val isLandscapeTablet = isTablet && screenWidth > screenHeight && !forceStacked

    if (isLandscapeTablet) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .weight(pane1Weight)
                    .fillMaxWidth()
            ) {
                pane1()
            }
            Box(
                modifier = Modifier
                    .weight(pane2Weight)
                    .fillMaxWidth()
            ) {
                pane2()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pane1()
            pane2()
        }
    }
}
