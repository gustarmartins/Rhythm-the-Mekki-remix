/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

/**
 * Main-thread admission outcomes, not an estimate of Android's live importance.
 * Only a new playback request retries a failed attempt; metadata callbacks never
 * start a second recovery loop. Tokens exclude obsolete async completions.
 */
internal class ForegroundPromotionState {
    private var latestAttempt = 0L
    private var retryNeeded = false

    fun beginAttempt(): Long = ++latestAttempt

    fun completeAttempt(attempt: Long, succeeded: Boolean): Boolean {
        if (attempt != latestAttempt) return false
        retryNeeded = !succeeded
        return true
    }

    fun shouldAttemptBeforePlayback(
        playWhenReady: Boolean,
        mediaItemCount: Int,
        blockedByListeningLimit: Boolean,
    ): Boolean = mediaItemCount > 0 && !blockedByListeningLimit && (!playWhenReady || retryNeeded)
}
