/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import chromahub.rhythm.app.shared.presentation.components.common.AlbumPlaceholder
import chromahub.rhythm.app.shared.presentation.components.common.ArtistPlaceholder
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.PlaylistPlaceholder
import chromahub.rhythm.app.shared.presentation.components.common.TrackPlaceholder
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import androidx.compose.ui.res.stringResource
import chromahub.rhythm.app.R
import androidx.compose.runtime.collectAsState
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.ArtistArtworkSource

/**
 * Modern Material 3 style utilities for image handling using Compose and Coil
 */
object M3ImageUtils {

    /**
     * Display a media image with appropriate Material 3 style placeholder
     */
    @Composable
    fun M3MediaImage(
        data: Any?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        shape: Shape? = null,
        type: M3PlaceholderType = M3PlaceholderType.GENERAL,
        name: String? = null,
        expressiveShape: Shape? = null
    ) {
        val context = LocalContext.current
        
        val imageRequest = remember(data, context) {
            ImageRequest.Builder(context)
                .data(data)
                .crossfade(150)
                .memoryCacheKey(data?.toString())
                .diskCacheKey(data?.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
        
        var showPlaceholder by remember(data) { mutableStateOf(data == null || data.toString().isBlank()) }
        
        Box(modifier = modifier) {
            if (data != null && data.toString().isNotBlank()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = contentDescription,
                    modifier = if (shape != null) Modifier.fillMaxSize().clip(shape) else Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    onState = { state ->
                        showPlaceholder = state is AsyncImagePainter.State.Loading || 
                                         state is AsyncImagePainter.State.Error ||
                                         state is AsyncImagePainter.State.Empty
                    }
                )
            }
            
            // Show appropriate placeholder based on loading state
            AnimatedVisibility(
                visible = showPlaceholder,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val resolvedShape = shape ?: androidx.compose.ui.graphics.RectangleShape
                    when (type) {
                        M3PlaceholderType.ALBUM -> AlbumPlaceholder(name = name, modifier = Modifier.fillMaxSize(), shape = resolvedShape)
                        M3PlaceholderType.ARTIST -> ArtistPlaceholder(name = name, modifier = Modifier.fillMaxSize(), shape = resolvedShape)
                        M3PlaceholderType.TRACK -> TrackPlaceholder(name = name, modifier = Modifier.fillMaxSize(), shape = resolvedShape)
                        M3PlaceholderType.PLAYLIST -> PlaylistPlaceholder(name = name, modifier = Modifier.fillMaxSize(), shape = resolvedShape)
                        M3PlaceholderType.GENERAL -> AlbumPlaceholder(name = name, modifier = Modifier.fillMaxSize(), shape = resolvedShape)
                    }
                }
            }
        }
    }
    
    /**
     * Album art with Material 3 placeholder and expressive shape support
     */
    @Composable
    fun AlbumArt(
        imageUrl: Any?,
        albumName: String?,
        modifier: Modifier = Modifier,
        shape: Shape? = null,
        applyExpressiveShape: Boolean = true
    ) {
        val expressiveShape = if (applyExpressiveShape) {
            rememberExpressiveShapeFor(
                ExpressiveShapeTarget.ALBUM_ART,
                MaterialTheme.shapes.large
            )
        } else {
            null // Don't apply any shape when applyExpressiveShape is false
        }
        val finalShape = shape ?: expressiveShape
        
        M3MediaImage(
            data = imageUrl,
            contentDescription = stringResource(R.string.album_artwork_description, albumName ?: ""),
            modifier = modifier,
            shape = finalShape,
            type = M3PlaceholderType.ALBUM,
            name = albumName,
            expressiveShape = if (applyExpressiveShape) expressiveShape else null
        )
    }
    
    /**
     * Artist image with Material 3 placeholder and expressive shape support
     */
    @Composable
    fun ArtistImage(
        imageUrl: Any?,
        artistName: String?,
        modifier: Modifier = Modifier,
        shape: Shape? = null,
        applyExpressiveShape: Boolean = true
    ) {
        val context = LocalContext.current
        val artistArtworkSource by remember { AppSettings.getInstance(context).artistArtworkSource }.collectAsState()
        val data = if (artistArtworkSource == ArtistArtworkSource.DISABLED) null else imageUrl

        val expressiveShape = if (applyExpressiveShape) {
            rememberExpressiveShapeFor(
                ExpressiveShapeTarget.ARTIST_ART,
                MaterialTheme.shapes.large
            )
        } else {
            null // Don't apply any shape when applyExpressiveShape is false
        }
        val finalShape = shape ?: expressiveShape
        
        M3MediaImage(
            data = data,
            contentDescription = stringResource(R.string.artist_artwork_description, artistName ?: ""),
            modifier = modifier,
            shape = finalShape,
            type = M3PlaceholderType.ARTIST,
            name = artistName,
            expressiveShape = if (applyExpressiveShape) expressiveShape else null
        )
    }
    
    /**
     * Track image with Material 3 placeholder and expressive shape support
     */
    @Composable
    fun TrackImage(
        imageUrl: Any?,
        trackName: String?,
        modifier: Modifier = Modifier,
        shape: Shape? = null,
        applyExpressiveShape: Boolean = true
    ) {
        val expressiveShape = if (applyExpressiveShape) {
            rememberExpressiveShapeFor(
                ExpressiveShapeTarget.SONG_ART,
                MaterialTheme.shapes.large
            )
        } else {
            null // Don't apply any shape when applyExpressiveShape is false
        }
        val finalShape = shape ?: expressiveShape
        
        M3MediaImage(
            data = imageUrl,
            contentDescription = stringResource(R.string.track_artwork_description, trackName ?: ""),
            modifier = modifier,
            shape = finalShape,
            type = M3PlaceholderType.TRACK,
            name = trackName,
            expressiveShape = if (applyExpressiveShape) expressiveShape else null
        )
    }
    
    /**
     * Playlist image with Material 3 placeholder and expressive shape support
     */
    @Composable
    fun PlaylistImage(
        imageUrl: Any?,
        playlistName: String?,
        modifier: Modifier = Modifier,
        shape: Shape? = null,
        applyExpressiveShape: Boolean = true
    ) {
        val expressiveShape = if (applyExpressiveShape) {
            rememberExpressiveShapeFor(
                ExpressiveShapeTarget.PLAYLIST_ART,
                MaterialTheme.shapes.large
            )
        } else {
            null // Don't apply any shape when applyExpressiveShape is false
        }
        val finalShape = shape ?: expressiveShape
        
        M3MediaImage(
            data = imageUrl,
            contentDescription = stringResource(R.string.playlist_artwork_description, playlistName ?: ""),
            modifier = modifier,
            shape = finalShape,
            type = M3PlaceholderType.PLAYLIST,
            name = playlistName,
            expressiveShape = if (applyExpressiveShape) expressiveShape else null
        )
    }
} 
