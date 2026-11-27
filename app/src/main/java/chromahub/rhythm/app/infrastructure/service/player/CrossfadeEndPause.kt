/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

/** Do not suppress native repeat/advance for the whole track while waiting for a fade. */
internal fun shouldArmCrossfadeEndPause(remainingUntilTransitionMs: Long): Boolean =
    remainingUntilTransitionMs in 0L..2000L

/** ExoPlayer positions use media time. The overlap and scheduling lead are wall-clock durations. */
internal fun playbackTimeMs(mediaDurationMs: Long, speed: Float): Long =
    (mediaDurationMs / speed.toDouble()).toLong()

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
