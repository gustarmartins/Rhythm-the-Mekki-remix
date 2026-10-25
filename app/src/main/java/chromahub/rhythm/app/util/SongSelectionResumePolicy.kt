/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

/** Timers only affect a new selection; they never start playback on their own. */
internal object SongSelectionResumePolicy {
    const val DISABLED = -1L
    const val MAX_SECONDS = 365L * 24 * 60 * 60

    fun shouldResume(
        enabled: Boolean,
        isPlaying: Boolean,
        playWhenReady: Boolean,
        pausedAtElapsedMs: Long?,
        lastPlaybackAtMs: Long?,
        pauseSeconds: Long,
        idleSeconds: Long,
        elapsedMs: Long,
        nowMs: Long
    ): Boolean {
        // Buffering or audio-focus suppression is still an active playback request.
        if (!enabled || isPlaying || playWhenReady) return false
        fun expired(since: Long?, now: Long, seconds: Long): Boolean =
            seconds in 0..MAX_SECONDS && since != null && since >= 0 && now >= since &&
                now - since >= seconds * 1_000L
        return expired(pausedAtElapsedMs, elapsedMs, pauseSeconds) ||
            expired(lastPlaybackAtMs, nowMs, idleSeconds)
    }
}

/** Monotonic pause timing in this process, persisted listening timing across process death. */
internal class SongSelectionPlaybackActivity(startedElapsedMs: Long, lastPlaybackAtMs: Long?) {
    var pausedAtElapsedMs: Long? = startedElapsedMs
        private set
    var lastPlaybackAtMs: Long? = lastPlaybackAtMs
        private set
    private var wasPlaying = false

    fun onPlaybackIntent(playWhenReady: Boolean, elapsedMs: Long) {
        if (playWhenReady) pausedAtElapsedMs = null
        else if (pausedAtElapsedMs == null) pausedAtElapsedMs = elapsedMs
    }

    fun onPlayingChanged(isPlaying: Boolean, nowMs: Long): Boolean {
        val previous = lastPlaybackAtMs
        // Initial/restoration callbacks must not overwrite the previous listening session.
        if (isPlaying || wasPlaying) lastPlaybackAtMs = nowMs
        if (isPlaying) pausedAtElapsedMs = null
        wasPlaying = isPlaying
        return previous != lastPlaybackAtMs
    }
}
