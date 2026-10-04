package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class QueueOccurrenceOrderTest {
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
