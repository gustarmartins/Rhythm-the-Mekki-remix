/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Sendspin can connect before MA finishes registering its player queue. Retry reads only. */
internal suspend fun awaitMusicAssistantQueue(
    timeoutMs: Long = 30_000L,
    pollIntervalMs: Long = 250L,
    readQueueId: suspend () -> String?,
): String = withTimeout(timeoutMs) {
    require(pollIntervalMs > 0)
    var queueId = readQueueId()
    while (queueId == null) {
        delay(pollIntervalMs)
        queueId = readQueueId()
    }
    queueId
}

/** A failed replacement must never turn its following Play into a resume of the old queue. */
internal class MusicAssistantQueueSelection {
    var generation: Long = 0L
        private set
    private var confirmedGeneration = 0L
    private var hasMedia = true // Resume of an existing server queue, before any explicit selection.

    fun beginReplacement(): Long = ++generation

    fun confirmReplacement(request: Long, hasMedia: Boolean) {
        if (request != generation) return
        confirmedGeneration = request
        this.hasMedia = hasMedia
    }

    // clear + add remains a valid Media3 path; additions cannot confirm a failed replacement.
    fun confirmAddition(request: Long) {
        if (request == generation && confirmedGeneration == request) hasMedia = true
    }

    /** Superseded requests do nothing; an unconfirmed/failed current selection is an error. */
    fun canPlay(request: Long): Boolean {
        if (request != generation) return false
        check(confirmedGeneration == generation && hasMedia) {
            "The requested queue was not loaded. Select the music again."
        }
        return true
    }
}
