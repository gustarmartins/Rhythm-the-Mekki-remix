/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.network

import com.google.gson.JsonObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

internal interface LastFmApi {
    @GET("2.0/?method=artist.getSimilar&format=json&autocorrect=1&limit=40")
    suspend fun similarArtists(@Query("api_key") key: String, @Query("artist") artist: String): JsonObject
}

internal fun recommendationArtistKey(artist: String): String =
    Normalizer.normalize(artist, Normalizer.Form.NFKC).trim().lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

/** Optional hints only: no streaming, no login/scrobbling, and no network on the playback thread. */
internal class LastFmRecommendations(
    private val api: LastFmApi,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Cached(val artists: Set<String>, val expiresAt: Long)
    private val mutex = Mutex()
    private val cache = linkedMapOf<Pair<String, String>, Cached>()
    private var nextRequestAt = 0L

    suspend fun similarArtists(key: String, artist: String): Set<String> {
        val normalized = recommendationArtistKey(artist)
        if (key.isBlank() || normalized in setOf("", "unknown", "<unknown>", "unknown artist", "various artists")) {
            return emptySet()
        }
        return mutex.withLock {
            val cacheKey = key to normalized
            val timestamp = now()
            cache[cacheKey]?.takeIf { it.expiresAt > timestamp }?.let { return@withLock it.artists }
            if (timestamp < nextRequestAt) return@withLock emptySet()
            nextRequestAt = timestamp + 2_000L
            val result = try {
                val response = api.similarArtists(key, artist)
                if (response.has("error")) emptySet() else {
                    response.getAsJsonObject("similarartists")?.getAsJsonArray("artist")
                        ?.mapNotNull { entry ->
                            entry.asJsonObject.get("name")?.takeUnless { it.isJsonNull }
                                ?.asString?.let(::recommendationArtistKey)?.takeIf { it.isNotEmpty() }
                        }?.toSet().orEmpty()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { emptySet() } // Never log URLs or keys; offline playback remains available.
            cache[cacheKey] = Cached(result, timestamp + if (result.isEmpty()) 15 * 60_000L else 24 * 60 * 60_000L)
            while (cache.size > 64) cache.remove(cache.keys.first())
            result
        }
    }

    companion object {
        val shared by lazy {
            val client = OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).build()
            LastFmRecommendations(Retrofit.Builder().baseUrl("https://ws.audioscrobbler.com/")
                .client(client).addConverterFactory(GsonConverterFactory.create()).build().create(LastFmApi::class.java))
        }
    }
}
