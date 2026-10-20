package chromahub.rhythm.app.features.streaming

import chromahub.rhythm.app.features.streaming.domain.searchStreamingSections
import chromahub.rhythm.app.features.streaming.domain.StreamingSearchController
import chromahub.rhythm.app.features.streaming.domain.streamingSearchEnabled
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class StreamingSearchControllerTest {
    private class Session {
        var mode = "LOCAL"
        var authenticated = false
        var online = true
        var offlineMode = false
        fun enabled() = streamingSearchEnabled(mode, authenticated, online, offlineMode)
    }

    private class Harness(scope: CoroutineScope, val session: Session, debounce: Long = 0,
                          search: suspend (String) -> List<String> = { listOf(it) }) {
        val requests = mutableListOf<String>()
        val errors = mutableListOf<Exception>()
        var results = emptyList<String>()
        var loading = false
        val controller = StreamingSearchController(scope, session::enabled, emptyList(),
            { query, _ -> requests += query; search(query) }, { results = it }, { loading = it }, { errors += it }, debounce)
    }

    @Test fun typingWithRhythmGoNeverEnabledDoesNotContactProviderOrPrompt() = runBlocking {
        val h = Harness(this, Session())
        for (query in listOf("T", "Tw", "Twice", "Twice Brave")) {
            h.controller.submit(query)
            yield()
        }
        assertTrue(h.requests.isEmpty())
        assertTrue(h.errors.isEmpty())
        assertTrue(h.results.isEmpty())
        assertFalse(h.loading)
    }

    @Test fun savedCredentialsDoNotEnableSearchInLocalMode() = runBlocking {
        val h = Harness(this, Session().apply { authenticated = true })
        h.controller.submit("Brave"); yield()
        assertTrue(h.requests.isEmpty())
    }

    @Test fun disconnectedAndOfflineStreamingSessionsStayQuiet() = runBlocking {
        for (session in listOf(
            Session().apply { mode = "STREAMING" },
            Session().apply { mode = "STREAMING"; authenticated = true; online = false },
            Session().apply { mode = "STREAMING"; authenticated = true; offlineMode = true }
        )) {
            val h = Harness(this, session)
            h.controller.submit("Brave"); yield()
            assertTrue(h.requests.isEmpty()); assertTrue(h.errors.isEmpty()); assertFalse(h.loading)
        }
    }

    @Test fun enablingRhythmGoSearchesTheExistingQueryAndClearingRemovesResults() = runBlocking {
        val session = Session()
        val h = Harness(this, session)
        h.controller.submit("Brave"); yield()
        session.mode = "STREAMING"; session.authenticated = true
        h.controller.submit("Brave"); yield()
        assertEquals(listOf("Brave"), h.requests)
        assertEquals(listOf("Brave"), h.results)
        h.controller.submit(""); yield()
        assertTrue(h.results.isEmpty()); assertFalse(h.loading)
    }

    @Test fun switchingToLocalCancelsPendingErrorsAndAllowsSameQueryOnReturn() = runBlocking {
        withTimeout(5_000) {
            val session = Session().apply { mode = "STREAMING"; authenticated = true }
            val release = CompletableDeferred<Unit>()
            val h = Harness(this, session) { release.await(); error("Connection failed") }
            h.controller.submit("Brave"); yield(); assertTrue(h.loading)
            session.mode = "LOCAL"; h.controller.submit(""); yield()
            release.complete(Unit); yield()
            assertTrue(h.errors.isEmpty()); assertFalse(h.loading)
            session.mode = "STREAMING"; h.controller.submit("Brave"); yield()
            assertEquals(listOf("Brave", "Brave"), h.requests)
            assertEquals(1, h.errors.size)
        }
    }

    @Test fun supersededProviderCannotOverwriteNewerResultsOrClearItsLoadingState() = runBlocking {
        withTimeout(5_000) {
            val session = Session().apply { mode = "STREAMING"; authenticated = true }
            val oldRelease = CompletableDeferred<Unit>()
            val newRelease = CompletableDeferred<Unit>()
            val h = Harness(this, session) { query ->
                if (query == "old") withContext(NonCancellable) { oldRelease.await() }
                else newRelease.await()
                listOf(query)
            }
            h.controller.submit("old"); yield()
            h.controller.submit("new"); yield()
            oldRelease.complete(Unit); yield(); yield()
            assertTrue(h.loading); assertTrue(h.results.isEmpty()); assertTrue(h.errors.isEmpty())
            newRelease.complete(Unit); yield()
            assertEquals(listOf("new"), h.results); assertFalse(h.loading)
        }
    }

    @Test fun rapidTypingDebouncesProviderRequests() = runBlocking {
        withTimeout(5_000) {
            val session = Session().apply { mode = "STREAMING"; authenticated = true }
            val h = Harness(this, session, debounce = 20)
            h.controller.submit("T"); yield()
            h.controller.submit("Tw"); yield()
            h.controller.submit("Twice"); delay(50)
            assertEquals(listOf("Twice"), h.requests)
        }
    }
    @Test fun songsPublishBeforeSlowCategoriesAndSurviveUnsupportedCategory() = runBlocking {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            val songsPublished = CompletableDeferred<Unit>()
            var results = emptyList<String>()
            var loading = false
            val errors = mutableListOf<Exception>()
            val controller = StreamingSearchController(this, { true }, emptyList<String>(),
                search = { _, publish ->
                    searchStreamingSections(emptyList(), listOf(
                        { val update: (List<String>) -> List<String> = { it + "DUN DUN" }; update },
                        { release.await(); throw java.io.IOException("albums unavailable") }
                    ), publish)
                },
                onResults = { results = it; if (it.isNotEmpty()) songsPublished.complete(Unit) },
                onLoading = { loading = it }, onError = { errors += it }, debounceMillis = 0)
            controller.submit("EVERGLOW")
            songsPublished.await()
            assertEquals(listOf("DUN DUN"), results)
            assertTrue(loading)
            release.complete(Unit)
            while (loading) yield()
            assertEquals(listOf("DUN DUN"), results)
            assertEquals("albums unavailable", errors.single().message)
        }
    }

    @Test fun cancelledPartialSearchCannotPublishIntoNewQuery() = runBlocking {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            var results = emptyList<String>()
            val controller = StreamingSearchController(this, { true }, emptyList<String>(),
                search = { query, publish ->
                    if (query == "old") withContext(NonCancellable) { release.await(); publish(listOf("stale")) }
                    listOf(query)
                }, onResults = { results = it }, onLoading = {}, onError = { throw it }, debounceMillis = 0)
            controller.submit("old"); yield()
            controller.submit("new"); yield()
            assertEquals(listOf("new"), results)
            release.complete(Unit); yield(); yield()
            assertEquals(listOf("new"), results)
        }
    }

}
