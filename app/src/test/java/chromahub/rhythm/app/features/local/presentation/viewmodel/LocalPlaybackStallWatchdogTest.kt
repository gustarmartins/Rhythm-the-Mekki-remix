/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.local.presentation.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPlaybackStallWatchdogTest {
    private val local = LocalPlaybackStallWatchdog.Sample(
        occurrenceKey = "song-a:occurrence-1",
        sourceUri = "content://media/external/audio/media/42",
        ready = true,
        playing = true,
        seeking = false,
        checkerEnabled = true,
        positionMs = 0,
    )

    @Test fun readyLocalDecoderWithoutProgressStillTriggersTheGuard() {
        for (uri in listOf(local.sourceUri, "file:///music/track.flac", "/music/track.flac")) {
            val watchdog = LocalPlaybackStallWatchdog()
            val sample = local.copy(sourceUri = uri)
            assertFalse(watchdog.update(sample, 1_000))
            assertFalse(watchdog.update(sample, 10_999))
            assertTrue(watchdog.update(sample, 11_000))
        }
    }

    @Test fun musicAssistantAndOtherNetworkSourcesAreNeverFileCorruption() {
        for (uri in listOf("musicassistant://library/track/42", "https://server/audio.flac", "http://server/audio.mp3")) {
            val watchdog = LocalPlaybackStallWatchdog()
            val sample = local.copy(sourceUri = uri)
            assertFalse(watchdog.update(sample, 0))
            assertFalse(watchdog.update(sample, 60_000))
            // Reproduces the full-library MA startup: position zero while buffering >10s.
            assertFalse(watchdog.update(sample.copy(ready = false, playing = false), 120_000))
        }
    }

    @Test fun localBufferingStartsAFreshDeadlineOnlyWhenReadyAndPlaying() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(local.copy(ready = false, playing = false), 9_000))
        assertFalse(watchdog.update(local.copy(ready = false, playing = false), 60_000))
        assertFalse(watchdog.update(local, 61_000))
        assertFalse(watchdog.update(local, 70_999))
        assertTrue(watchdog.update(local, 71_000))
    }

    @Test fun pausedOrSuppressedPlaybackCannotAccumulateAStall() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(local.copy(playing = false), 9_000))
        assertFalse(watchdog.update(local.copy(playing = false), 60_000))
        assertFalse(watchdog.update(local, 61_000))
        assertFalse(watchdog.update(local, 70_999))
        assertTrue(watchdog.update(local, 71_000))
    }

    @Test fun seekingResetsTheDeadlineEvenAtTheSamePosition() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(local.copy(seeking = true), 9_000))
        assertFalse(watchdog.update(local.copy(seeking = true), 60_000))
        assertFalse(watchdog.update(local, 61_000))
        assertFalse(watchdog.update(local, 70_999))
        assertTrue(watchdog.update(local, 71_000))
    }

    @Test fun duplicateTrackOccurrenceDoesNotInheritThePreviousDeadline() {
        val watchdog = LocalPlaybackStallWatchdog()
        val duplicate = local.copy(occurrenceKey = "song-a:occurrence-2")
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(duplicate, 9_000))
        assertFalse(watchdog.update(duplicate, 18_999))
        assertTrue(watchdog.update(duplicate, 19_000))
    }

    @Test fun queueReplacementOrRepeatCanResetTheSameOccurrenceAndPosition() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        watchdog.reset()
        assertFalse(watchdog.update(local, 9_000))
        assertFalse(watchdog.update(local, 18_999))
        assertTrue(watchdog.update(local, 19_000))
    }

    @Test fun advancingOrSeekingBackwardsResetsTheDeadline() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(local.copy(positionMs = 9_000), 9_000))
        assertFalse(watchdog.update(local.copy(positionMs = 1_000), 18_000))
        assertFalse(watchdog.update(local.copy(positionMs = 1_000), 27_999))
        assertTrue(watchdog.update(local.copy(positionMs = 1_000), 28_000))
    }

    @Test fun unknownSourceCannotBeAssumedToBeACorruptLocalFile() {
        for (uri in listOf(null, "")) {
            val watchdog = LocalPlaybackStallWatchdog()
            val unknown = local.copy(sourceUri = uri)
            assertFalse(watchdog.update(unknown, 0))
            assertFalse(watchdog.update(unknown, 60_000))
        }
    }

    @Test fun disabledCheckerResetsTheDeadlineAndDoesNotPausePlayback() {
        val watchdog = LocalPlaybackStallWatchdog()
        assertFalse(watchdog.update(local, 0))
        assertFalse(watchdog.update(local.copy(checkerEnabled = false), 9_000))
        assertFalse(watchdog.update(local.copy(checkerEnabled = false), 60_000))
        assertFalse(watchdog.update(local, 61_000))
        assertFalse(watchdog.update(local, 70_999))
        assertTrue(watchdog.update(local, 71_000))
    }
}
