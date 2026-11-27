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

    @Test fun nearEndClockInterpolatesCoarsePlayingPositionsWithinBufferedAudio() {
        val clock = CrossfadePositionClock()
        fun sample(pos: Long, now: Long, playing: Boolean = true, buffer: Long = 6_000L) =
            clock.position(pos, buffer, playing, 0.5f, now)
        assertEquals(5_292L, sample(5_292, 10_783))
        assertEquals(5_462L, sample(5_462, 11_112))
        assertEquals(5_475L, sample(5_462, 11_138))
        assertEquals(5_470L, sample(5_462, 11_150, buffer = 5_470))
        assertEquals(5_632L, sample(5_462, 12_000))
        assertEquals(5_462L, sample(5_462, 12_020, playing = false))
        assertEquals(5_462L, sample(5_462, 14_100))
        assertEquals(5_462L, sample(5_462, 14_300))
        assertEquals(1_000L, sample(1_000, 14_320))
        assertEquals(1_000L, sample(1_000, 14_620))
    }

    @Test fun aShortTrackMustHaveEnoughListeningTimeForBothSidesOfTheOverlap() {
        assertNull(CrossfadeWindow.forTrack(1_900, 2f, 4_000))
        assertNull(CrossfadeWindow.forTrack(8_000, 1f, 0))
        assertEquals(CrossfadeWindow(2_100, 900), CrossfadeWindow.forTrack(4_000, 2f, 4_000))
    }
}
