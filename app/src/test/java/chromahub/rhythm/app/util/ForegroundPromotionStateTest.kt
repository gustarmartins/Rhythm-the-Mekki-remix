/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class ForegroundPromotionStateTest {
    @Test fun pausedStartIsPromotedButSuccessfulOngoingPlaybackIsNotRepeated() {
        val state = ForegroundPromotionState()
        assertTrue(state.shouldAttemptBeforePlayback(false, 1, false))
        state.completeAttempt(state.beginAttempt(), true)
        assertFalse(state.shouldAttemptBeforePlayback(true, 1, false))
    }

    @Test fun failedAdmissionCanRecoverWithoutTogglingTheExistingPlayRequest() {
        val state = ForegroundPromotionState()
        state.completeAttempt(state.beginAttempt(), false)
        assertTrue(state.shouldAttemptBeforePlayback(true, 1, false))
        state.completeAttempt(state.beginAttempt(), true)
        assertFalse(state.shouldAttemptBeforePlayback(true, 1, false))
    }

    @Test fun staleSuccessCannotHideANewerFailure() {
        val state = ForegroundPromotionState()
        val earlier = state.beginAttempt()
        state.completeAttempt(state.beginAttempt(), false)
        assertFalse(state.completeAttempt(earlier, true))
        assertTrue(state.shouldAttemptBeforePlayback(true, 1, false))
    }

    @Test fun staleFailureCannotCreateRetryAfterSuccessfulNewAdmission() {
        val state = ForegroundPromotionState()
        val earlier = state.beginAttempt()
        state.completeAttempt(state.beginAttempt(), true)
        assertFalse(state.completeAttempt(earlier, false))
        assertFalse(state.shouldAttemptBeforePlayback(true, 1, false))
    }

    @Test fun eachExplicitRetryRemainsEligibleUntilAnAdmissionSucceeds() {
        val state = ForegroundPromotionState()
        repeat(3) {
            state.completeAttempt(state.beginAttempt(), false)
            assertTrue(state.shouldAttemptBeforePlayback(true, 1, false))
        }
        state.completeAttempt(state.beginAttempt(), true)
        assertFalse(state.shouldAttemptBeforePlayback(true, 1, false))
    }

    @Test fun recoveryPreservesEmptyQueueAndListeningLimit() {
        val state = ForegroundPromotionState()
        state.completeAttempt(state.beginAttempt(), false)
        assertFalse(state.shouldAttemptBeforePlayback(true, 0, false))
        assertFalse(state.shouldAttemptBeforePlayback(false, 0, false))
        assertFalse(state.shouldAttemptBeforePlayback(true, 1, true))
        assertFalse(state.shouldAttemptBeforePlayback(false, 1, true))
    }
}
