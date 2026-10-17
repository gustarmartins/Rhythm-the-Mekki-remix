/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation

import android.app.ActivityOptions
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import chromahub.rhythm.app.shared.presentation.components.player.playerEntranceMotion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.infrastructure.service.player.RhythmPlayerEngine
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Runs in the isolated debug package; never changes the owner's release preferences or queue. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PlaybackRegressionAndroidTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var host: ComponentActivity? = null
    private var restoreSettings: () -> Unit = {}

    private fun <T> main(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    @Before fun startHost() {
        require(context.packageName.endsWith(".debug"))
        val display = InstrumentationRegistry.getArguments().getString("displayId")?.toInt()
        val intent = Intent(context, ComponentActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        host = if (display == null) instrumentation.startActivitySync(intent) as ComponentActivity
        else instrumentation.startActivitySync(intent, ActivityOptions.makeBasic()
            .setLaunchDisplayId(display).toBundle()) as ComponentActivity
        main {
            if (display != null) assertEquals(display, host!!.window.decorView.display.displayId)
            val settings = AppSettings.getInstance(context)
            val rate = settings.playbackRate.value
            restoreSettings = { settings.setPlaybackRate(rate.speed, rate.pitch, rate.synced) }
        }
    }

    @After fun closeHost() {
        main { restoreSettings(); host?.finishAndRemoveTask() }
    }

    private fun showRateSheet(showPitch: Boolean = true, save: (Float, Float, Boolean) -> Unit) {
        main {
            host!!.setContent {
                MaterialTheme {
                    var visible by remember { mutableStateOf(true) }
                    if (visible) PlaybackSpeedAndPitchBottomSheet(
                        currentSpeed = 1.25f, currentPitch = 0.8f,
                        showPitchControls = showPitch,
                        onSave = save, onDismiss = { visible = false }
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun syncToggleAndPresetApplyOneLinkedPair() {
        var saved: chromahub.rhythm.app.shared.data.model.PlaybackRate? = null
        showRateSheet { speed, pitch, synced ->
            saved = chromahub.rhythm.app.shared.data.model.PlaybackRate(speed, pitch, synced)
        }
        compose.onNodeWithText(context.getString(chromahub.rhythm.app.R.string.player_sync_speed_pitch)).performClick()
        compose.onAllNodesWithText("0.5x")[0].performScrollTo().performClick()
        assertNull("Draft edits must wait for Apply", saved)
        compose.onNodeWithText(context.getString(chromahub.rhythm.app.R.string.ui_apply)).performClick()
        assertEquals(chromahub.rhythm.app.shared.data.model.PlaybackRate(0.5f, 0.5f, true), saved)
    }

    @Test fun unlinkedPitchPresetDoesNotChangeTempo() {
        var saved: chromahub.rhythm.app.shared.data.model.PlaybackRate? = null
        showRateSheet { speed, pitch, synced ->
            saved = chromahub.rhythm.app.shared.data.model.PlaybackRate(speed, pitch, synced)
        }
        compose.onAllNodesWithText("0.5x")[1].performScrollTo().performClick()
        compose.onNodeWithText(context.getString(chromahub.rhythm.app.R.string.ui_apply)).performClick()
        assertEquals(chromahub.rhythm.app.shared.data.model.PlaybackRate(1.25f, 0.5f, false), saved)
    }

    @Test fun defaultSpeedEditorDoesNotOfferUnsavedPitchOrSyncControls() {
        showRateSheet(showPitch = false) { _, _, _ -> }
        compose.onNodeWithText(context.getString(chromahub.rhythm.app.R.string.player_sync_speed_pitch)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(chromahub.rhythm.app.R.string.player_pitch_label)).assertDoesNotExist()
    }

    @Test fun serviceEngineRestoresAndObservesBothRatesWithoutAViewModel() {
        val settings = main { AppSettings.getInstance(context) }
        var engine: RhythmPlayerEngine? = null
        try {
            main {
                settings.setPlaybackRate(1.3f, 0.8f, false)
                engine = RhythmPlayerEngine(context).also { it.initialize() }
                assertEquals(1.3f, engine!!.masterPlayer.playbackParameters.speed)
                assertEquals(0.8f, engine!!.masterPlayer.playbackParameters.pitch)
                settings.setSyncSpeedAndPitch(true)
            }
            compose.waitUntil(5000) { main { engine!!.masterPlayer.playbackParameters.pitch == 1.3f } }
            main { settings.setPlaybackPitch(0.75f) }
            compose.waitUntil(5000) { main { engine!!.masterPlayer.playbackParameters.speed == 0.75f } }
            // Match the effects worker's access: this test thread is deliberately not Main.
            val sessionId = main { (engine!!.masterPlayer as androidx.media3.exoplayer.ExoPlayer).audioSessionId }
            assertEquals(sessionId, engine!!.getAudioSessionId())
            main {
                engine!!.release()
                engine = RhythmPlayerEngine(context).also { it.initialize() }
                assertEquals(0.75f, engine!!.masterPlayer.playbackParameters.speed)
                assertEquals(0.75f, engine!!.masterPlayer.playbackParameters.pitch)
            }
        } finally { main { engine?.release() } }
        assertEquals(0, engine!!.getAudioSessionId())
    }

    @Test fun reopeningUiPreservesPlayingRateAndSpeakerSessionLyrics() {
        val settings = main { AppSettings.getInstance(context) }
        val savedDefault = settings.useDefaultPlaybackSpeed.value
        val savedDefaultSpeed = settings.defaultPlaybackSpeed.value
        val savedLyrics = settings.bluetoothLyricsEnabled.value
        val savedQueue = settings.queuePersistenceEnabled.value
        val savedClose = settings.stopPlaybackOnAppClose.value
        val file = java.io.File(context.cacheDir, "playback-rate-regression.wav")
        val bytes = java.nio.ByteBuffer.allocate(44 + 48000 * 30 * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(bytes.capacity() - 8).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(48000).putInt(96000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(bytes.capacity() - 44)
        file.writeBytes(bytes.array())
        val store = androidx.lifecycle.ViewModelStore()
        var controller: androidx.media3.session.MediaController? = null
        try {
            main {
                settings.setUseDefaultPlaybackSpeed(false)
                settings.setQueuePersistenceEnabled(false)
                settings.setStopPlaybackOnAppClose(false)
                settings.setBluetoothLyricsEnabled(true)
                settings.setPlaybackRate(1.3f, 0.8f, false)
            }
            controller = main {
                androidx.media3.session.MediaController.Builder(context,
                    androidx.media3.session.SessionToken(context, android.content.ComponentName(context,
                        chromahub.rhythm.app.infrastructure.service.MediaPlaybackService::class.java)))
                    .buildAsync()
            }.get(15, java.util.concurrent.TimeUnit.SECONDS)
            val activeController = controller!!
            main {
                activeController.volume = 0f
                activeController.setMediaItem(androidx.media3.common.MediaItem.Builder()
                    .setMediaId("rate-regression").setUri(android.net.Uri.fromFile(file))
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Original title").setArtist("Regression fixture").build()).build())
                activeController.prepare()
                activeController.play()
            }
            compose.waitUntil(10000) { main { activeController.isPlaying } }
            main {
                settings.setDefaultPlaybackSpeed(1f)
                settings.setUseDefaultPlaybackSpeed(true)
            }
            repeat(2) {
                val vm = main {
                    chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel(
                        context.applicationContext as android.app.Application).also { store.put("player", it) }
                }
                compose.waitUntil(10000) { vm.serviceConnected.value }
                main {
                    assertEquals(1.3f, activeController.playbackParameters.speed)
                    assertEquals(0.8f, activeController.playbackParameters.pitch)
                    assertTrue(activeController.isPlaying)
                    store.clear()
                }
            }
            val lyrics = android.os.Bundle().apply {
                putString("song_id", "rate-regression")
                putStringArrayList("lyric_texts", arrayListOf("Speaker lyric"))
                putLongArray("lyric_timestamps", longArrayOf(0L))
            }
            val result = main {
                activeController.sendCustomCommand(androidx.media3.session.SessionCommand(
                    "UPDATE_LYRICS_DATA", android.os.Bundle.EMPTY), lyrics)
            }.get(10, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(androidx.media3.session.SessionResult.RESULT_SUCCESS, result.resultCode)
            compose.waitUntil(10000) { main { activeController.mediaMetadata.title.toString() == "Speaker lyric" } }
            main {
                assertEquals(1.3f, activeController.playbackParameters.speed)
                assertEquals(0.8f, activeController.playbackParameters.pitch)
                assertTrue(activeController.isPlaying)
                activeController.addMediaItem(androidx.media3.common.MediaItem.Builder()
                    .setMediaId("rate-regression-next").setUri(android.net.Uri.fromFile(file))
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Next title").setArtist("Regression fixture").build()).build())
                assertEquals(1.3f, activeController.playbackParameters.speed)
            }
            // MediaController receives the service's updated command availability asynchronously.
            // An immediate Next in the same call as Add can still be unavailable and ignored.
            compose.waitUntil(5000) { main { activeController.mediaItemCount == 2 &&
                activeController.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) } }
            main { activeController.seekToNextMediaItem() }
            compose.waitUntil(5000) { main { activeController.currentMediaItemIndex == 1 &&
                activeController.playbackParameters.speed == 1f } }
            main { assertEquals(0.8f, activeController.playbackParameters.pitch) }
        } finally {
            main {
                store.clear()
                controller?.stop()
                controller?.release()
                context.stopService(Intent(context, chromahub.rhythm.app.infrastructure.service.MediaPlaybackService::class.java))
                settings.setUseDefaultPlaybackSpeed(savedDefault)
                settings.setDefaultPlaybackSpeed(savedDefaultSpeed)
                settings.setBluetoothLyricsEnabled(savedLyrics)
                settings.setQueuePersistenceEnabled(savedQueue)
                settings.setStopPlaybackOnAppClose(savedClose)
            }
            file.delete()
        }
    }

    @Test fun entranceFramesDoNotRecomposeOrRemeasureContent() {
        val progress = mutableFloatStateOf(0f)
        var compositions = 0
        var layouts = 0
        main {
            host!!.setContent {
                SideEffect { compositions++ }
                Box(Modifier.size(100.dp)
                    .layout { measurable, constraints ->
                        layouts++
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .playerEntranceMotion(progress, 0f, 1f, 40.dp))
            }
        }
        compose.waitForIdle()
        val initialCompositions = compositions
        val initialLayouts = layouts
        repeat(20) { step ->
            main { progress.floatValue = (step + 1) / 20f }
            compose.waitForIdle()
        }
        assertEquals(initialCompositions, compositions)
        assertEquals(initialLayouts, layouts)
    }

    @Test fun canvasKeepsRenderingWhenTheArtworkUrlChanges() {
        val files = listOf("red", "blue").map { color ->
            java.io.File(context.cacheDir, "canvas-$color.mp4").also { file ->
                instrumentation.context.assets.open("canvas-regression/$color.mp4").use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }
        }
        val url = mutableStateOf(android.net.Uri.fromFile(files[0]).toString())
        fun texture(view: android.view.View): android.view.TextureView? {
            if (view is android.view.TextureView) return view
            if (view is android.view.ViewGroup) {
                for (index in 0 until view.childCount) texture(view.getChildAt(index))?.let { return it }
            }
            return null
        }
        fun hasColor(red: Boolean): Boolean = main {
            val bitmap = texture(host!!.window.decorView)?.getBitmap(1, 1) ?: return@main false
            try {
                val pixel = bitmap.getPixel(0, 0)
                if (red) android.graphics.Color.red(pixel) > 200 && android.graphics.Color.blue(pixel) < 50
                else android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 50
            } finally { bitmap.recycle() }
        }
        try {
            main {
                host!!.setContent {
                    // Exercise the production renderer with local clips. Network/consent
                    // eligibility belongs to CanvasArtworkPlayer and has separate policy tests.
                    chromahub.rhythm.app.shared.presentation.components.player.CanvasVideoSurface(
                        primaryUrl = url.value, fallbackUrl = null, modifier = Modifier.size(100.dp))
                }
            }
            compose.waitForIdle()
            compose.waitUntil(15000) { hasColor(red = true) }
            val originalView = main { texture(host!!.window.decorView) }
            main { url.value = android.net.Uri.fromFile(files[1]).toString() }
            compose.waitUntil(15000) { hasColor(red = false) }
            main { assertSame("Changing artwork should retain the video view", originalView, texture(host!!.window.decorView)) }
        } finally {
            main {
                host!!.setContent { }
            }
            files.forEach { it.delete() }
        }
    }

    @Test fun firstMetadataLoadDoesNotMoveAnchorsOrRequireASecondDismissSwipe() {
        val loaded = mutableStateOf(false)
        var dismissals = 0
        var state: SheetState? = null
        main {
            host!!.setContent {
                MaterialTheme {
                    val list = rememberLazyListState()
                    val sheet = rememberBottomSheetState(initialValue = SheetValue.Hidden,
                        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
                    SideEffect { state = sheet }
                    RhythmAdaptiveModalSheet(
                        adaptiveType = SheetAdaptiveType.BOTTOM_SHEET_ONLY,
                        sheetState = sheet, onDismissRequest = { dismissals++ }
                    ) {
                        SongInfoSheetViewport {
                            Text("Song information", Modifier.fillMaxWidth().height(80.dp).testTag("info-header"))
                            AdaptiveSheetScrollContainer(lazyListState = list,
                                reserveScrollBarSpace = true, modifier = Modifier.weight(1f)) { end ->
                                LazyColumn(state = list, contentPadding = PaddingValues(end = end)) {
                                    items(if (loaded.value) 30 else 1) { Text("Metadata $it", Modifier.height(64.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(5000) { main { state?.currentValue == SheetValue.Expanded && !state!!.isAnimationRunning } }
        val initialOffset = main { state!!.requireOffset() }
        val node = compose.onNodeWithTag("info-header")
        node.performTouchInput { down(center); moveBy(Offset(0f, 100f), 100) }
        val before = main { state!!.requireOffset() }
        assertTrue("The first movement must actually drag the sheet: $initialOffset -> $before", before > initialOffset)
        main { loaded.value = true }
        compose.waitForIdle()
        val after = main { state!!.requireOffset() }
        assertEquals("Loading must not move the drag anchor", before, after, 1f)
        // Supply normal motion samples so VelocityTracker sees a flick, not one event after
        // a 150 ms gap (outside its velocity history window).
        node.performTouchInput { repeat(5) { moveBy(Offset(0f, 80f), 16) }; up() }
        compose.waitForIdle()
        main {
            assertTrue("Dismissals=$dismissals, current=${state!!.currentValue}, target=${state!!.targetValue}, offset=${state!!.requireOffset()}", dismissals > 0)
            assertEquals(SheetValue.Hidden, state!!.currentValue)
        }
    }
}
