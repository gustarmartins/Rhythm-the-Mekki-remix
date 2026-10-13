/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.util

/** One pass: stable keys for duplicate occurrences without rescanning every preceding row. */
internal fun queueOccurrenceKeys(ids: List<String>): List<String> {
    val counts = HashMap<String, Int>()
    return ids.map { id ->
        val occurrence = (counts[id] ?: 0) + 1
        counts[id] = occurrence
        "$id@$occurrence"
    }
}

/** Remove the suffix first so the original current index remains valid for prefix removal. */
internal fun clearQueueRemovalRanges(size: Int, currentIndex: Int): List<IntRange> = buildList {
    if (currentIndex !in 0 until size) return@buildList
    if (currentIndex + 1 < size) add(currentIndex + 1 until size)
    if (currentIndex > 0) add(0 until currentIndex)
}

/** Restore future order by occurrence, retaining additions and duplicate tracks. */
internal fun <T> restoreQueueOccurrences(current: List<T>, currentIndex: Int, original: List<T>, id: (T) -> String): List<T> {
    if (current.isEmpty()) return emptyList()
    val index = currentIndex.coerceIn(current.indices)
    val prefix = current.take(index + 1)
    val upcoming = current.drop(index + 1)
    val byId = upcoming.indices.groupBy { id(upcoming[it]) }
        .mapValues { (_, indices) -> java.util.ArrayDeque(indices) }
    val remaining = BooleanArray(upcoming.size) { true }
    val restored = mutableListOf<T>()
    for (entry in original) {
        val position = byId[id(entry)]?.pollFirst() ?: continue
        remaining[position] = false
        restored.add(upcoming[position])
    }
    upcoming.forEachIndexed { i, entry -> if (remaining[i]) restored.add(entry) }
    return prefix + restored
}
