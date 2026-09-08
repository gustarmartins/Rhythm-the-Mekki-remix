/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.core.domain.model

/**
 * Base interface for album representations.
 * Implemented by both local and streaming albums.
 */
interface AlbumItem {
    val id: String
    val title: String
    val artist: String
    val artworkUri: String?
    val songCount: Int
    val year: Int?
    val sourceType: SourceType
    
    /**
     * Returns all songs in this album.
     */
    suspend fun getSongs(): List<PlayableItem>
}

/**
 * Base interface for artist representations.
 * Implemented by both local and streaming artists.
 */
interface ArtistItem {
    val id: String
    val name: String
    val artworkUri: String?
    val songCount: Int
    val albumCount: Int
    val sourceType: SourceType
    
    /**
     * Returns all songs by this artist.
     */
    suspend fun getSongs(): List<PlayableItem>
    
    /**
     * Returns all albums by this artist.
     */
    suspend fun getAlbums(): List<AlbumItem>
}

/**
 * Base interface for playlist representations.
 * Implemented by both local and streaming playlists.
 */
interface PlaylistItem {
    val id: String
    val name: String
    val description: String?
    val artworkUri: String?
    val songCount: Int
    val isEditable: Boolean
    val sourceType: SourceType
    
    /**
     * Returns all songs in this playlist.
     */
    suspend fun getSongs(): List<PlayableItem>
}
