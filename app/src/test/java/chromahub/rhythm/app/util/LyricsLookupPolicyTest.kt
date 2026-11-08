package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.LyricsSourcePreference
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LyricsLookupPolicyTest {
    @Test fun apiFirstTimeoutIncludesAdmissionAndStillUsesEmbeddedThenLocal() = runBlocking {
        withTimeout(5_000) {
            val gate = OptionalWorkGate(OptionalWorkPolicy(foreground = true, saver = true,
                blocked = setOf(OptionalWork.LYRICS)))
            for (embeddedAvailable in listOf(true, false)) {
                val visited = mutableListOf<LyricsOrigin>()
                val saved = LyricsData("Saved words", "[00:01.00]Saved words")
                val found = firstAvailableLyrics(LyricsSourcePreference.API_FIRST, fetch = { origin ->
                    visited += origin
                    when (origin) {
                        LyricsOrigin.API -> boundedLyricsLookup(50) {
                            gate.run(OptionalWork.LYRICS) { fail("Blocked lookup started"); saved }
                        }
                        LyricsOrigin.EMBEDDED -> if (embeddedAvailable) saved else null
                        LyricsOrigin.LOCAL -> saved
                    }
                })
                assertEquals(saved, found?.second)
                assertEquals(if (embeddedAvailable) LyricsOrigin.EMBEDDED else LyricsOrigin.LOCAL, found?.first)
                assertEquals(if (embeddedAvailable) listOf(LyricsOrigin.API, LyricsOrigin.EMBEDDED)
                    else listOf(LyricsOrigin.API, LyricsOrigin.EMBEDDED, LyricsOrigin.LOCAL), visited)
            }
            // Timed-out admission must not leave a phantom slot behind.
            gate.update(OptionalWorkPolicy(foreground = true))
            assertEquals("available", gate.run(OptionalWork.LYRICS) { "available" })
        }
    }

    @Test fun stalledPreferredProviderDoesNotPreventOtherProvidersFromStarting() = runBlocking {
        withTimeout(5_000) {
            var cancelled = false
            var fallbackStarted = false
            val result = firstAvailableLyricsProvider(listOf(
                { try { awaitCancellation() } finally { cancelled = true } },
                { fallbackStarted = true; "fallback" }
            ), timeoutMs = 50)
            assertEquals("fallback", result)
            assertTrue(fallbackStarted)
            assertTrue(cancelled)
        }
    }

    @Test fun preferredResultWinsAndCancelsUnusedRequests() = runBlocking {
        withTimeout(5_000) {
            val otherStarted = CompletableDeferred<Unit>()
            var otherCancelled = false
            val result = firstAvailableLyricsProvider(listOf(
                { otherStarted.await(); "preferred" },
                { try { otherStarted.complete(Unit); awaitCancellation() }
                  finally { otherCancelled = true } }
            ))
            assertEquals("preferred", result)
            assertTrue(otherCancelled)
        }
    }

    @Test fun providerFailureAndPlainBackupDoNotHideATimedResult() = runBlocking {
        assertEquals("timed", firstAvailableLyricsProvider(listOf(
            { throw java.io.IOException("offline") }, { "plain" }, { "timed" }
        ), preferred = { it != "plain" }))
        assertEquals("plain", firstAvailableLyricsProvider(listOf(
            { "plain" }, { null }
        ), preferred = { it != "plain" }))
    }

    @Test fun changingSongCancelsLookupWithoutContinuingFileFallback() = runBlocking {
        withTimeout(5_000) {
            val started = CompletableDeferred<Unit>()
            var cancelled = false
            val job = launch {
                firstAvailableLyrics(LyricsSourcePreference.API_FIRST, fetch = { origin ->
                    check(origin == LyricsOrigin.API) { "Cancelled lookup continued file fallback" }
                    boundedLyricsLookup {
                        firstAvailableLyricsProvider<LyricsData>(listOf({
                            try { started.complete(Unit); awaitCancellation() }
                            finally { cancelled = true }
                        }))
                    }
                })
            }
            started.await()
            job.cancelAndJoin()
            assertTrue(cancelled)
        }
    }
}
