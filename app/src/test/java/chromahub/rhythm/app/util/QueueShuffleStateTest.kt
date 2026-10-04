/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueShuffleStateTest {

    private data class TestTrack(val id: String, val title: String)

    @Test
    fun testAnchoredShuffleKeepsAnchorSongAtPosition() {
        val list = (1..10).map { TestTrack(it.toString(), "Track $it") }
        val anchorIndex = 3
        val anchorItem = list[anchorIndex]

        val result = list.toMutableList()
        result.removeAt(anchorIndex)
        result.shuffle()
        result.add(anchorIndex, anchorItem)

        assertEquals(10, result.size)
        assertEquals(anchorItem, result[anchorIndex])
        assertEquals(list.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun testRestoreQueueOrderPreservesPlayedAndRestoresUpcoming() {
        val originalOrder = listOf(
            TestTrack("1", "Track 1"),
            TestTrack("2", "Track 2"),
            TestTrack("3", "Track 3"),
            TestTrack("4", "Track 4"),
            TestTrack("5", "Track 5"),
            TestTrack("6", "Track 6")
        )

        // Shuffled queue where user started at track 1, then skipped to track 4, then to track 2
        // Queue currently at index 2 (Track 2)
        val currentQueue = listOf(
            TestTrack("1", "Track 1"), // played (index 0)
            TestTrack("4", "Track 4"), // played (index 1)
            TestTrack("2", "Track 2"), // CURRENT (index 2)
            TestTrack("6", "Track 6"), // upcoming
            TestTrack("3", "Track 3"), // upcoming
            TestTrack("5", "Track 5")  // upcoming
        )
        val currentIndex = 2

        val restored = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = currentQueue,
            currentIndex = currentIndex,
            originalOrder = originalOrder
        ) { it.id }

        assertEquals(6, restored.size)
        // Played segment unchanged
        assertEquals("1", restored[0].id)
        assertEquals("4", restored[1].id)
        // Current track unchanged
        assertEquals("2", restored[2].id)
        // Upcoming segment restored according to original order (3, 5, 6)
        assertEquals("3", restored[3].id)
        assertEquals("5", restored[4].id)
        assertEquals("6", restored[5].id)
    }

    @Test
    fun testRestoreQueueOrderPreservesNewlyAddedSongs() {
        val originalOrder = listOf(
            TestTrack("1", "Track 1"),
            TestTrack("2", "Track 2"),
            TestTrack("3", "Track 3")
        )

        // Song "99" was added to queue while shuffle was active
        val currentQueue = listOf(
            TestTrack("2", "Track 2"), // current (index 0)
            TestTrack("99", "Added Track"),
            TestTrack("3", "Track 3"),
            TestTrack("1", "Track 1")
        )

        val restored = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = currentQueue,
            currentIndex = 0,
            originalOrder = originalOrder
        ) { it.id }

        assertEquals(4, restored.size)
        assertEquals("2", restored[0].id)
        // Original order has 1, 3 in relative order, and newly added 99 placed after
        assertEquals("1", restored[1].id)
        assertEquals("3", restored[2].id)
        assertEquals("99", restored[3].id)
    }

    @Test
    fun testRestoreQueueOrderBoundaryConditions() {
        // Empty queue
        val emptyRestored = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = emptyList<TestTrack>(),
            currentIndex = 0,
            originalOrder = emptyList()
        ) { it.id }
        assertTrue(emptyRestored.isEmpty())

        // Single element queue
        val single = listOf(TestTrack("1", "Solo"))
        val singleRestored = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = single,
            currentIndex = 0,
            originalOrder = single
        ) { it.id }
        assertEquals(single, singleRestored)

        // Current index at end of queue
        val original = listOf(TestTrack("1", "T1"), TestTrack("2", "T2"), TestTrack("3", "T3"))
        val atEnd = listOf(TestTrack("2", "T2"), TestTrack("3", "T3"), TestTrack("1", "T1"))
        val atEndRestored = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = atEnd,
            currentIndex = 2,
            originalOrder = original
        ) { it.id }
        assertEquals(atEnd, atEndRestored)
    }

    @Test
    fun testRepeatedShuffleTogglesPreservePlayedProgression() {
        val originalOrder = (1..8).map { TestTrack(it.toString(), "Track $it") }

        // Initial play of track 1
        var queue = originalOrder
        var currentIndex = 0

        // Step 1: User enables shuffle (simulate anchored shuffle where track 1 stays at 0)
        val shuffledTracks = (2..8).map { TestTrack(it.toString(), "Track $it") }.shuffled()
        queue = listOf(queue[0]) + shuffledTracks

        // Step 2: User plays next 2 tracks: index moves 0 -> 1 -> 2
        currentIndex = 2
        val playedIds = queue.subList(0, currentIndex).map { it.id }
        val currentId = queue[currentIndex].id

        // Step 3: User disables shuffle
        queue = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = queue,
            currentIndex = currentIndex,
            originalOrder = originalOrder
        ) { it.id }

        // Verify played tracks remain before currentIndex
        assertEquals(playedIds, queue.subList(0, currentIndex).map { it.id })
        assertEquals(currentId, queue[currentIndex].id)

        // Step 4: User advances 1 more track
        currentIndex = 3
        val updatedPlayedIds = queue.subList(0, currentIndex).map { it.id }
        val updatedCurrentId = queue[currentIndex].id

        // Step 5: User disables shuffle again (or toggles)
        queue = QueueUtils.restoreQueueOrderOnShuffleDisable(
            currentSongs = queue,
            currentIndex = currentIndex,
            originalOrder = originalOrder
        ) { it.id }

        assertEquals(updatedPlayedIds, queue.subList(0, currentIndex).map { it.id })
        assertEquals(updatedCurrentId, queue[currentIndex].id)
        assertEquals(8, queue.size)
    }
}
