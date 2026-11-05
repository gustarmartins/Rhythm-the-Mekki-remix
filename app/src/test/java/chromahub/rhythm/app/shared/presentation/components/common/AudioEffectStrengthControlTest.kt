/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real Compose input dispatch, without starting playback or attaching any audio effects. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-rUS")
class AudioEffectStrengthControlTest {
    @get:Rule val compose = createComposeRule()
    private val strength = mutableIntStateOf(0)
    private val enabled = mutableStateOf(true)
    private val changes = mutableListOf<Int>()

    private fun show(initial: Int = 0, active: Boolean = true) {
        strength.intValue = initial
        enabled.value = active
        compose.setContent {
            MaterialTheme {
                AudioEffectStrengthControl(
                    title = "Bass Boost",
                    strength = strength.intValue,
                    enabled = enabled.value,
                    onStrengthChange = { changes += it; strength.intValue = it },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }

    private fun dial() = compose.onNodeWithContentDescription("Bass Boost: ${strength.intValue / 10}%")
    private fun increase() = compose.onNodeWithContentDescription("Increase Bass Boost by 1%")
    private fun decrease() = compose.onNodeWithContentDescription("Decrease Bass Boost by 1%")

    @Test fun touchingTheCenterAndMaximumEndOfTheDialDoesNotChangeGain() {
        show()
        dial().performTouchInput {
            click(center)
            click(Offset(width * 0.8f, height * 0.8f))
            click(Offset(width * 0.5f, height * 0.95f))
        }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        dial().assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    @Test fun crossingTheBottomGapOrSwipingAcrossTheDialCannotWrapToMaximum() {
        show()
        dial().performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
            swipe(Offset(width * 0.8f, height * 0.8f), Offset(width * 0.2f, height * 0.8f))
            swipe(Offset(width * 0.5f, height * 0.9f), Offset(width * 0.5f, height * 0.1f))
        }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test fun eachTapOrAccessibilityClickChangesOnlyOnePercentagePoint() {
        show()
        increase().performTouchInput { click() }
        increase().performClick()
        increase().performClick()
        decrease().performClick()
        compose.runOnIdle { assertEquals(listOf(10, 20, 30, 20), changes) }
    }

    @Test fun holdingIncreaseDoesNotRepeat() {
        show()
        increase().performTouchInput { longClick(durationMillis = 3000) }
        compose.runOnIdle { assertEquals(listOf(10), changes) }
    }

    @Test fun cancelledPressDoesNotApplyAChange() {
        show(200)
        increase().performTouchInput { down(center); cancel() }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test fun endpointsClampWithoutWrappingAndPreserveExistingFineValues() {
        show(995)
        increase().performClick()
        increase().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(1000), changes)
            strength.intValue = 5
        }
        decrease().performClick()
        decrease().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf(1000, 0), changes) }
    }

    @Test fun disabledEffectCannotBeAdjustedOrImplicitlyEnabled() {
        show(500, active = false)
        increase().assertIsNotEnabled().performTouchInput { click() }
        decrease().assertIsNotEnabled().performTouchInput { click() }
        dial().performTouchInput { click(); swipeUp() }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test fun disablingTheEffectWhilePressedCancelsThePendingAdjustment() {
        show(500)
        increase().performTouchInput { down(center) }
        compose.runOnIdle { enabled.value = false }
        increase().performTouchInput { up() }
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test fun scrollingOverTheDialScrollsThePageWithoutAdjustingAudio() {
        var readScroll: () -> Int = { 0 }
        compose.setContent {
            val scroll = rememberScrollState()
            readScroll = { scroll.value }
            MaterialTheme {
                Column(Modifier.width(160.dp).height(240.dp).verticalScroll(scroll)) {
                    AudioEffectStrengthControl("Bass Boost", 0, true, { changes += it })
                    Spacer(Modifier.height(600.dp))
                }
            }
        }
        dial().performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertTrue("The indicator must not capture page scrolling", readScroll() > 0)
            assertTrue(changes.isEmpty())
        }
    }
}
