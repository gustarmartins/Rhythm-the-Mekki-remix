package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import chromahub.rhythm.app.util.restorePlaybackOccurrences
import org.junit.Assert.*
import org.junit.Test

class RestoredQueuePlaybackTest {
    private fun traversal(order: ShuffleOrder): List<Int> {
        val result = mutableListOf<Int>()
        var index = order.firstIndex
        while (index != C.INDEX_UNSET) {
            check(result.size < order.length) { "Cyclic traversal" }
            result.add(index)
            index = order.getNextIndex(index)
        }
        return result
    }

    @Test fun largeSavedShuffleRetainsPlayedSegmentAndUpcomingOrder() {
        val ids = List(2257) { "song-${(it * 37) % 2257}" }
        val restored = restorePlaybackOccurrences(ids, 574, 130382L, ids.associateWith { it })!!
        val order = RhythmShuffleOrder.forRestoredTraversal(restored.items.size, true)!!
        assertEquals(ids, traversal(order).map { restored.items[it] })
        assertEquals(ids[574], restored.items[restored.index])
        assertEquals(573, order.getPreviousIndex(restored.index))
        assertEquals(575, order.getNextIndex(restored.index))
        assertEquals(130382L, restored.positionMs)
        // Another save/load must not progressively reshuffle the queue.
        val savedAgain = traversal(order).map { restored.items[it] }
        assertEquals(ids, savedAgain)
    }

    @Test fun removedFilesAndDuplicateOccurrencesKeepCurrentAndNext() {
        val ids = listOf("b", "missing", "a", "b", "d", "c")
        val restored = restorePlaybackOccurrences(ids, 3, 500L,
            listOf("a", "b", "c", "d").associateWith { it })!!
        val order = RhythmShuffleOrder.forRestoredTraversal(restored.items.size, true)!!
        assertEquals(listOf("b", "a", "b", "d", "c"), traversal(order).map { restored.items[it] })
        assertEquals(2, restored.index)
        assertEquals("d", restored.items[order.getNextIndex(restored.index)])
        assertEquals(500L, restored.positionMs)
    }

    @Test fun playNextAndAppendAfterRestoreRetainExistingUpcomingOrder() {
        val order = RhythmShuffleOrder.forRestoredTraversal(6, true)!!
        val inserted = order.cloneAndInsert(3, 2)
        assertEquals((0 until 8).toList(), traversal(inserted))
        assertEquals(3, inserted.getNextIndex(2))
        assertEquals(5, inserted.getNextIndex(4))
        val appended = inserted.cloneAndInsert(8, 3)
        assertEquals((0 until 11).toList(), traversal(appended))
    }

    @Test fun restoringWithoutShuffleRetainsThePlayersLatentRandomOrder() {
        assertNull(RhythmShuffleOrder.forRestoredTraversal(2257, false))
    }
}
