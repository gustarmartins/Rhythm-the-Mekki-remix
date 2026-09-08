/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Deezer API
 * Deezer provides a free public API for searching artists and getting their images
 * API Documentation: https://developers.deezer.com/api
 */
interface DeezerApiService {
    @GET("search/artist")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25 // Increased limit to get more results
    ): DeezerSearchResponse

    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25 // Increased limit to get more results
    ): DeezerAlbumSearchResponse

    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): DeezerTrackSearchResponse
}

// ---------- DTOs ----------

data class DeezerSearchResponse(
    val data: List<DeezerArtist> = emptyList(),
    val total: Int = 0,
    val next: String? = null
)

data class DeezerAlbumSearchResponse(
    val data: List<DeezerAlbum> = emptyList(),
    val total: Int = 0,
    val next: String? = null
)

data class DeezerArtist(
    val id: Long,
    val name: String,
    val link: String,
    val share: String? = null,
    val picture: String? = null,
    @SerializedName("picture_small") val pictureSmall: String? = null,
    @SerializedName("picture_medium") val pictureMedium: String? = null,
    @SerializedName("picture_big") val pictureBig: String? = null,
    @SerializedName("picture_xl") val pictureXl: String? = null,
    @SerializedName("nb_album") val nbAlbum: Int = 0,
    @SerializedName("nb_fan") val nbFan: Int = 0,
    val radio: Boolean = false,
    val tracklist: String? = null,
    val type: String = "artist"
)

data class DeezerAlbum(
    val id: Long,
    val title: String,
    val link: String? = null,
    val cover: String? = null,
    @SerializedName("cover_small") val coverSmall: String? = null,
    @SerializedName("cover_medium") val coverMedium: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null,
    @SerializedName("cover_xl") val coverXl: String? = null,
    @SerializedName("nb_tracks") val nbTracks: Int = 0,
    @SerializedName("release_date") val releaseDate: String? = null,
    val artist: DeezerAlbumArtist? = null,
    val type: String = "album"
)

data class DeezerAlbumArtist(
    val id: Long,
    val name: String,
    val link: String? = null,
    val picture: String? = null,
    @SerializedName("picture_small") val pictureSmall: String? = null,
    @SerializedName("picture_medium") val pictureMedium: String? = null,
    @SerializedName("picture_big") val pictureBig: String? = null,
    @SerializedName("picture_xl") val pictureXl: String? = null
)

data class DeezerTrackSearchResponse(
    val data: List<DeezerTrack> = emptyList()
)

data class DeezerTrack(
    val id: Long,
    val title: String,
    val artist: DeezerTrackArtist?,
    val album: DeezerTrackAlbum?
)

data class DeezerTrackArtist(
    val id: Long,
    val name: String
)

data class DeezerTrackAlbum(
    val id: Long,
    val title: String
)
