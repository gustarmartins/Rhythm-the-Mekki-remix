/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.domain.model

import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.model.SourceType

/**
 * Represents a song from a streaming service.
 */
data class StreamingSong(
    override val id: String,
    override val title: String,
    override val artist: String,
    override val album: String,
    override val duration: Long,
    override val artworkUri: String?,
    override val sourceType: SourceType,
    val streamingUrl: String?,
    val previewUrl: String?,
    val isExplicit: Boolean = false,
    val popularity: Int? = null,
    val releaseDate: String? = null,
    val isPlayable: Boolean = true,
    val externalId: String? = null, // Spotify URI, Apple Music ID, etc.
    val albumId: String? = null,
    val albumArtist: String? = null,
    val isrc: String? = null, // International Standard Recording Code
    val isFavorite: Boolean = false,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val codec: String? = null
) : PlayableItem {
    
    override fun getPlaybackUri(): String = streamingUrl ?: previewUrl ?: ""
    
    /**
     * Check if full playback is available (vs preview only).
     */
    fun hasFullPlayback(): Boolean = streamingUrl != null
    
    /**
     * Check if preview is available.
     */
    fun hasPreview(): Boolean = previewUrl != null
}

/**
 * Represents quality information for a streaming track.
 */
data class StreamingQualityInfo(
    val bitrate: Int,
    val format: String, // "MP3", "AAC", "FLAC", etc.
    val sampleRate: Int? = null,
    val bitDepth: Int? = null
)
