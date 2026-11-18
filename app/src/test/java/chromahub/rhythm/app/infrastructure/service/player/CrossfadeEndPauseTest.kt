/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

import org.junit.Assert.*
import org.junit.Test

class CrossfadeEndPauseTest {
    @Test fun nativeTraversalIsHeldOnlyNearAStillPendingFade() {
        assertFalse(shouldArmCrossfadeEndPause(180000))
        assertFalse(shouldArmCrossfadeEndPause(2001))
        assertTrue(shouldArmCrossfadeEndPause(2000))
        assertTrue(shouldArmCrossfadeEndPause(0))
        assertFalse(shouldArmCrossfadeEndPause(-1))
    }
}
