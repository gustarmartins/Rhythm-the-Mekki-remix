/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.core.domain.model

/**
 * Base interface for any playable audio item.
 * Implemented by both local and streaming songs.
 */
interface PlayableItem {
    val id: String
    val title: String
    val artist: String
    val album: String
    val duration: Long
    val artworkUri: String?
    
    /**
     * Returns the URI or URL to play this item.
     * For local songs, this is a content:// or file:// URI.
     * For streaming songs, this is the streaming URL.
     */
    fun getPlaybackUri(): String
    
    /**
     * Indicates the source type of this playable item.
     */
    val sourceType: SourceType
}

/**
 * Enum representing the source of a playable item.
 */
enum class SourceType {
    LOCAL,
    SUBSONIC,
    JELLYFIN,
    SPOTIFY,
    APPLE_MUSIC,
    YOUTUBE_MUSIC,
    DEEZER,
    UNKNOWN
}
