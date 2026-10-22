/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

internal data class LegacyPrevious<T>(val history: List<T>, val current: T, val upcoming: List<T>)

/** Repeat All wraps to the last occurrence without inserting the old current twice. */
internal fun <T> planLegacyPrevious(history: List<T>, current: T, upcoming: List<T>, repeatAll: Boolean): LegacyPrevious<T>? {
    val wrap = history.isEmpty() && repeatAll
    val candidates = if (wrap) upcoming else history
    if (candidates.isEmpty()) return null
    return LegacyPrevious(candidates.dropLast(1), candidates.last(), listOf(current) + if (wrap) emptyList() else upcoming)
}
