/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.domain.model

import chromahub.rhythm.app.core.domain.model.AlbumItem
import chromahub.rhythm.app.core.domain.model.ArtistItem
import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.model.SourceType

/**
 * Represents an artist from a streaming service.
 * Data comes directly from provider APIs (Jellyfin/Subsonic).
 */
data class StreamingArtist(
    override val id: String,
    override val name: String,
    override val artworkUri: String?,
    override val songCount: Int,
    override val albumCount: Int,
    override val sourceType: SourceType,
    
    // Optional metadata from provider
    val externalId: String? = null,
    val bio: String? = null,
    val genres: List<String> = emptyList(),
    val followers: Long? = null,
    val popularity: Int? = null,
    
    // Loaded relationships (may be incomplete)
    private val topTracks: List<StreamingSong> = emptyList(),
    private val albums: List<StreamingAlbum> = emptyList()
) : ArtistItem {
    
    override suspend fun getSongs(): List<PlayableItem> = topTracks
    override suspend fun getAlbums(): List<AlbumItem> = albums
    
    fun getTopTracks(): List<StreamingSong> = topTracks
    fun getAlbumsList(): List<StreamingAlbum> = albums
}
