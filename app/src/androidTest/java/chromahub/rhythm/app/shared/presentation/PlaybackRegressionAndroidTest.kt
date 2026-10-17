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
            main {
                engine!!.release()
                engine = RhythmPlayerEngine(context).also { it.initialize() }
                assertEquals(0.75f, engine!!.masterPlayer.playbackParameters.speed)
                assertEquals(0.75f, engine!!.masterPlayer.playbackParameters.pitch)
            }
        } finally { main { engine?.release() } }
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
                activeController.addMediaItem(activeController.currentMediaItem!!)
                assertEquals(1.3f, activeController.playbackParameters.speed)
                activeController.seekToNextMediaItem()
            }
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
        val node = compose.onNodeWithTag("info-header")
        node.performTouchInput { down(center); moveBy(Offset(0f, 100f), 100) }
        val before = main { state!!.requireOffset() }
        main { loaded.value = true }
        compose.waitForIdle()
        val after = main { state!!.requireOffset() }
        assertEquals("Loading must not move the drag anchor", before, after, 1f)
        node.performTouchInput { moveBy(Offset(0f, 400f), 150); up() }
        compose.waitUntil(5000) { main { dismissals == 1 } }
    }
}
