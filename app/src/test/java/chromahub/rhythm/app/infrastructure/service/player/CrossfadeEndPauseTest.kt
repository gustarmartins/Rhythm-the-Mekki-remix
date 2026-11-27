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

    @Test fun crossfadeDurationIsInListeningTimeAtEverySpeed() {
        assertEquals(CrossfadeWindow(55_950, 4_000), CrossfadeWindow.forTrack(60_000, 1f, 4_000))
        assertEquals(CrossfadeWindow(51_900, 4_000), CrossfadeWindow.forTrack(60_000, 2f, 4_000))
        assertEquals(CrossfadeWindow(57_975, 4_000), CrossfadeWindow.forTrack(60_000, 0.5f, 4_000))
        assertEquals(1_500L, playbackTimeMs(3_000, 2f))
    }

    @Test fun aShortTrackMustHaveEnoughListeningTimeForBothSidesOfTheOverlap() {
        assertNull(CrossfadeWindow.forTrack(1_900, 2f, 4_000))
        assertNull(CrossfadeWindow.forTrack(8_000, 1f, 0))
        assertEquals(CrossfadeWindow(2_100, 900), CrossfadeWindow.forTrack(4_000, 2f, 4_000))
    }
}
