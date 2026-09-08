/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.core.domain.repository

import chromahub.rhythm.app.core.domain.model.AlbumItem
import chromahub.rhythm.app.core.domain.model.ArtistItem
import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface for music operations.
 * Defines common operations that both local and streaming repositories must implement.
 */
interface MusicRepository {
    
    /**
     * Get all available songs as a Flow.
     */
    fun getSongs(): Flow<List<PlayableItem>>
    
    /**
     * Get all available albums as a Flow.
     */
    fun getAlbums(): Flow<List<AlbumItem>>
    
    /**
     * Get all available artists as a Flow.
     */
    fun getArtists(): Flow<List<ArtistItem>>
    
    /**
     * Get all playlists as a Flow.
     */
    fun getPlaylists(): Flow<List<PlaylistItem>>
    
    /**
     * Search for songs matching the query.
     */
    suspend fun searchSongs(query: String): List<PlayableItem>
    
    /**
     * Search for albums matching the query.
     */
    suspend fun searchAlbums(query: String): List<AlbumItem>
    
    /**
     * Search for artists matching the query.
     */
    suspend fun searchArtists(query: String): List<ArtistItem>
    
    /**
     * Search for playlists matching the query.
     */
    suspend fun searchPlaylists(query: String): List<PlaylistItem>
    
    /**
     * Get a song by its ID.
     */
    suspend fun getSongById(id: String): PlayableItem?
    
    /**
     * Get an album by its ID.
     */
    suspend fun getAlbumById(id: String): AlbumItem?
    
    /**
     * Get an artist by their ID.
     */
    suspend fun getArtistById(id: String): ArtistItem?
    
    /**
     * Get a playlist by its ID.
     */
    suspend fun getPlaylistById(id: String): PlaylistItem?
    
    /**
     * Get songs for a specific album by album ID.
     */
    suspend fun getSongsForAlbum(albumId: String): List<PlayableItem>
}
