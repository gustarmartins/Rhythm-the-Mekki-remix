/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

/** Do not suppress native repeat/advance for the whole track while waiting for a fade. */
internal fun shouldArmCrossfadeEndPause(remainingUntilTransitionMs: Long): Boolean =
    remainingUntilTransitionMs in 0L..2000L

/** ExoPlayer positions use media time. The overlap and scheduling lead are wall-clock durations. */
internal fun playbackTimeMs(mediaDurationMs: Long, speed: Float): Long =
    (mediaDurationMs / speed.toDouble()).toLong()

/** Estimate progress between coarse AudioTrack position updates near a scheduled fade. */
internal class CrossfadePositionClock {
    private var lastPositionMs: Long? = null
    private var lastAdvanceTimeMs = 0L
    private var observedStepMs = 0L

    fun position(positionMs: Long, bufferedMs: Long, playing: Boolean, speed: Float, nowMs: Long): Long {
        if (!playing) {
            lastPositionMs = null
            return positionMs
        }
        val previous = lastPositionMs
        if (previous != positionMs) {
            observedStepMs = if (previous == null) 0L else (positionMs - previous).coerceAtLeast(0L)
            lastPositionMs = positionMs
            lastAdvanceTimeMs = nowMs
        }
        // Never project beyond buffered audio, one observed position step, or 450 ms of wall time.
        val elapsed = (nowMs - lastAdvanceTimeMs).coerceIn(0L, 450L)
        return positionMs + minOf((elapsed * speed).toLong(), observedStepMs, (bufferedMs - positionMs).coerceAtLeast(0L))
    }
}

internal data class CrossfadeWindow(val startPositionMs: Long, val durationMs: Long) {
    companion object {
        fun forTrack(trackDurationMs: Long, speed: Float, requestedMs: Int): CrossfadeWindow? {
            val trackTimeMs = playbackTimeMs(trackDurationMs, speed)
            if (trackTimeMs < 1_000 || requestedMs <= 0) return null
            val maxFadeMs = (trackTimeMs * 0.45).toLong().coerceAtLeast(500L)
            val fadeMs = requestedMs.toLong().coerceIn(500L, maxFadeMs)
            val mediaLeadMs = ((fadeMs + 50L) * speed.toDouble()).toLong()
            return CrossfadeWindow((trackDurationMs - mediaLeadMs).coerceAtLeast(0L), fadeMs)
        }
    }
}
