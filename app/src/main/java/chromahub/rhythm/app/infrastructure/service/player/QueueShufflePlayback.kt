/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import chromahub.rhythm.app.infrastructure.service.PlayNextCommand

/** Apply a service-planned occurrence order without replacing, seeking or restarting current audio. */
@UnstableApi
internal fun applyQueueShuffleOrder(
    player: Player, order: List<String>, enabled: Boolean, nativeShuffle: Boolean,
    setShuffleOrder: (IntArray) -> Unit
) {
    val items = List(player.mediaItemCount) { player.getMediaItemAt(it) }
    val tokens = items.map { it.mediaMetadata.extras?.getString(PlayNextCommand.ENTRY_TOKEN).orEmpty() }
    require(order.size == tokens.size && order.toSet() == tokens.toSet() && tokens.distinct().size == tokens.size)
    val current = player.currentMediaItemIndex
    require(current in items.indices)
    if (enabled && nativeShuffle) {
        val positions = tokens.withIndex().associate { it.value to it.index }
        setShuffleOrder(order.map { positions.getValue(it) }.toIntArray())
    } else {
        val destination = order.indexOf(tokens[current])
        val byToken = tokens.zip(items).toMap()
        // At most two local playlist edits, even for thousands of songs. The current
        // MediaSource is retained, including its buffer, position, lyrics and play intent.
        if (tokens.drop(current + 1) != order.drop(destination + 1)) {
            player.replaceMediaItems(current + 1, items.size, order.drop(destination + 1).map(byToken::getValue))
        }
        if (tokens.take(current) != order.take(destination)) {
            player.replaceMediaItems(0, current, order.take(destination).map(byToken::getValue))
        }
        // Manual shuffle uses physical order; expose the same enabled state to media
        // controllers without a second, hidden shuffle over that physical queue.
        setShuffleOrder(IntArray(order.size) { it })
    }
    player.shuffleModeEnabled = enabled
}
