package chromahub.rhythm.app.infrastructure.service

/** Private app/service protocol for ordered additions, independent of the AVRCP view. */
object PlayNextCommand {
    const val ACTION = "chromahub.rhythm.app.ENQUEUE_PLAY_NEXT"
    const val GET_QUEUE = "chromahub.rhythm.app.GET_PLAYBACK_QUEUE"
    const val OFFSET = "queue_offset"
    const val LIMIT = "queue_limit"
    const val TOTAL = "queue_total"
    const val REVISION = "queue_revision"
    const val IDS = "queue_ids"
    const val SOURCE = "queue_source"
    const val ITEMS = "items"
    const val ENTRY_TOKEN = "rhythm_play_next_entry"
    const val INSERTION_INDEX = "insertion_index"
    const val VIRTUAL_QUEUE = "virtual_queue"
    const val CURRENT_INDEX = "current_index"
}

internal data class PlayNextInsertion(
    val insertionIndex: Int,
    val displayInsertionIndex: Int,
    val displayCurrentIndex: Int,
    val playbackOrder: List<Int>,
    val remainingPending: List<String>
)

/**
 * Append a request after unplayed manual requests, before the context queue. Tokens identify
 * occurrences, including duplicate songs. Advancing past, deleting or replacing an entry
 * removes it from the pending segment; playing previous never resurrects consumed requests.
 */
internal fun planPlayNextInsertion(
    entryTokens: List<String?>,
    currentIndex: Int,
    playbackOrder: List<Int>,
    pendingTokens: List<String>,
    addedCount: Int
): PlayNextInsertion {
    require(addedCount > 0)
    require(playbackOrder.sorted() == entryTokens.indices.toList())
    require(currentIndex == -1 || currentIndex in entryTokens.indices)
    val currentPosition = playbackOrder.indexOf(currentIndex)
    val consumed = pendingTokens.indexOf(entryTokens.getOrNull(currentIndex))
    val candidates = if (consumed >= 0) pendingTokens.drop(consumed + 1) else pendingTokens
    val future = playbackOrder.drop(currentPosition + 1).mapNotNull { entryTokens[it] }.toSet()
    val pending = candidates.filter { it in future }
    val byToken = entryTokens.withIndex().filter { it.value != null }.associate { it.value to it.index }
    val pendingIndices = pending.mapNotNull { byToken[it] }
    val insertion = (pendingIndices.lastOrNull()?.plus(1) ?: (currentIndex + 1))
        .coerceIn(0, entryTokens.size)
    fun remap(index: Int) = if (index >= insertion) index + addedCount else index
    val pendingIndexSet = pendingIndices.toSet()
    val context = playbackOrder.filterNot { it in pendingIndexSet }
    val split = context.indexOf(currentIndex) + 1
    val order = context.take(split).map(::remap) + pendingIndices.map(::remap) +
        (insertion until insertion + addedCount) + context.drop(split).map(::remap)
    return PlayNextInsertion(
        insertion, split + pending.size, split - 1, order, pending
    )
}
