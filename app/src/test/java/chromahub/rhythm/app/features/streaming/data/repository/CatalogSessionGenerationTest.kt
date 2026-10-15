/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CatalogSessionGenerationTest {
    @Test fun delayedProviderResponseCannotRestoreCatalogAfterLogout(): Unit = runBlocking {
        val gate = CatalogSessionGeneration()
        val generation = gate.select("MUSIC_ASSISTANT") { }
        var songs = listOf("old account")
        val response = CompletableDeferred<Unit>()
        val work = async {
            response.await()
            gate.publish(generation) { songs = listOf("late old response") }
        }
        gate.close { songs = emptyList() }
        response.complete(Unit)
        try { work.await(); fail("Old request must be cancelled") } catch (_: CancellationException) { }
        assertTrue(songs.isEmpty())
    }

    @Test fun reconnectDoesNotAllowOldResponseIntoNewAccount() {
        val gate = CatalogSessionGeneration()
        val old = gate.current()
        gate.close { }
        gate.open()
        val replacement = gate.current()
        var songs = listOf("new account")
        try { gate.publish(old) { songs = listOf("old account") }; fail("Old generation accepted") }
        catch (_: CancellationException) { }
        assertEquals(listOf("new account"), songs)
        gate.publish(replacement) { songs = listOf("new catalog") }
        assertEquals(listOf("new catalog"), songs)
    }

    @Test fun serviceObserverDoesNotInvalidateTheFirstRequestForThatSameService() {
        val gate = CatalogSessionGeneration()
        val cold = gate.select("MUSIC_ASSISTANT") { fail("Cold cache must remain usable") }
        assertTrue(gate.isCurrent(cold))
        val switched = gate.select("JELLYFIN") { }
        val observer = gate.select("JELLYFIN") { fail("Duplicate service change") }
        assertEquals(switched, observer)
        assertFalse(gate.isCurrent(cold))
        gate.close { }
        assertFalse(gate.isCurrent(gate.select("JELLYFIN") { }))
    }
    @Test fun failedReplacementLoginCanReopenPreservedCatalogWithoutAcceptingOldRequests() {
        val gate = CatalogSessionGeneration()
        val oldRequest = gate.current()
        var songs = listOf("existing account")
        gate.close { /* Login attempt retains the existing catalog. */ }
        gate.open() // Original credentials remain configured after failed authentication.
        gate.publish(gate.current()) { assertEquals(listOf("existing account"), songs) }
        assertFalse(gate.isCurrent(oldRequest))
    }

    @Test fun logoutStillAllowsCurrentOfflineDownloadsButRejectsOldOnlineResponse() {
        val gate = CatalogSessionGeneration()
        val online = gate.current()
        gate.close { }
        var songs = emptyList<String>()
        gate.publish(gate.current(), allowClosed = true) { songs = listOf("download") }
        assertEquals(listOf("download"), songs)
        try { gate.publish(online, allowClosed = true) { songs = listOf("stale network") }; fail("Old generation accepted") }
        catch (_: CancellationException) { }
        assertEquals(listOf("download"), songs)
    }

}
