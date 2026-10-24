/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.FlagSet
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class OwnedMetadataReplacementTest {
    private fun item(id: String, title: String = id) = MediaItem.Builder().setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()

    private class Queue(
        val items: List<MediaItem>,
        val ids: List<String> = items.indices.map { "window-$it" },
        val duration: Long = 10_000_000,
    ) : Timeline() {
        override fun getWindowCount() = items.size
        override fun getPeriodCount() = items.size
        override fun getIndexOfPeriod(uid: Any) = ids.indexOf(uid)
        override fun getUidOfPeriod(periodIndex: Int): Any = ids[periodIndex]
        override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period =
            error("Metadata comparison must not fetch media periods")
        override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
            window.uid = ids[windowIndex]
            window.mediaItem = items[windowIndex]
            window.durationUs = duration
            window.firstPeriodIndex = windowIndex
            window.lastPeriodIndex = windowIndex
            return window
        }
    }

    @Test fun largeQueueAcceptsOnlyTheOwnedMetadataReplacement() {
        val items = (0 until 2090).map { item("song-$it") }
        val changed = item("song-92", "display lyric")
        val updated = items.toMutableList().apply { set(92, changed) }
        val ids = items.indices.map { if (it == 92) "new-owned-window" else "window-$it" }
        assertTrue(OwnedMetadataReplacement(92, items[92], changed).matches(Queue(items), Queue(updated, ids)))
    }

    @Test fun queueEditsAndDuplicateOccurrenceReorderingStillNotifyControllers() {
        val duplicate = item("duplicate")
        val items = listOf(item("current"), duplicate, duplicate)
        val changed = item("current", "lyric")
        val replacement = OwnedMetadataReplacement(0, items[0], changed)
        assertFalse(replacement.matches(Queue(items), Queue(listOf(changed, duplicate))))
        assertFalse(replacement.matches(Queue(items), Queue(listOf(changed, duplicate, duplicate),
            listOf("new-owned-window", "window-2", "window-1"))))
        assertFalse(replacement.matches(Queue(items), Queue(listOf(changed, item("different"), duplicate))))
    }

    @Test fun unrelatedMetadataAndTimingChangesAreNotHidden() {
        val original = item("song")
        val changed = item("song", "lyric")
        val replacement = OwnedMetadataReplacement(0, original, changed)
        assertFalse(replacement.matches(Queue(listOf(original)), Queue(listOf(item("song", "other edit")))))
        assertFalse(replacement.matches(Queue(listOf(original)), Queue(listOf(changed), duration = 12_000_000)))
        assertFalse(OwnedMetadataReplacement(0, original, item("different", "lyric"))
            .matches(Queue(listOf(original)), Queue(listOf(item("different", "lyric")))))
        assertFalse(replacement.matches(Queue(listOf(changed)), Queue(listOf(changed))))
    }

    private val player = java.lang.reflect.Proxy.newProxyInstance(Player::class.java.classLoader,
        arrayOf(Player::class.java)) { _, _, _ -> null } as Player

    private fun events(vararg flags: Int): Player.Events = Player.Events(
        FlagSet.Builder().apply { flags.forEach { add(it) } }.build())

    @Test fun metadataAndOtherEventsSurviveWithoutAQueueNotification() {
        val original = item("song")
        val changed = item("song", "lyric")
        var timelineCalls = 0
        var metadata: MediaMetadata? = null
        var delivered: Player.Events? = null
        val sink = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) { timelineCalls++ }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { metadata = mediaMetadata }
            override fun onEvents(player: Player, events: Player.Events) { delivered = events }
        }
        val listener = OwnedMetadataListener(sink, Queue(listOf(original)),
            { OwnedMetadataReplacement(0, original, changed) }, { player })
        listener.onTimelineChanged(Queue(listOf(changed)), Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        listener.onMediaMetadataChanged(changed.mediaMetadata)
        listener.onEvents(player, events(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED))
        assertEquals(0, timelineCalls)
        assertEquals(changed.mediaMetadata, metadata)
        assertFalse(delivered!!.contains(Player.EVENT_TIMELINE_CHANGED))
        assertTrue(delivered!!.contains(Player.EVENT_MEDIA_METADATA_CHANGED))
        assertTrue(delivered!!.contains(Player.EVENT_PLAYBACK_STATE_CHANGED))
    }

    @Test fun realQueueEditInSameEventBatchIsNeverSuppressed() {
        val original = item("song")
        val changed = item("song", "lyric")
        var timelineCalls = 0
        var delivered: Player.Events? = null
        val sink = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) { timelineCalls++ }
            override fun onEvents(player: Player, events: Player.Events) { delivered = events }
        }
        val listener = OwnedMetadataListener(sink, Queue(listOf(original)),
            { OwnedMetadataReplacement(0, original, changed) }, { player })
        listener.onTimelineChanged(Queue(listOf(original, item("added"))), Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        listener.onTimelineChanged(Queue(listOf(changed, item("added"))), Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        listener.onEvents(player, events(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED))
        assertEquals(1, timelineCalls)
        assertTrue(delivered!!.contains(Player.EVENT_TIMELINE_CHANGED))
    }
}
