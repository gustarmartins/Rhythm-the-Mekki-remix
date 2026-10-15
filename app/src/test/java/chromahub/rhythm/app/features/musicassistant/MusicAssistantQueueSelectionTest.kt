/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MusicAssistantQueueSelectionTest {
    @Test fun connectedPlayerWaitsForQueueRegistrationWithoutBlockingOrReplayingMutations() = runBlocking {
        var registered = false
        var reads = 0
        var replacements = 0
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            val queue = awaitMusicAssistantQueue(timeoutMs = 1000, pollIntervalMs = 1) {
                reads++
                if (registered) "server-queue" else null
            }
            replacements++
            queue
        }
        // We reached the caller while registration is pending: waiting yielded its dispatcher.
        assertFalse(pending.isCompleted)
        assertEquals(1, reads)
        assertEquals(0, replacements)
        registered = true
        assertEquals("server-queue", pending.await())
        assertEquals(2, reads)
        assertEquals(1, replacements)
    }

    @Test fun registrationTimeoutDoesNotStartAQueueMutation() = runBlocking {
        var mutations = 0
        val failure = runCatching {
            awaitMusicAssistantQueue(timeoutMs = 30, pollIntervalMs = 1) { null }
            mutations++
        }.exceptionOrNull()
        assertTrue(failure is TimeoutCancellationException)
        assertEquals(0, mutations)
    }

    @Test fun cancellationStopsRegistrationReadsAndDependentMutations() = runBlocking {
        val firstRead = CompletableDeferred<Unit>()
        var reads = 0
        var mutations = 0
        val pending = async {
            awaitMusicAssistantQueue(timeoutMs = 30_000, pollIntervalMs = 10_000) {
                reads++
                firstRead.complete(Unit)
                null
            }
            mutations++
        }
        firstRead.await()
        pending.cancelAndJoin()
        assertTrue(pending.isCancelled)
        assertEquals(1, reads)
        assertEquals(0, mutations)
    }

    @Test fun failedReadIsNotRetriedAsIfItWereDelayedRegistration() = runBlocking {
        val expected = IOException("offline")
        var reads = 0
        val failure = runCatching {
            awaitMusicAssistantQueue(pollIntervalMs = 1) {
                reads++
                throw expected
            }
        }.exceptionOrNull()
        assertSame(expected, failure)
        assertEquals(1, reads)
    }

    @Test fun failedReplacementCannotResumeThePreviousServerContext() {
        val selection = MusicAssistantQueueSelection()
        val requestedAlbum = selection.beginReplacement()
        var oldQueuePlays = 0
        // A failed replacement does not confirm its request, even if reconciliation sees an old queue.
        val failure = runCatching {
            if (selection.canPlay(requestedAlbum)) oldQueuePlays++
        }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(0, oldQueuePlays)
    }

    @Test fun successfulReplacementAllowsItsDependentPlay() {
        val selection = MusicAssistantQueueSelection()
        val requestedAlbum = selection.beginReplacement()
        selection.confirmReplacement(requestedAlbum, hasMedia = true)
        assertTrue(selection.canPlay(requestedAlbum))
    }

    @Test fun olderCompletionAndPlayCannotConfirmOrResumeANewerSelection() {
        val selection = MusicAssistantQueueSelection()
        val firstAlbum = selection.beginReplacement()
        val secondAlbum = selection.beginReplacement()
        selection.confirmReplacement(firstAlbum, hasMedia = true)
        assertFalse(selection.canPlay(firstAlbum))
        assertTrue(runCatching { selection.canPlay(secondAlbum) }.exceptionOrNull() is IllegalStateException)
        selection.confirmReplacement(secondAlbum, hasMedia = true)
        assertTrue(selection.canPlay(secondAlbum))
        assertFalse(selection.canPlay(firstAlbum))
    }

    @Test fun anotherExplicitSelectionCanRecoverAfterAFailure() {
        val selection = MusicAssistantQueueSelection()
        val failed = selection.beginReplacement()
        assertTrue(runCatching { selection.canPlay(failed) }.isFailure)
        val retry = selection.beginReplacement()
        selection.confirmReplacement(retry, hasMedia = true)
        assertTrue(selection.canPlay(retry))
        assertFalse(selection.canPlay(failed))
    }

    @Test fun clearingTheQueueDoesNotPermitResumingItsPreviousContents() {
        val selection = MusicAssistantQueueSelection()
        val clear = selection.beginReplacement()
        selection.confirmReplacement(clear, hasMedia = false)
        assertTrue(runCatching { selection.canPlay(clear) }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun clearThenAddAllowsPlayingTheNewContents() {
        val selection = MusicAssistantQueueSelection()
        val clear = selection.beginReplacement()
        selection.confirmReplacement(clear, hasMedia = false)
        selection.confirmAddition(clear)
        assertTrue(selection.canPlay(clear))
    }

    @Test fun addingToAnOldQueueCannotConfirmAFailedOrSupersededReplacement() {
        val selection = MusicAssistantQueueSelection()
        val failed = selection.beginReplacement()
        selection.confirmAddition(failed)
        assertTrue(runCatching { selection.canPlay(failed) }.isFailure)
        val newer = selection.beginReplacement()
        selection.confirmAddition(failed)
        assertTrue(runCatching { selection.canPlay(newer) }.isFailure)
    }

    @Test fun normalResumeWithoutAnExplicitReplacementRemainsAvailable() {
        val selection = MusicAssistantQueueSelection()
        assertTrue(selection.canPlay(selection.generation))
    }
}
