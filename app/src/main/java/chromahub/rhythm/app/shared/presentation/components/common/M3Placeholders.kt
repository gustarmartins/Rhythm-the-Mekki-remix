/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlin.random.Random

/**
 * Material 3 placeholder types
 */
enum class M3PlaceholderType {
    ALBUM,
    ARTIST,
    TRACK,
    PLAYLIST,
    GENERAL
}

/**
 * A Material 3 placeholder for media content with expressive shape support
 */
@Composable
fun M3Placeholder(
    type: M3PlaceholderType,
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape? = null
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
    
    // Get the default shapes for each type (when expressive shapes are disabled)
    val defaultShape = when (type) {
        M3PlaceholderType.ALBUM -> RoundedCornerShape(8.dp)
        M3PlaceholderType.ARTIST -> CircleShape
        M3PlaceholderType.TRACK -> RoundedCornerShape(4.dp)
        M3PlaceholderType.PLAYLIST -> RoundedCornerShape(8.dp)
        M3PlaceholderType.GENERAL -> RoundedCornerShape(8.dp)
    }
    
    // Use provided shape if available, otherwise get expressive shape from settings
    val finalShape = shape ?: if (expressiveShapesEnabled) {
        when (type) {
            M3PlaceholderType.ALBUM -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART, defaultShape)
            M3PlaceholderType.ARTIST -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ARTIST_ART, defaultShape)
            M3PlaceholderType.TRACK -> rememberExpressiveShapeFor(ExpressiveShapeTarget.SONG_ART, defaultShape)
            M3PlaceholderType.PLAYLIST -> rememberExpressiveShapeFor(ExpressiveShapeTarget.PLAYLIST_ART, defaultShape)
            M3PlaceholderType.GENERAL -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART, defaultShape)
        }
    } else {
        defaultShape
    }
    
    val icon = when (type) {
        M3PlaceholderType.ALBUM -> RhythmIcons.AlbumFilled
        M3PlaceholderType.ARTIST -> RhythmIcons.ArtistFilled
        M3PlaceholderType.TRACK -> RhythmIcons.MusicNote
        M3PlaceholderType.PLAYLIST -> RhythmIcons.Queue
        M3PlaceholderType.GENERAL -> MaterialSymbolIcon("audio_file", filled = true)
    }
    
    val containerColor = getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = finalShape,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val minDim = minOf(maxWidth, maxHeight)
            val iconSize = (minDim * 0.42f).coerceIn(28.dp, 96.dp)

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generate a consistent color based on the name
 */
@Composable
private fun getColorForName(name: String?, fallbackColor: Color): Color {
    if (name.isNullOrBlank()) {
        return fallbackColor
    }
    
    val seed = name.hashCode()
    val random = Random(seed)
    
    // Generate a color in the same palette as the theme
    val baseColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    
    return baseColors[random.nextInt(baseColors.size)]
}

/**
 * Album art placeholder
 */
@Composable
fun AlbumPlaceholder(
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape? = null
) {
    M3Placeholder(
        type = M3PlaceholderType.ALBUM,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Artist image placeholder
 */
@Composable
fun ArtistPlaceholder(
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape? = null
) {
    M3Placeholder(
        type = M3PlaceholderType.ARTIST,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Track placeholder
 */
@Composable
fun TrackPlaceholder(
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape? = null
) {
    M3Placeholder(
        type = M3PlaceholderType.TRACK,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Playlist placeholder
 */
@Composable
fun PlaylistPlaceholder(
    modifier: Modifier = Modifier,
    name: String? = null,
    shape: Shape? = null
) {
    M3Placeholder(
        type = M3PlaceholderType.PLAYLIST,
        name = name,
        modifier = modifier,
        shape = shape
    )
}
