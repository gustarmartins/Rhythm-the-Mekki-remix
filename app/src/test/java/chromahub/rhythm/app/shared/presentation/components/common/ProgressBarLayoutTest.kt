/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ProgressBarLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectingWavyAllocatesTheWaveHeightInsteadOfSqueezingItIntoTheThinTrack() {
        val style = mutableStateOf(ProgressStyle.NORMAL)
        compose.setContent {
            MaterialTheme {
                StyledProgressBar(0.45f, style.value, Modifier.testTag("progress"), height = 4.dp)
            }
        }
        compose.onNodeWithTag("progress").assertHeightIsEqualTo(4.dp)
        compose.runOnIdle { style.value = ProgressStyle.WAVY }
        compose.onNodeWithTag("progress").assertHeightIsAtLeast(24.dp)
    }

    @Test fun tabletWavyTrackHasTheSameRoomWhenPaused() {
        compose.setContent {
            MaterialTheme {
                StyledProgressBar(0.5f, ProgressStyle.WAVY, Modifier.testTag("progress"), height = 3.dp, isPlaying = false)
            }
        }
        compose.onNodeWithTag("progress").assertHeightIsAtLeast(24.dp)
    }
}
