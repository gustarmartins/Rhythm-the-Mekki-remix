/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Utility functions for queue operations, including shuffle algorithms.
 */
object QueueUtils {

    /**
     * Builds an anchored shuffle queue using the Fisher-Yates algorithm.
     *
     * The current song remains at its current position in the queue, while all other
     * songs are shuffled around it. This provides a more intuitive shuffle experience
     * where the currently playing song doesn't suddenly jump to a different position.
     *
     * @param originalQueue The original queue of songs
     * @param anchorIndex The index of the song that should remain in its current position
     * @return A new list with the anchored shuffle applied
     */
    fun buildAnchoredShuffleQueue(originalQueue: List<Song>, anchorIndex: Int): List<Song> {
        if (originalQueue.size <= 1) return originalQueue.toList()
        if (anchorIndex < 0 || anchorIndex >= originalQueue.size) {
            // Invalid anchor, fall back to regular shuffle
            return buildShuffleQueue(originalQueue)
        }

        val result = originalQueue.toMutableList()
        val anchorSong = result[anchorIndex]

        // Remove the anchor song temporarily
        result.removeAt(anchorIndex)

        // Shuffle the remaining songs using Fisher-Yates
        val random = Random(System.currentTimeMillis())
        for (i in result.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            // Swap elements
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        // Insert the anchor song back at its original position
        result.add(anchorIndex, anchorSong)

        return result
    }

    /**
     * Suspendable version of buildAnchoredShuffleQueue for large queues.
     * Runs the shuffle operation on a background thread to avoid blocking the UI.
     *
     * @param originalQueue The original queue of songs
     * @param anchorIndex The index of the song that should remain in its current position
     * @return A new list with the anchored shuffle applied
     */
    suspend fun buildAnchoredShuffleQueueSuspending(
        originalQueue: List<Song>,
        anchorIndex: Int
    ): List<Song> = withContext(Dispatchers.Default) {
        buildAnchoredShuffleQueue(originalQueue, anchorIndex)
    }

    /**
     * Builds a completely random shuffle queue using the Fisher-Yates algorithm.
     *
     * @param originalQueue The original queue of songs
     * @return A new list with all songs randomly shuffled
     */
    fun buildShuffleQueue(originalQueue: List<Song>): List<Song> {
        if (originalQueue.size <= 1) return originalQueue.toList()

        val result = originalQueue.toMutableList()
        val random = Random(System.currentTimeMillis())

        // Fisher-Yates shuffle algorithm
        for (i in result.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            // Swap elements
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result
    }

    /**
     * Prepares a shuffled queue by building it and ensuring the anchor song
     * is at the correct position.
     *
     * @param originalQueue The original queue of songs
     * @param currentSongId The ID of the currently playing song
     * @return A shuffled queue with the current song properly positioned
     */
    fun prepareShuffledQueue(originalQueue: List<Song>, currentSongId: String?): List<Song> {
        if (originalQueue.isEmpty()) return emptyList()

        val currentIndex = if (currentSongId != null) {
            originalQueue.indexOfFirst { it.id == currentSongId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        return buildAnchoredShuffleQueue(originalQueue, currentIndex)
    }
}
