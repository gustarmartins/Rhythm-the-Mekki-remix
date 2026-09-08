/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class RhythmShuffleOrderTest {

    @Test
    fun testInitialShuffleLengthAndContents() {
        val count = 10
        val order = RhythmShuffleOrder(count, Random(42))
        assertEquals(count, order.length)

        val visited = mutableSetOf<Int>()
        var current = order.firstIndex
        while (current != C.INDEX_UNSET) {
            assertTrue(visited.add(current))
            current = order.getNextIndex(current)
        }
        assertEquals(count, visited.size)
        assertEquals((0 until count).toSet(), visited)
    }

    @Test
    fun testEmptyShuffleOrder() {
        val order = RhythmShuffleOrder(0)
        assertEquals(0, order.length)
        assertEquals(C.INDEX_UNSET, order.firstIndex)
        assertEquals(C.INDEX_UNSET, order.lastIndex)
        assertEquals(C.INDEX_UNSET, order.getNextIndex(0))
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(0))
    }

    @Test
    fun testPlayNextSingleItem() {
        // Given shuffled order: [2, 0, 4, 1, 3]
        // Currently playing: timeline index 0 (at position 1)
        val initialShuffled = intArrayOf(2, 0, 4, 1, 3)
        val order = RhythmShuffleOrder(initialShuffled)

        // Insert 1 item at timeline index 1 (Play Next after index 0)
        val updatedOrder = order.cloneAndInsert(1, 1)

        assertEquals(6, updatedOrder.length)
        // Existing items >= 1 are shifted by +1: 2->3, 4->5, 1->2, 3->4
        // New item (1) is placed right after 0: [3, 0, 1, 5, 2, 4]
        assertEquals(1, updatedOrder.getNextIndex(0))
        assertEquals(5, updatedOrder.getNextIndex(1))
        assertEquals(0, updatedOrder.getPreviousIndex(1))

        // Verify full traversal
        val traversal = mutableListOf<Int>()
        var current = updatedOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            traversal.add(current)
            current = updatedOrder.getNextIndex(current)
        }
        assertEquals(listOf(3, 0, 1, 5, 2, 4), traversal)
    }

    @Test
    fun testPlayNextMultipleItems() {
        // Given shuffled order: [2, 0, 4, 1, 3]
        // Currently playing: timeline index 0
        val initialShuffled = intArrayOf(2, 0, 4, 1, 3)
        val order = RhythmShuffleOrder(initialShuffled)

        // Insert 2 items at timeline index 1 (Play Next batch after index 0)
        val updatedOrder = order.cloneAndInsert(1, 2)

        assertEquals(7, updatedOrder.length)
        // New items 1, 2 are placed right after 0: [4, 0, 1, 2, 6, 3, 5]
        assertEquals(1, updatedOrder.getNextIndex(0))
        assertEquals(2, updatedOrder.getNextIndex(1))
        assertEquals(6, updatedOrder.getNextIndex(2))

        val traversal = mutableListOf<Int>()
        var current = updatedOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            traversal.add(current)
            current = updatedOrder.getNextIndex(current)
        }
        assertEquals(listOf(4, 0, 1, 2, 6, 3, 5), traversal)
    }

    @Test
    fun testAddToQueueSingleItem() {
        // Given shuffled order: [2, 0, 4, 1, 3]
        val initialShuffled = intArrayOf(2, 0, 4, 1, 3)
        val order = RhythmShuffleOrder(initialShuffled)

        // Append 1 item at the end of timeline (index 5)
        val updatedOrder = order.cloneAndInsert(5, 1)

        assertEquals(6, updatedOrder.length)
        // New item 5 should be appended at the end of the shuffled list: [2, 0, 4, 1, 3, 5]
        assertEquals(5, updatedOrder.lastIndex)
        assertEquals(5, updatedOrder.getNextIndex(3))

        val traversal = mutableListOf<Int>()
        var current = updatedOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            traversal.add(current)
            current = updatedOrder.getNextIndex(current)
        }
        assertEquals(listOf(2, 0, 4, 1, 3, 5), traversal)
    }

    @Test
    fun testAddToQueueMultipleItems() {
        // Given shuffled order: [2, 0, 4, 1, 3]
        val initialShuffled = intArrayOf(2, 0, 4, 1, 3)
        val order = RhythmShuffleOrder(initialShuffled)

        // Append 3 items at the end of timeline (index 5)
        val updatedOrder = order.cloneAndInsert(5, 3)

        assertEquals(8, updatedOrder.length)
        val traversal = mutableListOf<Int>()
        var current = updatedOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            traversal.add(current)
            current = updatedOrder.getNextIndex(current)
        }
        assertEquals(listOf(2, 0, 4, 1, 3, 5, 6, 7), traversal)
    }

    @Test
    fun testRemoveItem() {
        // Given [3, 0, 1, 5, 2, 4]
        val initialShuffled = intArrayOf(3, 0, 1, 5, 2, 4)
        val order = RhythmShuffleOrder(initialShuffled)

        // Remove item at timeline index 1
        val updatedOrder = order.cloneAndRemove(1, 2)

        assertEquals(5, updatedOrder.length)
        val traversal = mutableListOf<Int>()
        var current = updatedOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            traversal.add(current)
            current = updatedOrder.getNextIndex(current)
        }
        assertEquals(listOf(2, 0, 4, 1, 3), traversal)
    }

    @Test
    fun testCloneAndClear() {
        val order = RhythmShuffleOrder(intArrayOf(2, 0, 4, 1, 3))
        val cleared = order.cloneAndClear()
        assertEquals(0, cleared.length)
        assertEquals(C.INDEX_UNSET, cleared.firstIndex)
    }

    @Test
    fun testInsertOnEmptyOrder() {
        val order = RhythmShuffleOrder(0)
        val populated = order.cloneAndInsert(0, 10)
        assertEquals(10, populated.length)

        val visited = mutableSetOf<Int>()
        var current = populated.firstIndex
        while (current != C.INDEX_UNSET) {
            assertTrue(visited.add(current))
            current = populated.getNextIndex(current)
        }
        assertEquals((0 until 10).toSet(), visited)
    }

    @Test
    fun testOutOfBoundsIndexSafety() {
        val order = RhythmShuffleOrder(intArrayOf(2, 0, 1))
        assertEquals(C.INDEX_UNSET, order.getNextIndex(-1))
        assertEquals(C.INDEX_UNSET, order.getNextIndex(99))
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(-1))
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(99))

        val removedOutOfBounds = order.cloneAndRemove(-5, -2)
        assertEquals(3, removedOutOfBounds.length)

        val removedBeyondEnd = order.cloneAndRemove(10, 20)
        assertEquals(3, removedBeyondEnd.length)
    }
}
