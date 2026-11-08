/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class QueueShufflePolicyTest {
    private val original = listOf("played", "current", "Jump-1", "Jump-2", "Brave", "a", "b", "c")
    private val requests = listOf("Jump-1", "Jump-2", "Brave")

    @Test fun everyShuffleKeepsHistoryAndFifoDuplicatesAheadOfContext() {
        val large = original + List(2077) { "library-$it" }
        repeat(100) { seed ->
            val plan = planQueueShuffle(large, 1, requests, large, true, Random(seed))
            assertEquals(large.take(5), plan.order.take(5))
            assertEquals(requests, plan.pending)
            assertEquals(large.toSet(), plan.order.toSet())
            assertEquals(large.size, plan.order.size)
            assertNotEquals(large.drop(5), plan.order.drop(5))
        }
    }

    @Test fun disablingShuffleRestoresOnlyContextAndKeepsLaterRequestsFirst() {
        val shuffled = planQueueShuffle(original, 1, requests, original, true, Random(7))
        val inserted = planPlayNextInsertion(shuffled.order, 1, shuffled.order.indices.toList(), shuffled.pending, 1)
        val withLater = shuffled.order.toMutableList().apply { add(inserted.insertionIndex, "Jet Rocket") }
        val restored = planQueueShuffle(withLater, 1, requests + "Jet Rocket", original, false)
        assertEquals(original.take(5) + "Jet Rocket" + original.drop(5), restored.order)
    }

    @Test fun consumingSkippingAndDeletingRequestsNeverResurrectsThem() {
        assertEquals(listOf("Brave"), planQueueShuffle(original, 3, requests, original, true).pending)
        assertTrue(planQueueShuffle(original, 5, requests, original, true).pending.isEmpty())
        val deleted = original - "Jump-2"
        assertEquals(listOf("Jump-1", "Brave"), planQueueShuffle(deleted, 1, requests, original, false).pending)
        val consumed = remainingPlayNext(original, 3, requests)
        assertEquals(listOf("Brave"), planQueueShuffle(original, 1, consumed, original, true).pending)
    }

    @Test fun dragAndRemoveContractsSurviveTheNextShuffle() {
        val moved = planQueueEntryEdit(original, 1, requests, "Brave", QueueEntryCommand.MOVE, "Jump-1")
        assertEquals(listOf("Brave", "Jump-1", "Jump-2"), moved.pending)
        val shuffled = planQueueShuffle(moved.order, 1, moved.pending, original, true)
        assertEquals(moved.order.take(5), shuffled.order.take(5))
        val removed = planQueueEntryEdit(shuffled.order, 1, shuffled.pending, "Jump-1", QueueEntryCommand.REMOVE)
        assertEquals(listOf("Brave", "Jump-2"), planQueueShuffle(removed.order, 1, removed.pending, original, false).pending)
    }

    @Test fun shuffleAfterAdvancingKeepsNewPlayedPrefixAndAllContinuationSongs() {
        val shuffled = planQueueShuffle(original, 1, requests, original, true, Random(9)).order
        val continued = shuffled + listOf("library-1", "library-2")
        val restored = planQueueShuffle(continued, 5, emptyList(), original, false)
        assertEquals(continued.take(6), restored.order.take(6))
        assertEquals(listOf("library-1", "library-2"), restored.order.takeLast(2))
        assertEquals(continued.toSet(), restored.order.toSet())
    }

    @Test fun freshShuffleSelectionIncludesSongsBeforeTheSelectedOccurrence() {
        val plan = planQueueShuffle(original, 3, requests, original, true, Random(1), freshSelection = true)
        assertEquals("Jump-2", plan.order.first())
        assertEquals(original.toSet(), plan.order.toSet())
        assertTrue(plan.pending.isEmpty())
    }

    @Test fun savedOccurrencesDistinguishDuplicatesAndRejectStaleIdLists() {
        val saved = SavedPlaybackQueue(listOf("song", "gone", "song", "tail"),
            listOf("song-1", "gone-1", "song-2", "tail-1"), listOf("song-2"), emptyList(), true)
        assertTrue(saved.matches(listOf("song", "gone", "song", "tail")))
        assertFalse(saved.matches(listOf("song", "tail")))
        assertEquals(listOf("song-1", "song-2", "tail-1"), saved.survivingTokens(setOf("song", "tail")))
        assertFalse(saved.copy(tokens = listOf("same", "same", "other", "last")).matches(saved.ids))
    }
}
