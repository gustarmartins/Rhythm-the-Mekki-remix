/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import java.util.Random

/**
 * A custom [ShuffleOrder] implementation designed for music queue management.
 *
 * Unlike [ShuffleOrder.DefaultShuffleOrder] which randomly distributes inserted items
 * across the entire playlist, [RhythmShuffleOrder] respects playback queue semantics:
 * - Items inserted right after the current song (Play Next) are inserted immediately
 *   after the current song in the shuffled sequence.
 * - Items added to the end of the timeline (Add to Queue) are appended to the end
 *   of the shuffled sequence.
 * - Items removed from the timeline are cleanly removed while preserving the remaining order.
 */
@UnstableApi
class RhythmShuffleOrder : ShuffleOrder {

    private val random: Random
    private val shuffled: IntArray
    private val indexInShuffled: IntArray

    constructor(length: Int, random: Random = Random()) : this(
        shuffled = createShuffledList(length.coerceAtLeast(0), random),
        random = random
    )

    constructor(shuffled: IntArray, random: Random = Random()) : this(
        shuffled = shuffled.clone(),
        indexInShuffled = IntArray(shuffled.size).apply {
            for (i in shuffled.indices) {
                val element = shuffled[i]
                if (element in indices) {
                    this[element] = i
                }
            }
        },
        random = random
    )

    private constructor(
        shuffled: IntArray,
        indexInShuffled: IntArray,
        random: Random
    ) {
        this.random = random
        this.shuffled = shuffled
        this.indexInShuffled = indexInShuffled
    }

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int {
        if (index < 0 || index >= indexInShuffled.size) return C.INDEX_UNSET
        val shuffledIndex = indexInShuffled[index]
        if (shuffledIndex < 0 || shuffledIndex >= shuffled.size) return C.INDEX_UNSET
        val nextShuffledIndex = shuffledIndex + 1
        return if (nextShuffledIndex in shuffled.indices) shuffled[nextShuffledIndex] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        if (index < 0 || index >= indexInShuffled.size) return C.INDEX_UNSET
        val shuffledIndex = indexInShuffled[index]
        if (shuffledIndex < 0 || shuffledIndex >= shuffled.size) return C.INDEX_UNSET
        val prevShuffledIndex = shuffledIndex - 1
        return if (prevShuffledIndex in shuffled.indices) shuffled[prevShuffledIndex] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[shuffled.size - 1] else C.INDEX_UNSET
    }

    override fun getFirstIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[0] else C.INDEX_UNSET
    }

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        if (insertionCount <= 0) return this

        if (shuffled.isEmpty()) {
            return RhythmShuffleOrder(insertionCount, Random(random.nextLong()))
        }

        val oldLength = shuffled.size
        val clampedIndex = insertionIndex.coerceIn(0, oldLength)
        val newLength = oldLength + insertionCount
        val newShuffled = IntArray(newLength)

        val targetPosInShuffled = when {
            clampedIndex >= oldLength -> oldLength
            clampedIndex <= 0 -> 0
            else -> {
                val anchorTimelineIndex = clampedIndex - 1
                if (anchorTimelineIndex in indexInShuffled.indices) {
                    val anchorPos = indexInShuffled[anchorTimelineIndex]
                    if (anchorPos in shuffled.indices) anchorPos + 1 else oldLength
                } else {
                    oldLength
                }
            }
        }

        for (i in 0 until targetPosInShuffled) {
            val originalValue = shuffled[i]
            newShuffled[i] = if (originalValue >= clampedIndex) {
                originalValue + insertionCount
            } else {
                originalValue
            }
        }

        for (i in 0 until insertionCount) {
            newShuffled[targetPosInShuffled + i] = clampedIndex + i
        }

        for (i in targetPosInShuffled until oldLength) {
            val originalValue = shuffled[i]
            newShuffled[i + insertionCount] = if (originalValue >= clampedIndex) {
                originalValue + insertionCount
            } else {
                originalValue
            }
        }

        val newIndexInShuffled = IntArray(newLength)
        for (i in newShuffled.indices) {
            val element = newShuffled[i]
            if (element in newIndexInShuffled.indices) {
                newIndexInShuffled[element] = i
            }
        }

        return RhythmShuffleOrder(newShuffled, newIndexInShuffled, Random(random.nextLong()))
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val oldLength = shuffled.size
        if (oldLength == 0) return this

        val clampedFrom = indexFrom.coerceIn(0, oldLength)
        val clampedTo = indexToExclusive.coerceIn(clampedFrom, oldLength)
        val numberOfElementsToRemove = clampedTo - clampedFrom
        if (numberOfElementsToRemove <= 0) return this

        val newLength = oldLength - numberOfElementsToRemove
        if (newLength <= 0) {
            return RhythmShuffleOrder(0, Random(random.nextLong()))
        }

        val newShuffled = IntArray(newLength)
        var writeIndex = 0

        for (i in 0 until oldLength) {
            val element = shuffled[i]
            if (element in clampedFrom until clampedTo) {
                continue
            }
            if (writeIndex < newLength) {
                newShuffled[writeIndex] = if (element >= clampedTo) {
                    element - numberOfElementsToRemove
                } else {
                    element
                }
                writeIndex++
            }
        }

        val newIndexInShuffled = IntArray(newLength)
        for (i in newShuffled.indices) {
            val element = newShuffled[i]
            if (element in newIndexInShuffled.indices) {
                newIndexInShuffled[element] = i
            }
        }

        return RhythmShuffleOrder(newShuffled, newIndexInShuffled, Random(random.nextLong()))
    }

    override fun cloneAndClear(): ShuffleOrder {
        return RhythmShuffleOrder(0, Random(random.nextLong()))
    }

    companion object {
        private fun createShuffledList(length: Int, random: Random): IntArray {
            val shuffled = IntArray(length)
            for (i in 0 until length) {
                val swapIndex = random.nextInt(i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
            return shuffled
        }
    }
}
