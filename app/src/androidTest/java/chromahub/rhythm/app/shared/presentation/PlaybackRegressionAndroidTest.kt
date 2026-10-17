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
