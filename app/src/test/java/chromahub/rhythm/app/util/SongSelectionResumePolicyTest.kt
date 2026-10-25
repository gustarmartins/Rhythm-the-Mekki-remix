/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class SongSelectionResumePolicyTest {
    private fun resumes(
        enabled: Boolean = true, playing: Boolean = false, playWhenReady: Boolean = false,
        pausedAt: Long? = 1_000, lastPlayback: Long? = 1_000,
        pause: Long = 30, idle: Long = -1, elapsed: Long = 31_000, now: Long = 31_000
    ) = SongSelectionResumePolicy.shouldResume(
        enabled, playing, playWhenReady, pausedAt, lastPlayback, pause, idle, elapsed, now
    )

    @Test fun disabledFeaturePreservesPlayNextEvenAfterLongInactivity() {
        assertFalse(resumes(enabled = false, pause = 0, idle = 0))
    }

    @Test fun pausedDelayExpiresExactlyAtTheConfiguredBoundary() {
        assertFalse(resumes(elapsed = 30_999))
        assertTrue(resumes(elapsed = 31_000))
    }

    @Test fun zeroPauseDelayPlaysTheTappedSongImmediately() {
        assertTrue(resumes(pause = 0, elapsed = 1_000))
    }

    @Test fun listeningBufferingAndFocusSuppressionKeepPlayNext() {
        assertFalse(resumes(playing = true, pause = 0, idle = 0))
        assertFalse(resumes(playWhenReady = true, pause = 0, idle = 0))
    }

    @Test fun eitherTimerCanBeDisabledIndependently() {
        assertTrue(resumes(pause = -1, idle = 30))
        assertTrue(resumes(pause = 30, idle = -1))
        assertFalse(resumes(pause = -1, idle = -1))
    }

    @Test fun restoredSessionUsesLastListeningTimeWhileItsPauseTimerIsNew() {
        assertTrue(resumes(pausedAt = 31_000, pause = 300, idle = 30))
        assertFalse(resumes(pausedAt = 31_000, pause = 300, idle = 31))
    }

    @Test fun missingHistoryAndClockRollbackDoNotSatisfyIdleThreshold() {
        assertFalse(resumes(pause = -1, idle = 0, lastPlayback = null))
        assertFalse(resumes(pause = -1, idle = 0, lastPlayback = 31_001))
        assertFalse(resumes(pause = 0, pausedAt = 31_001))
    }

    @Test fun invalidOrOverflowingThresholdsNeverResume() {
        assertFalse(resumes(pause = Long.MAX_VALUE))
        assertFalse(resumes(pause = -2))
    }

    @Test fun repeatedPauseCallbacksDoNotRestartTheClock() {
        val activity = SongSelectionPlaybackActivity(0, null)
        activity.onPlaybackIntent(true, 100)
        activity.onPlaybackIntent(false, 200)
        activity.onPlaybackIntent(false, 300)
        assertEquals(200L, activity.pausedAtElapsedMs)
        activity.onPlaybackIntent(true, 400)
        activity.onPlaybackIntent(false, 500)
        assertEquals(500L, activity.pausedAtElapsedMs)
    }

    @Test fun restorationDoesNotErasePreviousListeningTimestamp() {
        val activity = SongSelectionPlaybackActivity(10_000, 500)
        assertFalse(activity.onPlayingChanged(false, 10_000))
        assertEquals(500L, activity.lastPlaybackAtMs)
        assertTrue(activity.onPlayingChanged(true, 11_000))
        assertTrue(activity.onPlayingChanged(false, 12_000))
        assertFalse(activity.onPlayingChanged(false, 13_000))
        assertEquals(12_000L, activity.lastPlaybackAtMs)
    }
}
