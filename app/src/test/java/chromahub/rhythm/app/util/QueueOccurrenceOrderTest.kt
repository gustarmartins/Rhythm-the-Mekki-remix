package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class QueueOccurrenceOrderTest {
    @Test fun clearLargeQueueRetainsExactOccurrenceInAtMostTwoRemovals() {
        for (index in listOf(0, 5_000, 9_999)) {
            val queue = (0 until 10_000).toMutableList()
            val ranges = clearQueueRemovalRanges(queue.size, index)
            assertTrue(ranges.size <= 2)
            ranges.forEach { queue.subList(it.first, it.last + 1).clear() }
            assertEquals(listOf(index), queue)
        }
        assertTrue(clearQueueRemovalRanges(0, -1).isEmpty())
        assertTrue(clearQueueRemovalRanges(1, 0).isEmpty())
    }
    @Test fun duplicateKeysNeedOnlyOneReadPerOccurrence() {
        var reads = 0
        val ids = object : AbstractList<String>() {
            override val size = 10_000
            override fun get(index: Int): String { reads++; return "duplicate" }
        }
        val keys = queueOccurrenceKeys(ids)
        assertEquals(10_000, reads)
        assertEquals(10_000, keys.toSet().size)
        assertEquals("duplicate@10000", keys.last())
    }

    @Test fun unshuffleKeepsAnAddedDuplicateOccurrence() {
        assertEquals(listOf("current", "a", "b", "a"),
            restoreQueueOccurrences(listOf("current", "a", "a", "b"), 0, listOf("current", "a", "b")) { it })
    }
    @Test fun playedDuplicateIsNotResurrected() {
        assertEquals(listOf("a", "current", "a", "b"),
            restoreQueueOccurrences(listOf("a", "current", "b", "a"), 1, listOf("a", "current", "a", "b")) { it })
    }
    @Test fun queuedLibraryContinuationSurvivesUnshuffle() {
        val queue = listOf("current", "contextB", "contextA", "libraryA", "libraryB")
        assertEquals(listOf("current", "contextA", "contextB", "libraryA", "libraryB"),
            restoreQueueOccurrences(queue, 0, listOf("current", "contextA", "contextB")) { it })
    }
    @Test fun missingOriginalTracksAreNotAddedBack() {
        assertEquals(listOf("current", "kept"),
            restoreQueueOccurrences(listOf("current", "kept"), 0, listOf("current", "missing", "kept")) { it })
    }
    @Test fun playedSegmentAndCurrentOccurrenceStayFixed() {
        assertEquals(listOf("played2", "played1", "current", "a", "b"),
            restoreQueueOccurrences(listOf("played2", "played1", "current", "b", "a"), 2,
                listOf("played1", "played2", "current", "a", "b")) { it })
    }
}
