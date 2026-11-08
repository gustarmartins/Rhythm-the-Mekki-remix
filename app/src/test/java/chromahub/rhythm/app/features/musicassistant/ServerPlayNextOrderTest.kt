/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ServerPlayNextOrderTest {
    @Test fun serverShuffleRetainsDuplicateOccurrencesInRequestOrder() = runBlocking {
        val order = mutableListOf("history", "current", "b", "Jump-2", "a", "Brave", "Jump-1")
        restoreServerPlayNext(listOf("Jump-1", "Jump-2", "Brave"), "current",
            read = { "current" to order.toList() },
            move = { token, shift -> val from = order.indexOf(token); order.add(from + shift, order.removeAt(from)) })
        assertEquals(listOf("history", "current", "Jump-1", "Jump-2", "Brave", "b", "a"), order)
    }

    @Test fun bufferedServerRejectionIsReportedInsteadOfSilentSuccess() = runBlocking {
        try {
            restoreServerPlayNext(listOf("request"), "current",
                read = { "current" to listOf("current", "context", "request") }, move = { _, _ -> })
            fail("Unaccepted server edits must fail")
        } catch (_: IllegalStateException) { }
    }

    @Test fun playbackAdvancingDuringRemoteShuffleNeverMovesPlayedRequests() = runBlocking {
        var moves = 0
        try {
            restoreServerPlayNext(listOf("request"), "current",
                read = { "context" to listOf("current", "context", "request") }, move = { _, _ -> moves++ })
            fail("Changed current occurrence must abort")
        } catch (_: IllegalStateException) { }
        assertEquals(0, moves)
    }
}
