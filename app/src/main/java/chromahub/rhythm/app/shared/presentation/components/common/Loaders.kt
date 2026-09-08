/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Official Material Design 3 Expressive Linear Progress Indicator
 * Wraps LinearWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3LinearLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    showTrackGap: Boolean = true,
    showStopIndicator: Boolean = true,
    fourColor: Boolean = false,
    isExpressive: Boolean = true
) {
    val targetProgress = progress?.coerceIn(0f, 1f) ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "m3_linear_loader_progress"
    )

    if (progress != null) {
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    } else {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    }
}

/**
 * Official Material Design 3 Expressive Circular Progress Indicator
 * Wraps CircularWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3CircularLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
    strokeWidth: Float = 4f,
    showTrackGap: Boolean = true,
    fourColor: Boolean = false,
    isExpressive: Boolean = true
) {
    val targetProgress = progress?.coerceIn(0f, 1f) ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "m3_circular_loader_progress"
    )

    if (progress != null) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    } else {
        CircularWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    }
}

/**
 * Official Material Design 3 Expressive Four-Color Circular Loader
 * Uses LoadingIndicator from androidx.compose.material3
 */
@Composable
fun M3FourColorCircularLoader(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    LoadingIndicator(modifier = modifier)
}

/**
 * Official Material Design 3 Expressive Four-Color Linear Loader
 * Uses LinearWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3FourColorLinearLoader(
    modifier: Modifier = Modifier,
    isExpressive: Boolean = true
) {
    LinearWavyProgressIndicator(modifier = modifier)
}

/**
 * Official Material Design 3 Expressive Pulse Loader
 * Uses LoadingIndicator from androidx.compose.material3
 */
@Composable
fun M3PulseLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    LoadingIndicator(modifier = modifier)
}

/**
 * Simple Circular Loading Indicator
 * Uses ContainedLoadingIndicator from androidx.compose.material3 for quick actions
 */
@Composable
fun SimpleCircularLoader(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    ContainedLoadingIndicator(modifier = modifier)
}

/**
 * Material Design 3 Expressive Buffered Linear Indicator
 * Uses LinearWavyProgressIndicator for both progress and buffer
 */
@Composable
fun M3BufferedLinearLoader(
    progress: Float,
    buffer: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    bufferColor: Color = color.copy(alpha = 0.4f),
    isExpressive: Boolean = true
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier
    )
}

/**
 * Material Design 3 Expressive Segmented Progress Indicator
 * Uses LinearWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3SegmentedLoader(
    progress: Float,
    modifier: Modifier = Modifier,
    segmentCount: Int = 5,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showLabel: Boolean = false
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier
    )
}

/**
 * Material Design 3 Expressive Dot Progress Indicator
 * Uses CircularWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3DotLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Float = 8f,
    dotCount: Int = 3
) {
    CircularWavyProgressIndicator(modifier = modifier)
}

/**
 * Material Design 3 Expressive Branded Loader
 * Uses CircularWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3BrandedLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    showLabel: Boolean = false
) {
    if (progress != null) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = modifier
        )
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * Material Design 3 Expressive Step Progress Indicator
 * Uses LinearWavyProgressIndicator from androidx.compose.material3
 */
@Composable
fun M3StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    completedColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier
    )
}
