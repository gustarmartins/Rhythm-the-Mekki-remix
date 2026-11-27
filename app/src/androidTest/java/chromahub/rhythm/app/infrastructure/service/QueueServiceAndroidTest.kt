/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.infrastructure.service.player.RhythmPlayerEngine
import chromahub.rhythm.app.shared.data.model.AppSettings
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real app/controller IPC and the production service in the isolated debug package. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class QueueServiceAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun <T> main(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }
    private fun await(description: String, predicate: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
        while (System.nanoTime() < deadline) {
            if (main(predicate)) return
            Thread.sleep(25)
        }
        fail(description)
    }
    private fun command(controller: MediaController, action: String, args: Bundle = Bundle.EMPTY): Bundle {
        val result = main { controller.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args) }
            .get(45, TimeUnit.SECONDS)
        assertEquals(action, SessionResult.RESULT_SUCCESS, result.resultCode)
        return result.extras
    }
    private fun snapshot(controller: MediaController) = command(controller, PlayNextCommand.GET_QUEUE,
        Bundle().apply { putInt(PlayNextCommand.LIMIT, 200) })

    @Test fun gaplessPreloadingTracksTheSettingWithoutPausingAutomaticOrManualTraversal() {
        require(context.packageName.endsWith(".debug"))
        val settings = AppSettings.getInstance(context)
        val previousGapless = settings.gaplessPlayback.value
        val previousSpatial = settings.systemSpatialAudio.value
        val previousCrossfade = settings.crossfade.value
        val wav = File.createTempFile("gapless-boundary", ".wav", context.cacheDir)
        val bytesPerSecond = 16_000 // mono 8 kHz, 16-bit PCM
        val size = bytesPerSecond * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(8_000).putInt(bytesPerSecond)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(size).array()
        wav.outputStream().use { it.write(header); it.write(ByteArray(size)) }
        val engine = main { RhythmPlayerEngine(context) }
        try {
            val player = main {
                settings.setCrossfade(false)
                settings.setGaplessPlayback(true)
                settings.setSystemSpatialAudio(true)
                engine.initialize()
                (engine.masterPlayer as ExoPlayer).apply { volume = 0f }
            }
            await("Gapless enables native playlist preload") {
                player.preloadConfiguration.targetPreloadDurationUs == 1_500_000L
            }
            main { settings.setCrossfade(true) }
            await("Crossfade's other deck owns preload") {
                player.preloadConfiguration.targetPreloadDurationUs == C.TIME_UNSET
            }
            for (gapless in listOf(false, true)) {
                main {
                    settings.setCrossfade(false)
                    settings.setGaplessPlayback(gapless)
                }
                await("Gapless preference reaches ExoPlayer") {
                    val expectedUs = if (gapless) 1_500_000L else C.TIME_UNSET
                    player.preloadConfiguration.targetPreloadDurationUs == expectedUs &&
                        !player.pauseAtEndOfMediaItems &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                            player.trackSelectionParameters.audioOffloadPreferences.isGaplessSupportRequired == gapless)
                }
                main {
                    val uri = android.net.Uri.fromFile(wav)
                    val queue = listOf("first", "second", "manual").map {
                        MediaItem.Builder().setMediaId(it).setUri(uri).build()
                    }
                    player.setMediaItems(queue)
                    player.prepare()
                    player.play()
                }
                await("First item auto-advances while playing (gapless=$gapless)") {
                    player.currentMediaItem?.mediaId == "second" && player.playWhenReady
                }
                main { player.seekToNextMediaItem() }
                await("Explicit next advances exactly once (gapless=$gapless)") {
                    player.currentMediaItem?.mediaId == "manual" && player.playWhenReady
                }
                val allowedNext = !gapless
                val previousBehavior = main { player.audioAttributes.spatializationBehavior }
                main { settings.setSystemSpatialAudio(allowedNext) }
                instrumentation.waitForIdleSync()
                main {
                    assertEquals(previousBehavior, player.audioAttributes.spatializationBehavior)
                    player.pause()
                    assertEquals(previousBehavior, player.audioAttributes.spatializationBehavior)
                    player.stop()
                }
                val expectedBehavior = if (allowedNext) C.SPATIALIZATION_BEHAVIOR_AUTO else C.SPATIALIZATION_BEHAVIOR_NEVER
                await("Spatial preference applies after stopping the prepared output") {
                    player.audioAttributes.spatializationBehavior == expectedBehavior
                }
            }
        } finally {
            main {
                engine.release()
                settings.setCrossfade(previousCrossfade)
                settings.setGaplessPlayback(previousGapless)
                settings.setSystemSpatialAudio(previousSpatial)
            }
            wav.delete()
        }
    }

    @Test fun playNextSurvivesBothShuffleEnginesThroughRealServiceAndController() {
        require(context.packageName.endsWith(".debug"))
        val settings = AppSettings.getInstance(context)
        val wav = File(context.cacheDir, "queue-regression.wav")
        val dataSize = 8000 * 2 * 60
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(dataSize).array()
        wav.outputStream().use { it.write(header); it.write(ByteArray(dataSize)) }
        main {
            settings.setContinueWithDeviceLibrary(false)
            settings.setCrossfade(false)
            settings.setBluetoothLyricsEnabled(false)
            settings.setQueuePersistenceEnabled(true)
        }
        val controller = main { MediaController.Builder(context,
            SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))).buildAsync() }
            .get(20, TimeUnit.SECONDS)
        try {
            fun item(id: String) = MediaItem.Builder().setMediaId(id).setUri(android.net.Uri.fromFile(wav)).build()
            for (native in listOf(false, true)) {
                main {
                    controller.pause()
                    controller.shuffleModeEnabled = false
                    settings.setShuffleUsesExoplayer(native)
                    val items = List(2077) { item(if (it == 1 || it == 7) "same-song" else "context-$it") }
                    controller.setMediaItems(items, 7, 17000L)
                }
                val before = snapshot(controller)
                val current = before.getStringArrayList(QueueEntryCommand.TOKENS)!![7]
                val albumIds = List(30) { "album-track-$it" }
                for (batch in albumIds.chunked(24)) {
                    command(controller, PlayNextCommand.ACTION, Bundle().apply {
                        putBoolean(PlayNextCommand.LIST_PLAYBACK, true)
                        putParcelableArrayList(PlayNextCommand.ITEMS, ArrayList(batch.map {
                            item(it).toBundleIncludeLocalConfiguration()
                        }))
                    })
                }
                command(controller, PlayNextCommand.ACTION, Bundle().apply {
                    putParcelableArrayList(PlayNextCommand.ITEMS, arrayListOf(
                        item("Jump").toBundleIncludeLocalConfiguration(),
                        item("Jump").toBundleIncludeLocalConfiguration(),
                        item("Brave").toBundleIncludeLocalConfiguration()))
                })
                await("Paused queue ready after enqueue") { controller.playbackState == Player.STATE_READY }
                assertFalse(main { controller.playWhenReady })
                assertEquals(17000L, main { controller.currentPosition })
                val requested = snapshot(controller).getStringArrayList(QueueEntryCommand.TOKENS)!!.subList(8, 11).toList()
                for (enabled in listOf(true, false, true)) {
                    main { controller.shuffleModeEnabled = enabled }
                    val actual = snapshot(controller)
                    assertEquals(2080 + albumIds.size, actual.getInt(PlayNextCommand.TOTAL))
                    assertEquals(7, actual.getInt(PlayNextCommand.CURRENT_INDEX))
                    assertEquals(current, actual.getStringArrayList(QueueEntryCommand.TOKENS)!![7])
                    assertEquals(requested, actual.getStringArrayList(QueueEntryCommand.TOKENS)!!.subList(8, 11))
                    assertEquals(listOf("Jump", "Jump", "Brave"), actual.getStringArrayList(PlayNextCommand.IDS)!!.subList(8, 11))
                    assertEquals(albumIds, actual.getStringArrayList(PlayNextCommand.IDS)!!.drop(11).take(albumIds.size))
                    assertEquals(17000L, main { controller.currentPosition })
                    assertFalse(main { controller.playWhenReady })
                }
                // Later requests append after the surviving FIFO segment, even after shuffle.
                command(controller, PlayNextCommand.ACTION, Bundle().apply {
                    putParcelableArrayList(PlayNextCommand.ITEMS, arrayListOf(item("Jet Rocket").toBundleIncludeLocalConfiguration()))
                })
                assertEquals(listOf("Jump", "Jump", "Brave", "Jet Rocket"),
                    snapshot(controller).getStringArrayList(PlayNextCommand.IDS)!!.subList(8, 12))
                command(controller, PlayNextCommand.PERSIST_QUEUE)
                val saved = settings.savedPlaybackQueueFor(settings.savedQueue.value)!!
                assertEquals(requested, saved.pending.take(3))
                assertEquals(current, saved.tokens[settings.savedQueueIndex.value])
                // Exercise the production restoration protocol as a fresh player queue,
                // then shuffle it again. Song IDs alone cannot retain these reservations.
                main {
                    controller.clearMediaItems()
                    controller.setMediaItems(restoredQueueItems(saved.ids.map(::item), saved), 7, 17000L)
                    controller.prepare()
                }
                await("Restored paused queue ready") { controller.playbackState == Player.STATE_READY }
                assertEquals(requested, snapshot(controller).getStringArrayList(QueueEntryCommand.TOKENS)!!.subList(8, 11))
                main { controller.shuffleModeEnabled = false }
                assertEquals(requested, snapshot(controller).getStringArrayList(QueueEntryCommand.TOKENS)!!.subList(8, 11))
                main { controller.shuffleModeEnabled = true }
                assertEquals(17000L, main { controller.currentPosition })
                main { controller.seekToNextMediaItem() }
                await("First requested occurrence plays next") {
                    controller.currentMediaItem?.mediaMetadata?.extras?.getString(PlayNextCommand.ENTRY_TOKEN) == requested[0]
                }
                main { controller.shuffleModeEnabled = false }
                val afterNext = snapshot(controller)
                assertEquals(requested.drop(1), afterNext.getStringArrayList(QueueEntryCommand.TOKENS)!!.subList(9, 11))
                assertFalse(main { controller.playWhenReady })
                command(controller, PlayNextCommand.CLEAR_EXCEPT_CURRENT)
            }
        } finally {
            main { controller.pause(); controller.clearMediaItems(); controller.release() }
            wav.delete()
        }
    }
}
