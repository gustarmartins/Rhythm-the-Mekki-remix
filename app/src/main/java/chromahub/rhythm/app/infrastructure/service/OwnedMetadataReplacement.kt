/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi

/** One service-owned display update, not an external playlist edit. */
@UnstableApi
internal data class OwnedMetadataReplacement(
    val index: Int,
    val before: MediaItem,
    val after: MediaItem,
) {
    fun matches(previous: Timeline, current: Timeline): Boolean {
        if (previous.windowCount != current.windowCount || previous.periodCount != current.periodCount ||
            index !in 0 until current.windowCount || before == after ||
            before.buildUpon().setMediaMetadata(after.mediaMetadata).build() != after ||
            before.mediaMetadata.extras?.getString(PlayNextCommand.ENTRY_TOKEN) !=
                after.mediaMetadata.extras?.getString(PlayNextCommand.ENTRY_TOKEN) ||
            previous.getFirstWindowIndex(true) != current.getFirstWindowIndex(true)) return false

        val oldWindow = Timeline.Window()
        val newWindow = Timeline.Window()
        for (i in 0 until current.windowCount) {
            previous.getWindow(i, oldWindow)
            current.getWindow(i, newWindow)
            if (i == index) {
                if (oldWindow.mediaItem != before || newWindow.mediaItem != after) return false
                // ExoPlayer may replace this window UID for a same-source metadata
                // update. Normalize only this explicitly owned replacement.
                newWindow.uid = oldWindow.uid
                newWindow.mediaItem = oldWindow.mediaItem
            }
            if (oldWindow != newWindow || previous.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true) !=
                current.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true)) return false
        }
        return true
    }
}
