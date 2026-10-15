/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.domain

internal data class StreamingQueueSelection<T>(val items: List<T>, val startIndex: Int)

/** Dropping unavailable rows must never change which occurrence the user selected. */
internal fun <S, T> resolveStreamingQueueSelection(
    requested: List<S>, startIndex: Int, resolve: (S) -> T?
): StreamingQueueSelection<T>? {
    if (requested.isEmpty()) return null
    val selected = startIndex.coerceIn(requested.indices)
    val resolved = requested.map(resolve)
    if (resolved[selected] == null) return null
    return StreamingQueueSelection(resolved.filterNotNull(), resolved.take(selected).count { it != null })
}
