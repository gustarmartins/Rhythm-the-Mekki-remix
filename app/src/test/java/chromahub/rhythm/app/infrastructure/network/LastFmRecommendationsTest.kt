/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LastFmRecommendationsTest {
    private val response = """{"similarartists":{"artist":[{"name":"Björk","match":"0.9"},{"name":"CHVRCHES","match":"0.7"}]}}"""
    private fun parsed(value: String = response) = JsonParser.parseString(value).asJsonObject

    @Test fun officialLookupEncodesArtistAndUsesCachedNormalizedResults() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().body(response).build())
            val client = OkHttpClient()
            try {
                val api = Retrofit.Builder().baseUrl(server.url("/"))
                    .client(client).addConverterFactory(GsonConverterFactory.create()).build().create(LastFmApi::class.java)
                val recommendations = LastFmRecommendations(api)
                val expected = setOf("björk", "chvrches")
                assertEquals(expected, recommendations.similarArtists("test-key", "Florence + The Machine"))
                assertEquals(expected, recommendations.similarArtists("test-key", "  FLORENCE + The Machine "))
                val request = server.takeRequest(5, TimeUnit.SECONDS)!!
                assertEquals("GET", request.method)
                assertEquals("artist.getSimilar", request.url.queryParameter("method"))
                assertEquals("Florence + The Machine", request.url.queryParameter("artist"))
                assertEquals("test-key", request.url.queryParameter("api_key"))
                assertEquals(1, server.requestCount)
            } finally {
                client.dispatcher.cancelAll()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
    }

    @Test fun blankKeyOrUnknownArtistNeverMakesARequest() = runBlocking {
        val recommendations = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String): JsonObject = error("No request expected")
        })
        assertTrue(recommendations.similarArtists("", "Artist").isEmpty())
        assertTrue(recommendations.similarArtists("key", "<unknown>").isEmpty())
        assertTrue(recommendations.similarArtists("key", "  ").isEmpty())
    }

    @Test fun apiErrorsBackOffInsteadOfRetryingOnEveryTransition() = runBlocking {
        var time = 10_000L
        var calls = 0
        val recommendations = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String): JsonObject {
                calls++
                return parsed("""{"error":29,"message":"Rate limit exceeded"}""")
            }
        }, now = { time })
        assertTrue(recommendations.similarArtists("key", "Artist").isEmpty())
        time += 3_000
        assertTrue(recommendations.similarArtists("key", "Artist").isEmpty())
        assertEquals(1, calls)
        time += 15 * 60_000
        recommendations.similarArtists("key", "Artist")
        assertEquals(2, calls)
    }

    @Test fun successfulCacheExpiresAndRapidNewSeedsAreThrottled() = runBlocking {
        var time = 10_000L
        var calls = 0
        val recommendations = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String): JsonObject {
                calls++
                return parsed()
            }
        }, now = { time })
        assertEquals(2, recommendations.similarArtists("key", "Artist").size)
        assertTrue(recommendations.similarArtists("key", "Another artist").isEmpty())
        time += 2_000
        assertEquals(2, recommendations.similarArtists("key", "Another artist").size)
        time += 24 * 60 * 60_000L
        assertEquals(2, recommendations.similarArtists("key", "Artist").size)
        assertEquals(3, calls)
    }

    @Test fun networkFailureAndMalformedResponseLeaveLocalSelectionAvailable() = runBlocking {
        val failing = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String): JsonObject = throw java.io.IOException("offline")
        })
        assertTrue(failing.similarArtists("key", "Artist").isEmpty())
        val malformed = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String) = parsed("""{"similarartists":"bad"}""")
        })
        assertTrue(malformed.similarArtists("key", "Artist").isEmpty())
    }

    @Test fun cancellationDoesNotCommitAStaleRecommendation() = runBlocking {
        val recommendations = LastFmRecommendations(object : LastFmApi {
            override suspend fun similarArtists(key: String, artist: String): JsonObject = throw CancellationException()
        })
        try {
            recommendations.similarArtists("key", "Artist")
            fail("Cancellation must propagate to the replaced queue job")
        } catch (_: CancellationException) { }
    }
}
