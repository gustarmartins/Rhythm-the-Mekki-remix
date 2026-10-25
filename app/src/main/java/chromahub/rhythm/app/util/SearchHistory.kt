/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

/** Normalize only surrounding whitespace; keep the owner's spelling for display. */
internal fun updatedSearchHistory(history: List<String>, query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return history
    return (listOf(trimmed) + history.map(String::trim))
        .filter(String::isNotEmpty).distinctBy { it.lowercase(java.util.Locale.ROOT) }.take(10)
}
