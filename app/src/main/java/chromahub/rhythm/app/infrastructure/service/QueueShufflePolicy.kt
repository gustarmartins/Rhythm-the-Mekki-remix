/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import kotlin.random.Random

/** Occurrence tokens, never song IDs: two copies of one song are independent requests. */
internal fun remainingPlayNext(tokens: List<String>, current: Int, pending: List<String>): List<String> {
    val consumed = pending.indexOf(tokens.getOrNull(current))
    val future = tokens.drop(current + 1).toSet()
    return (if (consumed >= 0) pending.drop(consumed + 1) else pending)
        .filter { it in future }.distinct()
}

internal data class QueueShufflePlan(val order: List<String>, val pending: List<String>)

/** Shuffle changes context only. Played occurrences and the FIFO manual segment stay put. */
internal fun planQueueShuffle(
    tokens: List<String>, current: Int, pending: List<String>, original: List<String>,
    enabled: Boolean, random: Random = Random.Default, freshSelection: Boolean = false
): QueueShufflePlan {
    require(tokens.size == tokens.toSet().size && tokens.none(String::isBlank))
    require(current in tokens.indices)
    val requests = if (freshSelection) emptyList() else remainingPlayNext(tokens, current, pending)
    val prefix = if (freshSelection) listOf(tokens[current]) else tokens.take(current + 1)
    val excluded = (prefix + requests).toSet()
    val context = tokens.filterNot { it in excluded }
    val ordered = if (enabled) context.shuffled(random) else {
        val available = context.toSet()
        val restored = original.filter { it in available }.distinct()
        val known = restored.toSet()
        restored + context.filterNot { it in known }
    }
    return QueueShufflePlan(prefix + requests + ordered, requests)
}

/** One coherent service-owned save, including the order to restore when shuffle is disabled. */
internal data class SavedPlaybackQueue(
    val ids: List<String>,
    val tokens: List<String>,
    val pending: List<String>,
    val original: List<String>,
    val shuffled: Boolean
) {
    fun matches(songIds: List<String>): Boolean = ids == songIds && tokens.size == ids.size &&
        tokens.none(String::isBlank) && tokens.toSet().size == tokens.size

    /** Keep occurrence identity even when a file disappeared between saving and restoration. */
    fun survivingTokens(availableIds: Set<String>): List<String> =
        ids.indices.filter { ids[it] in availableIds }.map { tokens[it] }
}
