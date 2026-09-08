/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.data.provider

/**
 * Lightweight song model used by provider API clients before mapping to UI/domain models.
 */
data class ProviderSong(
    val providerId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val albumId: String? = null,
    val albumArtist: String? = null,
    val isFavorite: Boolean = false,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val codec: String? = null
)

/**
 * Lightweight playlist model used by provider API clients before mapping to UI/domain models.
 */
data class ProviderPlaylist(
    val providerId: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val owner: String? = null,
    val isPublic: Boolean = true
)

/**
 * Lightweight album model used by provider API clients before mapping to UI/domain models.
 */
data class ProviderAlbum(
    val providerId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val year: Int? = null,
    val description: String? = null
)

/**
 * Lightweight artist model used by provider API clients before mapping to UI/domain models.
 */
data class ProviderArtist(
    val providerId: String,
    val name: String,
    val artworkUrl: String? = null,
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val description: String? = null
)

/**
 * Result of a successful provider connection/authentication.
 */
data class ProviderConnectionResult(
    val displayName: String,
    val serverUrl: String = ""
)
