/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

/** App-only edits address occurrences in playback order, never the AVRCP timeline. */
object QueueEntryCommand {
    const val ACTION = "chromahub.rhythm.app.EDIT_QUEUE_ENTRY"
    const val OPERATION = "operation"
    const val TARGET = "target_entry"
    const val TOKENS = "queue_entry_tokens"
    const val SELECT = "select"
    const val PLAY = "play"
    const val NEXT = "next"
    const val REMOVE = "remove"
    const val MOVE = "move"
}

internal data class QueueEntryEdit(
    val order: List<String>,
    val currentIndex: Int,
    val pending: List<String>
)

/** Pure policy shared by local/legacy playback and server-owned queues. */
internal fun planQueueEntryEdit(
    tokens: List<String>,
    currentIndex: Int,
    pending: List<String>,
    selected: String,
    operation: String,
    target: String? = null
): QueueEntryEdit {
    require(tokens.all { it.isNotBlank() } && tokens.toSet().size == tokens.size)
    require(currentIndex in tokens.indices) { "Queue has no current occurrence" }
    val from = tokens.indexOf(selected)
    require(from >= 0) { "Queue occurrence is no longer available" }
    val current = tokens[currentIndex]
    val future = tokens.drop(currentIndex + 1).toSet()
    val consumed = pending.indexOf(current)
    val remaining = (if (consumed >= 0) pending.drop(consumed + 1) else pending)
        .filter { it in future }.distinct()
    var nextPending = remaining
    val edited = tokens.toMutableList()
    when (operation) {
        QueueEntryCommand.PLAY -> {
            val after = tokens.drop(from + 1).toSet()
            return QueueEntryEdit(tokens, from, remaining.filter { it in after })
        }
        QueueEntryCommand.NEXT -> {
            // Repeated taps never duplicate or demote an already reserved request.
            if (selected == current || selected in remaining) return QueueEntryEdit(tokens, currentIndex, remaining)
            val context = tokens.filterNot { it == selected || it in remaining }
            val split = context.indexOf(current) + 1
            edited.clear()
            edited.addAll(context.take(split) + remaining + selected + context.drop(split))
            nextPending = remaining + selected
        }
        QueueEntryCommand.REMOVE -> {
            require(selected != current) { "Cannot remove the currently playing occurrence" }
            edited.removeAt(from)
            nextPending = remaining.filterNot { it == selected }
        }
        QueueEntryCommand.MOVE -> {
            val to = tokens.indexOf(target)
            require(from > currentIndex && to > currentIndex) { "Only upcoming occurrences can be reordered" }
            edited.add(to, edited.removeAt(from))
            // Explicit dragging overrides FIFO order; only its contiguous manual prefix remains reserved.
            nextPending = edited.drop(edited.indexOf(current) + 1).takeWhile { it in remaining }
        }
        else -> error("Unknown queue operation")
    }
    return QueueEntryEdit(edited, edited.indexOf(current), nextPending)
}
