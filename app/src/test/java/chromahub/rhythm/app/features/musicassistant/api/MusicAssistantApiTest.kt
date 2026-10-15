/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class MusicAssistantApiTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var transport: MusicAssistantHttpTransport
    private lateinit var api: MusicAssistantApi

    @Before fun setup() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient()
        transport = MusicAssistantHttpTransport(server.url("/proxy/music/").toString(), { "test-token" }, client)
        api = MusicAssistantApi(transport)
    }

    @After fun cleanup() {
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
        server.close()
    }

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private fun request() = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
    private fun body() = JsonParser.parseString(request().body!!.utf8()).asJsonObject

    @Test fun serverTimeUsesTheClockCommand() = runBlocking {
        respond("1791960000.125")
        assertEquals(1791960000.125, api.serverTime(), 0.001)
        assertEquals("time", body().get("command").asString)
    }

    @Test fun proxyPathBearerAndBareArrayAreUsed() = runBlocking {
        respond("[$TRACK]")
        val tracks = api.libraryTracks(offset = 200, limit = 100)
        assertEquals(1, tracks.size)
        val request = request()
        assertEquals("/proxy/music/api", request.url.encodedPath)
        assertEquals("POST", request.method)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertNull(request.url.query)
        val envelope = JsonParser.parseString(request.body!!.utf8()).asJsonObject
        assertEquals("music/tracks/library_items", envelope["command"].asString)
        assertTrue(envelope["message_id"].asString.isNotBlank())
        assertEquals(200, envelope["args"].asJsonObject["offset"].asInt)
        assertEquals(100, envelope["args"].asJsonObject["limit"].asInt)
        assertFalse(envelope.toString().contains("test-token"))
    }

    @Test fun metadataKeepsProviderIdentityFlacAndEmbeddedLyrics() = runBlocking {
        respond(TRACK)
        val track = api.track("library://track/42")
        assertEquals(123456L, track.durationMs)
        assertEquals("flac", track.codec)
        assertEquals(96000, track.sampleRate)
        assertEquals("[00:01.00]Hello", track.syncedLyrics)
        assertEquals("Hello", track.lyrics)
        val song = track.toProviderSong()
        assertEquals("library://track/42", song.providerId)
        assertEquals("Artist A, Artist B", song.artist)
        assertNull(song.artworkUrl)
    }

    @Test fun olderPagedListsAndUnknownFieldsAreAccepted() = runBlocking {
        respond("""{"items":[$TRACK],"count":1,"new_server_field":true}""")
        assertEquals("Song", api.libraryTracks().single().title)
    }

    @Test fun queueOccurrencesAreNeverDeduplicatedByTrack() = runBlocking {
        respond("""[{"queue_item_id":"occ-a","name":"Song","media_item":$TRACK},{"queue_item_id":"occ-b","name":"Song","media_item":$TRACK}]""")
        val items = api.queueItems("queue")
        assertEquals(listOf("occ-a", "occ-b"), items.map { it.occurrenceId })
        assertEquals(items[0].track!!.uri, items[1].track!!.uri)
    }

    @Test fun groupedPlayerUsesServerQueueAndPausedState() = runBlocking {
        respond("""{"queue_id":"leader","state":"paused","items":2,"current_index":1,"current_item":{"queue_item_id":"occ-b"},"elapsed_time":12.5,"shuffle_enabled":true,"repeat_mode":"all"}""")
        val queue = api.activeQueue("child")!!
        assertEquals("leader", queue.id)
        assertEquals("paused", queue.state)
        assertEquals("occ-b", queue.currentOccurrenceId)
        assertEquals(1, queue.currentIndex)
        assertEquals(12.5, queue.elapsedSeconds, 0.0)
        assertTrue(queue.shuffle)
        assertEquals("all", queue.repeat)
    }

    @Test fun explicitSelectionUsesTheRequestedOccurrenceNotTheFirstTrack() = runBlocking {
        val commands = mutableListOf<Pair<String, JsonObject>>()
        val recording = MusicAssistantApi(MusicAssistantTransport { command, args ->
            commands += command to args
            if (command == "player_queues/items") JsonParser.parseString(
                """[{"queue_item_id":"a1","name":"Song","media_item":$TRACK},
                    {"queue_item_id":"a2","name":"Song","media_item":$TRACK},
                    {"queue_item_id":"a3","name":"Song","media_item":$TRACK}]""")
            else com.google.gson.JsonNull.INSTANCE
        })
        recording.replaceQueueAndSelect("queue", List(3) { "library://track/42" }, 2, 12)
        assertEquals(listOf("player_queues/clear", "player_queues/shuffle", "player_queues/play_media",
            "player_queues/items", "player_queues/play_index"), commands.map { it.first })
        assertEquals("add", commands[2].second["option"].asString)
        assertFalse(commands[2].second.has("start_item"))
        assertEquals("a3", commands.last().second["index"].asString)
        assertEquals(12, commands.last().second["seek_position"].asInt)
    }

    @Test fun shuffleIsAppliedOnlyAfterSelectingTheExactOccurrence() = runBlocking {
        val commands = mutableListOf<Pair<String, JsonObject>>()
        val recording = MusicAssistantApi(MusicAssistantTransport { command, args ->
            commands += command to args
            if (command == "player_queues/items") JsonParser.parseString(
                """[{"queue_item_id":"selected","name":"Song","media_item":$TRACK}]""")
            else com.google.gson.JsonNull.INSTANCE
        })
        recording.replaceQueueAndSelect("queue", listOf("library://track/42"), 0, shuffle = true)
        val toggles = commands.filter { it.first == "player_queues/shuffle" }.map { it.second["shuffle_enabled"].asBoolean }
        assertEquals(listOf(false, true), toggles)
        assertEquals("player_queues/play_index", commands[commands.lastIndex - 1].first)
        assertEquals("player_queues/shuffle", commands.last().first)
    }

    @Test fun missingTrackInReplacementDoesNotStartTheWrongOccurrence() = runBlocking {
        val commands = mutableListOf<String>()
        val recording = MusicAssistantApi(MusicAssistantTransport { command, _ ->
            commands += command
            if (command == "player_queues/items") JsonParser.parseString("[]")
            else com.google.gson.JsonNull.INSTANCE
        })
        try {
            recording.replaceQueueAndSelect("queue", listOf("library://track/42"), 0)
            fail("An incomplete queue must be rejected")
        } catch (_: IllegalStateException) { }
        assertFalse(commands.contains("player_queues/play_index"))
        assertEquals(1, commands.count { it == "player_queues/play_media" })
    }

    @Test fun inactivePlayerHasNoQueue() = runBlocking {
        respond("null")
        assertNull(api.activeQueue("idle"))
    }

    @Test fun stoppedPlayerRetainsServerResumePositionAfterTransfer() = runBlocking {
        respond("""{"queue_id":"target","state":"idle","elapsed_time":0,"resume_pos":42,"items":3,"current_index":1}""")
        val queue = api.activeQueue("target")!!
        assertEquals("idle", queue.state)
        assertEquals(0.0, queue.elapsedSeconds, 0.0)
        assertEquals(42.0, queue.resumePositionSeconds, 0.0)
        assertEquals(1, queue.currentIndex)
    }

    @Test fun transferDoesNotForcePlaybackOnPausedSource() = runBlocking {
        respond("null")
        api.transfer("from", "to")
        val envelope = body()
        assertEquals("player_queues/transfer", envelope["command"].asString)
        val args = envelope["args"].asJsonObject
        assertEquals("from", args["source_queue_id"].asString)
        assertEquals("to", args["target_queue_id"].asString)
        assertFalse(args.has("auto_play"))
    }

    @Test fun appendPreservesOrderAndDuplicatesWithoutReplacingPlayback() = runBlocking {
        respond("null")
        val uris = listOf("library://track/1", "library://track/2", "library://track/1")
        api.append("queue", uris)
        val args = body()["args"].asJsonObject
        assertEquals("add", args["option"].asString)
        assertEquals(uris, args["media"].asJsonArray.map { it.asString })
    }

    @Test fun failedMutationIsNotRetriedOrReplaced() = runBlocking {
        respond("private token test-token", 500)
        val error = runCatching { api.append("queue", listOf("library://track/1")) }.exceptionOrNull()
        assertEquals(MusicAssistantFailure.SERVER, (error as MusicAssistantApiException).failure)
        assertFalse(error.toString().contains("test-token"))
        assertNull(error.cause)
        assertEquals(1, server.requestCount)
    }

    @Test fun authenticationAndPermissionFailuresStayDistinct() = runBlocking {
        for ((code, failure) in listOf(401 to MusicAssistantFailure.AUTHENTICATION, 403 to MusicAssistantFailure.PERMISSION)) {
            respond("credential information", code)
            val error = runCatching { api.players() }.exceptionOrNull() as MusicAssistantApiException
            assertEquals(failure, error.failure)
            assertFalse(error.message!!.contains("credential information"))
        }
    }

    @Test fun retryAfterDoesNotReplayAQueueMutation() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).addHeader("Retry-After", "0").body("busy").build())
        // A buggy retry would consume this acknowledgement and conceal the first failure.
        respond("null")
        val error = runCatching { api.append("queue", listOf("library://track/1")) }.exceptionOrNull()
        assertEquals(MusicAssistantFailure.SERVER, (error as MusicAssistantApiException).failure)
        assertEquals(1, server.requestCount)
    }

    @Test fun slowQueuePreparationCanFinishWithoutReplayingTheCommand() = runBlocking {
        // Reproduces a cold real server taking longer than OkHttp's default 10s read limit.
        server.enqueue(MockResponse.Builder().body("null").headersDelay(11, TimeUnit.SECONDS).build())
        api.play("queue", listOf("library://track/1"))
        assertEquals(1, server.requestCount)
    }

    @Test fun redirectDoesNotForwardCredentialsEvenOnSameHost() = runBlocking {
        server.enqueue(MockResponse.Builder().code(307).addHeader("Location", server.url("/other")).build())
        val error = runCatching { api.players() }.exceptionOrNull() as MusicAssistantApiException
        assertEquals(MusicAssistantFailure.REDIRECT, error.failure)
        assertEquals(1, server.requestCount)
    }

    @Test fun loginUsesOnlyLoginRouteAndReturnsToken() = runBlocking {
        respond("""{"success":true,"token":"new-token","user":{"username":"friend"}}""")
        assertEquals("new-token", transport.login("friend", "password"))
        val request = request()
        assertEquals("/proxy/music/auth/login", request.url.encodedPath)
        assertNull(request.headers["Authorization"])
        val payload = JsonParser.parseString(request.body!!.utf8()).asJsonObject
        assertEquals("builtin", payload["provider_id"].asString)
        assertEquals("friend", payload["credentials"].asJsonObject["username"].asString)
    }

    @Test fun invalidCatalogDoesNotBecomeAnEmptySuccessfulRefresh() = runBlocking {
        for (payload in listOf("<html>Login</html>", "{}", "{\"items\":null}")) {
            respond(payload)
            val error = runCatching { api.libraryTracks() }.exceptionOrNull() as MusicAssistantApiException
            assertEquals(MusicAssistantFailure.INVALID_RESPONSE, error.failure)
        }
    }

    @Test fun oversizedResponseIsRejectedBeforeParsing() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]")
            .setHeader("Content-Length", MusicAssistantHttpTransport.MAX_RESPONSE_BYTES + 1).build())
        val error = runCatching { api.players() }.exceptionOrNull() as MusicAssistantApiException
        assertEquals(MusicAssistantFailure.RESPONSE_TOO_LARGE, error.failure)
    }

    @Test fun cancellationCancelsInFlightHttpCall() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]").headersDelay(5, TimeUnit.SECONDS).build())
        val job = launch { api.players() }
        // Await arrival without blocking the runBlocking event loop which launches the request.
        withTimeout(5000) {
            while (server.requestCount == 0) kotlinx.coroutines.delay(10)
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(1, server.requestCount)
    }

    @Test fun mutationsWaitForPreviousAcknowledgement() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()
        val session = MusicAssistantApi { command, _ ->
            calls.add(command)
            if (calls.size == 1) { entered.complete(Unit); release.await() }
            com.google.gson.JsonNull.INSTANCE
        }
        val first = async { session.playerAction("p", MusicAssistantApi.PlayerAction.PAUSE) }
        entered.await()
        val second = async { session.volume("p", 50) }
        kotlinx.coroutines.yield()
        assertEquals(listOf("players/cmd/pause"), calls)
        release.complete(Unit)
        first.await(); second.await()
        assertEquals(listOf("players/cmd/pause", "players/cmd/volume_set"), calls)
    }

    @Test fun removingFavoriteClearsInsteadOfDisliking() = runBlocking {
        respond(TRACK)
        respond("null")
        api.favorite("library://track/42", false)
        assertEquals("music/item_by_uri", body()["command"].asString)
        val envelope = body()
        assertEquals("music/favorites/remove_item", envelope["command"].asString)
        assertEquals("track", envelope["args"].asJsonObject["media_type"].asString)
        assertEquals("42", envelope["args"].asJsonObject["library_item_id"].asString)
        assertFalse(envelope["args"].asJsonObject.has("favorite"))
    }

    @Test fun addingFavoriteUsesStableCommandAndOriginalUri() = runBlocking {
        respond("null")
        api.favorite("filesystem_local://track/Music/song.flac", true)
        val envelope = body()
        assertEquals("music/favorites/add_item", envelope["command"].asString)
        assertEquals("filesystem_local://track/Music/song.flac", envelope["args"].asJsonObject["item"].asString)
    }

    @Test fun removingProviderFavoriteResolvesLibraryIdentityFirst() = runBlocking {
        respond("""{"item_id":"folder/song.flac","provider":"filesystem_local--123","media_type":"track"}""")
        respond(TRACK)
        respond("null")
        api.favorite("filesystem_local--123://track/folder/song.flac", false)
        body()
        val lookup = body()
        assertEquals("music/get_library_item", lookup["command"].asString)
        assertEquals("folder/song.flac", lookup["args"].asJsonObject["item_id"].asString)
        assertEquals("filesystem_local--123", lookup["args"].asJsonObject["provider_instance_id_or_domain"].asString)
        assertEquals("42", body()["args"].asJsonObject["library_item_id"].asString)
    }

    @Test fun absentLibraryMappingDoesNotMutateAnUnrelatedFavorite() = runBlocking {
        respond("""{"item_id":"42","provider":"provider--123","media_type":"track"}""")
        respond("null")
        api.favorite("provider--123://track/42", false)
        assertEquals(2, server.requestCount)
    }

    @Test fun groupArgumentsAndModernPlayerFieldsArePreserved() = runBlocking {
        respond("null")
        api.groupMembers("leader", listOf("phone", "phone"), listOf("speaker"))
        val envelope = body()
        assertEquals("players/cmd/set_members", envelope["command"].asString)
        val args = envelope["args"].asJsonObject
        assertEquals("leader", args["target_player"].asString)
        assertEquals(listOf("phone"), args["player_ids_to_add"].asJsonArray.map { it.asString })
        respond("""[{"player_id":"leader","display_name":"Living room","available":true,"group_members":["phone","speaker"],"supported_features":["set_members","future_feature"]}]""")
        val player = api.players().single()
        assertEquals("Living room", player.name)
        assertEquals(listOf("phone", "speaker"), player.groupMembers)
        assertTrue(player.supportedFeatures.contains("future_feature"))
    }

    @Test fun invalidInputDoesNotReachServer() = runBlocking {
        assertTrue(runCatching { api.libraryTracks(-1) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { api.volume("p", 101) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { api.groupMembers("p", listOf("a"), listOf("a")) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, server.requestCount)
    }

    companion object {
        private const val TRACK = """{"uri":"library://track/42","item_id":"42","provider":"library","media_type":"track","name":"Song","duration":123.456,"artists":[{"name":"Artist A"},{"name":"Artist B"}],"album":{"name":"Album","uri":"library://album/7"},"favorite":true,"metadata":{"lyrics":"Hello","lrc_lyrics":"[00:01.00]Hello"},"provider_mappings":[{"available":true,"audio_format":{"content_type":"flac","sample_rate":96000,"channels":2}}]}"""
    }
}
