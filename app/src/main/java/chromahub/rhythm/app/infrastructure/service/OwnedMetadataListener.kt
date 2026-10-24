/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.FlagSet
import androidx.media3.common.util.UnstableApi

/** Keeps normal metadata notifications while excluding an owned display edit from queue events. */
@UnstableApi
internal class OwnedMetadataListener(
    private val listener: Player.Listener,
    initialTimeline: Timeline,
    private val replacement: () -> OwnedMetadataReplacement?,
    private val player: () -> Player,
) : Player.Listener by listener {
    private var previousTimeline = initialTimeline
    private var suppressedTimeline = false
    private var forwardedTimeline = false

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        val displayUpdate = reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED &&
            replacement()?.matches(previousTimeline, timeline) == true
        previousTimeline = timeline
        if (displayUpdate) suppressedTimeline = true
        else {
            forwardedTimeline = true
            listener.onTimelineChanged(timeline, reason)
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val omitTimeline = suppressedTimeline && !forwardedTimeline
        suppressedTimeline = false
        forwardedTimeline = false
        if (!omitTimeline || !events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            listener.onEvents(this.player(), events)
            return
        }
        val flags = FlagSet.Builder()
        for (i in 0 until events.size()) {
            val event = events.get(i)
            if (event != Player.EVENT_TIMELINE_CHANGED) flags.add(event)
        }
        val remaining = flags.build()
        if (remaining.size() != 0) listener.onEvents(this.player(), Player.Events(remaining))
    }
}
