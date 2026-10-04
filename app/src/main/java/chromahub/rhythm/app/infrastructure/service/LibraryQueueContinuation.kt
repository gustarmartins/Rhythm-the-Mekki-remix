/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.infrastructure.service

internal data class LibraryContinuationPlan(val songIds: List<String>, val startsNewCycle: Boolean)

/** Play unvisited library songs first; start another cycle only after the pool is exhausted. */
internal fun planLibraryContinuation(libraryIds: List<String>, queuedIds: List<String>, currentId: String?): LibraryContinuationPlan {
    val library = libraryIds.distinct()
    if (library.isEmpty()) return LibraryContinuationPlan(emptyList(), false)
    val queued = queuedIds.toHashSet()
    val fresh = library.filterNot { it in queued }
    if (fresh.isNotEmpty()) return LibraryContinuationPlan(fresh, false)
    val nextCycle = if (library.size > 1) library.filterNot { it == currentId } else library
    return LibraryContinuationPlan(nextCycle, true)
}
