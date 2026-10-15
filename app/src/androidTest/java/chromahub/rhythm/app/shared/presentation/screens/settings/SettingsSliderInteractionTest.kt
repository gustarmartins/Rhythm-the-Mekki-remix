/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.screens.settings

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetProgressAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercise actual settings sheets across recompositions while a finger is still down.
 * Pass `-e displayId <secondary display>` to use an already-created isolated test display.
 */
@RunWith(AndroidJUnit4::class)
class SettingsSliderInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val settings = AppSettings.getInstance(context)
    private var restoreSettings: () -> Unit = {}
    private var testActivity: ComponentActivity? = null

    @Before fun startHostAndSaveSettings() {
        val requestedDisplay = InstrumentationRegistry.getArguments().getString("displayId")?.let { raw ->
            requireNotNull(raw.toIntOrNull()?.takeIf { it > 0 }) {
                "displayId must identify an existing secondary display"
            }
        }
        val options = requestedDisplay?.let { displayId ->
            require(Build.VERSION.SDK_INT >= 28) { "Display-targeted instrumentation requires Android 9+" }
            val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            requireNotNull(manager.getDisplay(displayId)) { "Test display $displayId no longer exists" }
            ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()
        }
        // ui-test-manifest declares ComponentActivity in the debug app. Always create a separate
        // task so cleanup cannot remove another Rhythm activity, even in a standard display run.
        val intent = Intent(context, ComponentActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        testActivity = if (options == null) {
            instrumentation.startActivitySync(intent)
        } else {
            instrumentation.startActivitySync(intent, options)
        } as ComponentActivity
        instrumentation.runOnMainSync {
            if (requestedDisplay != null) {
                assertEquals(
                    "Test activity must stay on the requested display",
                    requestedDisplay,
                    requireNotNull(testActivity!!.window.decorView.display).displayId
                )
            }
            saveSettings()
        }
    }

    private fun saveSettings() {
        val playerTheme = settings.playerThemeId.value
        val miniTheme = settings.miniPlayerThemeId.value
        val shapes = settings.expressiveShapesEnabled.value
        val radius = settings.playerArtworkCornerRadius.value
        val miniRadius = settings.miniPlayerCornerRadius.value
        val miniSize = settings.miniPlayerArtworkSize.value
        val ambient = settings.playerAmbientBackdropEnabled.value
        val accent = settings.playerAccentBackgroundEnabled.value
        val intensity = settings.playerAmbientBackdropIntensity.value
        restoreSettings = {
            settings.setPlayerThemeId(playerTheme)
            settings.setMiniPlayerThemeId(miniTheme)
            settings.setExpressiveShapesEnabled(shapes)
            settings.setPlayerArtworkCornerRadius(radius)
            settings.setMiniPlayerCornerRadius(miniRadius)
            settings.setMiniPlayerArtworkSize(miniSize)
            settings.setPlayerAmbientBackdropEnabled(ambient)
            settings.setPlayerAccentBackgroundEnabled(accent)
            settings.setPlayerAmbientBackdropIntensity(intensity)
        }
        settings.setPlayerThemeId("MATERIAL")
        settings.setMiniPlayerThemeId("MATERIAL")
        settings.setExpressiveShapesEnabled(false)
    }

    @After fun restoreSettingsAndCloseHost() {
        try {
            instrumentation.runOnMainSync { restoreSettings.invoke() }
        } finally {
            // ActivityScenario.close() uses an EmptyActivity on the default display. Finish only
            // our task directly so secondary-display tests never launch that foreground helper.
            instrumentation.runOnMainSync { testActivity?.finishAndRemoveTask() }
            instrumentation.waitForIdleSync()
            testActivity = null
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        instrumentation.runOnMainSync { checkNotNull(testActivity).setContent(content = content) }
        compose.waitForIdle()
    }

    @Test fun miniPlayerRadiusKeepsDraggingAndRemainsOpenForAnotherAdjustment() {
        compose.runOnIdle { settings.setMiniPlayerCornerRadius(14) }
        setContent { MaterialTheme { MiniPlayerCustomizationSettingsScreen {} } }
        openSetting(R.string.settings_miniplayer_corner_radius)
        dragAcrossRecomposition({ settings.miniPlayerCornerRadius.value.toFloat() }, 14f)
        compose.runOnIdle { assertTrue(settings.miniPlayerCornerRadius.value > 20) }
        dragBack()
        compose.runOnIdle { assertTrue(settings.miniPlayerCornerRadius.value < 10) }
    }

    @Test fun miniPlayerArtworkSizeKeepsDraggingAndRemainsOpenForAnotherAdjustment() {
        compose.runOnIdle { settings.setMiniPlayerArtworkSize(56) }
        setContent { MaterialTheme { MiniPlayerCustomizationSettingsScreen {} } }
        openSetting(R.string.settings_miniplayer_artwork_size)
        dragAcrossRecomposition({ settings.miniPlayerArtworkSize.value.toFloat() }, 56f)
        compose.runOnIdle { assertTrue(settings.miniPlayerArtworkSize.value > 64) }
        dragBack()
        compose.runOnIdle { assertTrue(settings.miniPlayerArtworkSize.value < 50) }
    }

    @Test fun playerArtworkRadiusCommitsOnlyAfterTheCompleteDrag() {
        compose.runOnIdle { settings.setPlayerArtworkCornerRadius(20) }
        setContent { MaterialTheme { PlayerCustomizationSettingsScreen {} } }
        openSetting(R.string.settings_miniplayer_corner_radius)
        dragAcrossRecomposition({ settings.playerArtworkCornerRadius.value.toFloat() }, 20f)
        compose.runOnIdle { assertTrue(settings.playerArtworkCornerRadius.value > 30) }
        dragBack()
        compose.runOnIdle { assertTrue(settings.playerArtworkCornerRadius.value < 12) }
    }

    @Test fun ambientToggleAndIntensityRemainUsableAfterPreferenceUpdates() {
        compose.runOnIdle {
            settings.setPlayerThemeId("EXPRESSIVE")
            settings.setPlayerAmbientBackdropEnabled(false)
            settings.setPlayerAmbientBackdropIntensity(0.5f)
        }
        setContent { MaterialTheme { PlayerCustomizationSettingsScreen {} } }
        openSetting(R.string.player_ambient_backdrop)
        compose.runOnIdle {
            assertTrue(settings.playerAmbientBackdropEnabled.value)
            assertFalse(settings.playerAccentBackgroundEnabled.value)
        }
        // A second toggle must work after the preference flow recomposes the row.
        openSetting(R.string.player_ambient_backdrop)
        compose.runOnIdle { assertFalse(settings.playerAmbientBackdropEnabled.value) }
        openSetting(R.string.player_ambient_backdrop)
        openSetting(R.string.player_ambient_intensity)
        dragAcrossRecomposition({ settings.playerAmbientBackdropIntensity.value }, 0.5f)
        compose.runOnIdle { assertTrue(settings.playerAmbientBackdropIntensity.value > 0.75f) }
        dragBack()
        compose.runOnIdle { assertTrue(settings.playerAmbientBackdropIntensity.value < 0.3f) }
    }

    private fun openSetting(titleResource: Int) {
        val title = context.getString(titleResource)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    private fun dragAcrossRecomposition(readSavedValue: () -> Float, initialValue: Float) {
        val slider = compose.onNode(hasSetProgressAction()).assertIsDisplayed()
        slider.performTouchInput {
            down(Offset(width * 0.5f, centerY))
            moveTo(Offset(width * 0.65f, centerY))
        }
        // Let onValueChange recompose the sheet before continuing the SAME gesture.
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(initialValue, readSavedValue(), 0.001f) }
        slider.assertIsDisplayed().performTouchInput {
            moveTo(Offset(width * 0.85f, centerY))
            up()
        }
        compose.waitForIdle()
        slider.assertIsDisplayed()
    }

    private fun dragBack() {
        val slider = compose.onNode(hasSetProgressAction()).assertIsDisplayed()
        slider.performTouchInput {
            down(Offset(width * 0.85f, centerY))
            moveTo(Offset(width * 0.55f, centerY))
        }
        compose.waitForIdle()
        slider.assertIsDisplayed().performTouchInput {
            moveTo(Offset(width * 0.2f, centerY))
            up()
        }
        compose.waitForIdle()
        slider.assertIsDisplayed()
    }
}
