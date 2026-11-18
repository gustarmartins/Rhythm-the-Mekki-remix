/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.icons

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class RepeatModeIconTest {
    @Test fun realMedia3ModesMapToTheCorrectSharedPhoneAndTabletPresentation() {
        assertEquals(RhythmIcons.Player.RepeatOne, repeatModeIcon(Player.REPEAT_MODE_ONE))
        assertEquals(RhythmIcons.Player.Repeat, repeatModeIcon(Player.REPEAT_MODE_ALL))
        assertEquals(RhythmIcons.Player.Repeat, repeatModeIcon(Player.REPEAT_MODE_OFF))
    }
}
