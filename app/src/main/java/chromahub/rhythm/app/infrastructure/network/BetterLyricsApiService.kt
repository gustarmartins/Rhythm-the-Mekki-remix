/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Better Lyrics API.
 * Better Lyrics API serves synchronized lyrics in TTML format.
 */
interface BetterLyricsApiService {
    /**
     * Get lyrics for a song and artist.
     */
    @GET("getLyrics")
    suspend fun getLyrics(
        @Query("a") artist: String,
        @Query("s") song: String
    ): BetterLyricsResponse
}

/**
 * Response model for Better Lyrics getLyrics endpoint.
 */
data class BetterLyricsResponse(
    @SerializedName("ttml") val ttml: String?
)
