/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import android.os.Bundle
import androidx.media3.common.MediaItem

/** Carry restoration state in the same command as the playlist, across controller IPC. */
internal fun restoredQueueItems(
    items: List<MediaItem>, saved: SavedPlaybackQueue?, restoreShuffleMode: Boolean = true
): List<MediaItem> {
    val occurrences = saved?.ids?.zip(saved.tokens)?.groupBy({ it.first }, { it.second })
        ?.mapValues { (_, tokens) -> ArrayDeque(tokens) }
    return items.mapIndexed { index, item ->
        val extras = Bundle(item.mediaMetadata.extras ?: Bundle.EMPTY).apply {
            occurrences?.get(item.mediaId)?.removeFirstOrNull()?.let { putString(PlayNextCommand.ENTRY_TOKEN, it) }
            if (index == 0) {
                putBoolean(PlayNextCommand.RESTORED_TRAVERSAL, true)
                saved?.let {
                    putBoolean(PlayNextCommand.REQUESTED_SHUFFLE, restoreShuffleMode && it.shuffled)
                    putStringArrayList(PlayNextCommand.RESTORED_PENDING, ArrayList(it.pending))
                    putStringArrayList(PlayNextCommand.RESTORED_ORIGINAL, ArrayList(it.original))
                }
                if (!restoreShuffleMode) putBoolean(PlayNextCommand.REQUESTED_SHUFFLE, false)
            }
        }
        item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
    }
}
