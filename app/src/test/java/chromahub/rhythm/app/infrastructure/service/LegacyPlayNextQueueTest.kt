package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyPlayNextQueueTest {
    private fun <T> LegacyPlayNextQueue<T>.future() = upcoming.map { items[it] }
    private fun <T> LegacyPlayNextQueue<T>.past() = history.map { items[it] }

    @Test fun batchPlaysEverySelectedSongBeforeExistingShuffledFuture() {
        // Physical order differs from playback order: C, A, D, B.
        val result = insertLegacyPlayNext(
            listOf("A", "B", "C", "D"), 0, listOf(2), listOf(3, 1),
            listOf("X", "Y", "Z")
        )
        assertEquals(listOf("X", "Y", "Z", "D", "B"), result.future())
        assertEquals(listOf("C"), result.past())
        assertEquals("A", result.items[0])
        assertEquals(listOf("A", "X", "Y", "Z", "B", "C", "D"), result.items)
    }

    @Test fun consecutiveCommandsBeforeRecollapseKeepAllInsertionsUpcoming() {
        // Regression: the old first insertion restored the full queue at index 2;
        // the following controller commands still used collapsed index 1.
        var result = LegacyPlayNextQueue(listOf("A", "B", "C", "D"), listOf(0, 1), listOf(3))
        for (song in listOf("Z", "Y", "X")) {
            result = insertLegacyPlayNext(result.items, 2, result.history, result.upcoming, listOf(song))
        }
        assertEquals(listOf("X", "Y", "Z", "D"), result.future())
        assertEquals(listOf("A", "B"), result.past())
        assertEquals("C", result.items[2])
    }

    @Test fun laterPlayNextKeepsPreviousRequestsAheadOfShuffledRemainder() {
        val first = insertLegacyPlayNext(listOf("A", "B", "C", "D"), 2, listOf(1), listOf(0, 3), listOf("X"))
        val second = insertLegacyPlayNext(first.items, 2, first.history, first.upcoming, listOf("Y", "Z"))
        assertEquals(listOf("Y", "Z", "X", "A", "D"), second.future())
        assertEquals(first.past(), second.past())
    }

    @Test fun duplicateSongIdsRemainDistinctQueueOccurrences() {
        val result = insertLegacyPlayNext(listOf("A", "A", "B"), 1, listOf(0), listOf(2), listOf("A", "A"))
        assertEquals(listOf("A", "A", "B"), result.future())
        assertEquals(listOf(2, 3, 4), result.upcoming)
        assertEquals(listOf(0), result.history)
    }

    @Test fun insertAfterLastPhysicalItemStillPrecedesShuffledFuture() {
        val result = insertLegacyPlayNext(listOf("A", "B", "C"), 2, emptyList(), listOf(1, 0), listOf("X", "Y"))
        assertEquals(listOf("X", "Y", "B", "A"), result.future())
    }

    @Test fun insertionIntoSingleItemQueueCanAdvanceThroughEntireBatch() {
        val result = insertLegacyPlayNext(listOf("A"), 0, emptyList(), emptyList(), listOf("X", "Y", "Z"))
        val future = result.upcoming.toMutableList()
        val played = mutableListOf("A")
        while (future.isNotEmpty()) played += result.items[future.removeAt(0)]
        assertEquals(listOf("A", "X", "Y", "Z"), played)
    }

    @Test fun emptyBatchPreservesExactQueueAndNavigation() {
        val result = insertLegacyPlayNext(listOf("A", "B", "C"), 1, listOf(2), listOf(0), emptyList())
        assertEquals(listOf("A", "B", "C"), result.items)
        assertEquals(listOf(2), result.history)
        assertEquals(listOf(0), result.upcoming)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCurrentIndexCannotSilentlyEditWrongPosition() {
        insertLegacyPlayNext(listOf("A"), -1, emptyList(), emptyList(), listOf("X"))
    }
}
