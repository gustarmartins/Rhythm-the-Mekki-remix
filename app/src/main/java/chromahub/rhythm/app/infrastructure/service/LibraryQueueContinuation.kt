/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.infrastructure.service

import kotlin.random.Random

internal data class LibraryContinuationPlan(val songIds: List<String>, val startsNewCycle: Boolean)

/**
 * Draw from the whole library, without replacement. [queuedIds] is the selected context or
 * pending queue, not retained playback history. Only actually visited songs enter [playedIds].
 * Context builders permit a new cycle only after all available songs were actually visited;
 * merely including the remaining songs in a selected list does not exhaust that cycle.
 */
internal fun planLibraryContinuation(
    libraryIds: List<String>,
    queuedIds: List<String>,
    currentId: String?,
    playedIds: Set<String> = emptySet(),
    preferredIds: Set<String> = emptySet(),
    allowNewCycle: Boolean = true,
    random: Random = Random.Default
): LibraryContinuationPlan {
    val library = libraryIds.filter { it.isNotBlank() }.distinct()
    if (library.isEmpty()) return LibraryContinuationPlan(emptyList(), false)
    val excluded = queuedIds.toHashSet().apply { currentId?.let(::add) }
    val available = library.filterNot { it in excluded }
    val fresh = available.filterNot { it in playedIds }
    if (fresh.isNotEmpty()) {
        return LibraryContinuationPlan(mixLibrarySongs(fresh, preferredIds, random), false)
    }
    if (!allowNewCycle) return LibraryContinuationPlan(emptyList(), false)
    val nextCycle = if (library.size == 1 && library.single() == currentId) library else available
    return LibraryContinuationPlan(mixLibrarySongs(nextCycle, preferredIds, random), nextCycle.isNotEmpty())
}

/** A related song every four slots; the rest explores the library. Every candidate survives. */
private fun mixLibrarySongs(ids: List<String>, preferredIds: Set<String>, random: Random): List<String> {
    val shuffled = ids.shuffled(random)
    if (preferredIds.isEmpty()) return shuffled
    val related = ArrayDeque(shuffled.filter { it in preferredIds })
    val other = ArrayDeque(shuffled.filterNot { it in preferredIds })
    return buildList(ids.size) {
        while (related.isNotEmpty() || other.isNotEmpty()) {
            add(if (related.isNotEmpty() && (size % 4 == 0 || other.isEmpty())) related.removeFirst()
                else other.removeFirst())
        }
    }
}
