/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

import android.os.Bundle
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import chromahub.rhythm.app.infrastructure.service.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class QueueShufflePlaybackTest {
    private fun item(token: String, id: String = token) = MediaItem.Builder().setMediaId(id)
        .setUri("file:///test/$id.wav").setMediaMetadata(MediaMetadata.Builder().setTitle("lyric-$token")
            .setExtras(Bundle().apply { putString(PlayNextCommand.ENTRY_TOKEN, token) }).build()).build()
    private fun token(item: MediaItem) = item.mediaMetadata.extras!!.getString(PlayNextCommand.ENTRY_TOKEN)!!
    private fun traversal(player: Player): List<String> {
        val result = mutableListOf<String>()
        var index = player.currentTimeline.getFirstWindowIndex(player.shuffleModeEnabled)
        while (index != C.INDEX_UNSET) {
            check(result.size < player.mediaItemCount)
            result += token(player.getMediaItemAt(index))
            index = player.currentTimeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
        }
        return result
    }
    private fun apply(player: ExoPlayer, order: List<String>, enabled: Boolean, native: Boolean) =
        applyQueueShuffleOrder(player, order, enabled, native) { player.setShuffleOrder(RhythmShuffleOrder(it)) }

    @Test fun largeQueueShuffleAndUnshuffleRetainActualCurrentMediaSourceAndRequests() {
        for (native in listOf(false, true)) {
            val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
            try {
                val items = listOf(item("duplicate-old", "same-song"), item("current", "same-song"),
                    item("Jump-1", "Jump"), item("Jump-2", "Jump"), item("Brave")) +
                    List(2077) { item("context-$it") }
                val original = items.map(::token)
                player.setMediaItems(items, 1, 42_500L)
                val uid = player.currentTimeline.getWindow(1, Timeline.Window()).uid
                val current = player.currentMediaItem
                val pending = listOf("Jump-1", "Jump-2", "Brave")
                repeat(3) { seed ->
                    val order = traversal(player)
                    val plan = planQueueShuffle(order, order.indexOf("current"), pending, original, true, Random(seed))
                    apply(player, plan.order, true, native)
                    assertEquals(plan.order, traversal(player))
                    assertEquals("Jump-1", token(player.getMediaItemAt(player.nextMediaItemIndex)))
                    assertEquals(uid, player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window()).uid)
                    assertEquals(current, player.currentMediaItem)
                    assertEquals(42_500L, player.currentPosition)
                    assertFalse(player.playWhenReady)
                    val restored = planQueueShuffle(plan.order, 1, pending, original, false)
                    apply(player, restored.order, false, native)
                    assertEquals(original, traversal(player))
                    assertEquals(uid, player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window()).uid)
                    assertEquals(42_500L, player.currentPosition)
                }
                player.seekToNextMediaItem()
                assertEquals("Jump-1", token(player.currentMediaItem!!))
                player.seekToNextMediaItem()
                assertEquals("Jump-2", token(player.currentMediaItem!!))
                player.seekToNextMediaItem()
                assertEquals("Brave", token(player.currentMediaItem!!))
            } finally { player.release() }
        }
    }

    @Test fun nativeCurrentAtLastPhysicalIndexNormalizesWithoutSeekingAndKeepsRepeatAndPlayIntent() {
        for (repeat in listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL)) {
            for (playing in listOf(false, true)) {
                val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
                try {
                    player.setMediaItems(listOf(item("context"), item("pending"), item("history"), item("current")), 3, 9000L)
                    player.setShuffleOrder(RhythmShuffleOrder(intArrayOf(2, 3, 1, 0)))
                    player.shuffleModeEnabled = true
                    player.repeatMode = repeat
                    player.playWhenReady = playing
                    val uid = player.currentTimeline.getWindow(3, Timeline.Window()).uid
                    apply(player, listOf("history", "current", "pending", "context"), false, true)
                    assertEquals(1, player.currentMediaItemIndex)
                    assertEquals(uid, player.currentTimeline.getWindow(1, Timeline.Window()).uid)
                    assertEquals(9000L, player.currentPosition)
                    assertEquals(playing, player.playWhenReady)
                    assertEquals(repeat, player.repeatMode)
                    player.seekToNextMediaItem()
                    assertEquals("pending", token(player.currentMediaItem!!))
                } finally { player.release() }
            }
        }
    }

    @Test fun restorationBundlePreservesPendingDuplicateAfterMissingFileAndIpcRoundTrip() {
        val saved = SavedPlaybackQueue(listOf("same", "missing", "same", "tail"),
            listOf("first", "missing", "request", "tail"), listOf("request"), listOf("first", "tail", "request"), true)
        val items = restoredQueueItems(listOf(item("unused-1", "same"), item("unused-2", "same"), item("unused-3", "tail")), saved)
        @Suppress("DEPRECATION")
        val roundTrip = items.map { MediaItem.fromBundle(it.toBundleIncludeLocalConfiguration()) }
        assertEquals(listOf("first", "request", "tail"), roundTrip.map(::token))
        val extras = roundTrip.first().mediaMetadata.extras!!
        assertTrue(extras.getBoolean(PlayNextCommand.RESTORED_TRAVERSAL))
        assertTrue(extras.getBoolean(PlayNextCommand.REQUESTED_SHUFFLE))
        assertEquals(listOf("request"), extras.getStringArrayList(PlayNextCommand.RESTORED_PENDING))
        val plan = planQueueShuffle(roundTrip.map(::token), 0,
            extras.getStringArrayList(PlayNextCommand.RESTORED_PENDING)!!,
            extras.getStringArrayList(PlayNextCommand.RESTORED_ORIGINAL)!!, true)
        assertEquals(listOf("first", "request", "tail"), plan.order)
        val withoutMode = restoredQueueItems(roundTrip, saved, restoreShuffleMode = false).first().mediaMetadata.extras!!
        assertFalse(withoutMode.getBoolean(PlayNextCommand.REQUESTED_SHUFFLE))
        assertEquals(listOf("request"), withoutMode.getStringArrayList(PlayNextCommand.RESTORED_PENDING))
    }
}
