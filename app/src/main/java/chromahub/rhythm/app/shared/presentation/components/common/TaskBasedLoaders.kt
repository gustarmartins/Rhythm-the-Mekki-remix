/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity

/**
 * Task-Based Loader Components
 * These loaders are expressive and self-documenting based on their purpose.
 * 
 * Purpose: Replace generic M3LinearLoader, M3CircularLoader, etc. with 
 * task-specific implementations that clearly communicate intent.
 */

/**
 * MediaScanningLoader - For media/library scanning operations
 * 
 * Use when:
 * - Scanning music library from device storage
 * - Indexing media files
 * - Building song/album/artist catalogs
 * 
 * Features: Four-color circular animation to indicate comprehensive scanning
 */
@Composable
fun MediaScanningLoader(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        // Use Material 3 Expressive Loading Indicator for comprehensive scanning
        LoadingIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * DataProcessingLoader - For data processing and transformation operations
 * 
 * Use when:
 * - Processing/filtering large datasets
 * - Applying transformations to data
 * - Computing statistics or aggregations
 * - Loading and parsing configuration files
 * 
 * Features: Linear progress for determinate or shimmer effect for indeterminate
 */
@Composable
fun DataProcessingLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = modifier
            )
        } else {
            LinearWavyProgressIndicator(modifier = modifier)
        }
    } else {
        // Fallback to simpler indicator
        LinearWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * ActionProgressLoader - For quick, short-lived actions
 * 
 * Use when:
 * - Adding/removing items from blacklist/whitelist
 * - Quick file operations
 * - Saving/deleting individual items
 * - Toggling settings
 * 
 * Features: Small, compact circular loader that doesn't distract
 */
@Composable
fun ActionProgressLoader(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        // Use ContainedLoadingIndicator for compact, unobtrusive loading
        ContainedLoadingIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * ContentLoadingIndicator - For loading content in screens/views
 * 
 * Use when:
 * - Loading songs, albums, artists lists
 * - Fetching remote content
 * - Waiting for API responses
 * - Loading screen data during navigation
 * 
 * Features: Standard circular loader that indicates data is being fetched
 */
@Composable
fun ContentLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        CircularWavyProgressIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * PlaybackBufferingLoader - For media playback buffering
 * 
 * Use when:
 * - Buffering audio/video content
 * - Loading media for playback
 * - Preparing player for playback
 * - Streaming content preparation
 * 
 * Features: Pulse animation to indicate active buffering
 */
@Composable
fun PlaybackBufferingLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        // LoadingIndicator provides smooth pulsing animation
        LoadingIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * InitializationLoader - For app/feature initialization
 * 
 * Use when:
 * - App first-time setup
 * - Feature initialization
 * - Permission setup flows
 * - Onboarding processes
 * 
 * Features: Four-color animation indicating multi-stage initialization
 */
@Composable
fun InitializationLoader(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        // LoadingIndicator for multi-stage initialization
        LoadingIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * FileOperationLoader - For file I/O operations
 * 
 * Use when:
 * - Reading/writing files
 * - Importing/exporting playlists
 * - Backup/restore operations
 * - File system operations
 * 
 * Features: Linear progress with optional determinate progress tracking
 */
@Composable
fun FileOperationLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = modifier
            )
        } else {
            LinearWavyProgressIndicator(modifier = modifier)
        }
    } else {
        LinearWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * NetworkOperationLoader - For network requests
 * 
 * Use when:
 * - Fetching data from APIs
 * - Downloading content
 * - Uploading data
 * - Syncing with remote services
 * 
 * Features: Circular loader with subtle animation
 */
@Composable
fun NetworkOperationLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 4f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        CircularWavyProgressIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * ImageLoadingPlaceholder - For image loading operations
 * 
 * Use when:
 * - Loading album art
 * - Loading artist images
 * - Loading playlist covers
 * - Any image asset loading
 * 
 * Features: Subtle pulse animation that doesn't overpower the UI
 */
@Composable
fun ImageLoadingPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        // Subtle ContainedLoadingIndicator for image placeholders
        ContainedLoadingIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * SearchingLoader - For search operations
 * 
 * Use when:
 * - Performing search queries
 * - Filtering large datasets
 * - Real-time search results
 * 
 * Features: Linear shimmer effect to indicate active searching
 */
@Composable
fun SearchingLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        LinearWavyProgressIndicator(modifier = modifier)
    } else {
        LinearWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * MultiStageOperationLoader - For operations with multiple stages
 * 
 * Use when:
 * - Multi-step setup wizards
 * - Complex processing pipelines
 * - Staged data migration
 * - Multi-phase initialization
 * 
 * Features: Four-color linear progress indicating multiple stages
 */
@Composable
fun MultiStageOperationLoader(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = modifier
            )
        } else {
            LinearWavyProgressIndicator(modifier = modifier)
        }
    } else {
        LinearWavyProgressIndicator(modifier = modifier)
    }
}

/**
 * CacheOperationLoader - For cache-related operations
 * 
 * Use when:
 * - Building cache
 * - Clearing cache
 * - Cache validation
 * - Preloading cached data
 * 
 * Features: Circular loader with moderate emphasis
 */
@Composable
fun CacheOperationLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
    strokeWidth: Float = 3f,
    isExpressive: Boolean = true
) {
    if (isExpressive) {
        CircularWavyProgressIndicator(modifier = modifier)
    } else {
        CircularWavyProgressIndicator(modifier = modifier)
    }
}
