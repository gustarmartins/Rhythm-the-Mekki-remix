/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Service interface for Spotify Web API
 * Used to search for tracks and get Spotify track IDs
 */
interface SpotifySearchApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10,
        @Query("market") market: String = "US",
        @Header("Authorization") authorization: String
    ): Response<SpotifySearchResponse>
}

// ---------- DTOs ----------

data class SpotifySearchResponse(
    val tracks: SpotifyTracksResponse?
)

data class SpotifyTracksResponse(
    val items: List<SpotifyTrack> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum?,
    @SerializedName("duration_ms") val durationMs: Long = 0,
    val popularity: Int = 0,
    @SerializedName("external_urls") val externalUrls: SpotifyExternalUrls?
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    @SerializedName("external_urls") val externalUrls: SpotifyExternalUrls?
)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val images: List<SpotifyImage> = emptyList(),
    @SerializedName("release_date") val releaseDate: String?
)

data class SpotifyImage(
    val url: String,
    val width: Int?,
    val height: Int?
)

data class SpotifyExternalUrls(
    val spotify: String?
)
