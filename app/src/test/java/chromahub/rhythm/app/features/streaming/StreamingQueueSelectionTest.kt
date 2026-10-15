/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming

import chromahub.rhythm.app.features.streaming.domain.resolveStreamingQueueSelection
import org.junit.Assert.*
import org.junit.Test

class StreamingQueueSelectionTest {
    @Test fun missingEarlierRowsDoNotShiftTheSelectedSong() {
        val selected = resolveStreamingQueueSelection(listOf("gone", "Boston", "folklore"), 2) {
            it.takeUnless { it == "gone" }
        }!!
        assertEquals(listOf("Boston", "folklore"), selected.items)
        assertEquals("folklore", selected.items[selected.startIndex])
    }
    @Test fun anUnavailableSelectionDoesNotPlayAnotherSong() {
        assertNull(resolveStreamingQueueSelection(listOf("Boston", "gone"), 1) { it.takeUnless { it == "gone" } })
    }
    @Test fun duplicateOccurrenceAndOrderArePreserved() {
        val selected = resolveStreamingQueueSelection(listOf("a", "b", "a"), 2) { it }!!
        assertEquals(listOf("a", "b", "a"), selected.items)
        assertEquals(2, selected.startIndex)
    }
}
