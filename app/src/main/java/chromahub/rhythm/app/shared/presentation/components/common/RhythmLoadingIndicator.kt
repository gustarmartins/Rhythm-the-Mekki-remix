/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A [LoadingIndicator] that reflects the current theme's [primaryContainer] colour.
 *
 * Uses M3 expressive loading indicator with animated colour transitions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhythmLoadingIndicator(
    modifier: Modifier = Modifier
) {
    LoadingIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
