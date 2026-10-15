/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.local.presentation.viewmodel

import chromahub.rhythm.app.infrastructure.service.player.MissingLocalMediaClassifier

/** Detect a local decoder that reports ready/playing but never advances its position. */
internal class LocalPlaybackStallWatchdog(private val timeoutMs: Long = 10_000L) {
    data class Sample(
        val occurrenceKey: String?,
        val sourceUri: String?,
        val ready: Boolean,
        val playing: Boolean,
        val seeking: Boolean,
        val checkerEnabled: Boolean,
        val positionMs: Long,
    )

    private var occurrenceKey: String? = null
    private var lastPositionMs: Long? = null
    private var lastProgressAtMs = 0L

    /** [nowMs] must use a monotonic clock; buffering and network stalls are not file corruption. */
    fun update(sample: Sample, nowMs: Long): Boolean {
        val source = sample.sourceUri?.takeIf { it.isNotBlank() }
        val localSource = source != null && (source.startsWith('/') ||
            MissingLocalMediaClassifier.isLocalScheme(source.substringBefore(':', "")))
        if (!sample.checkerEnabled || !localSource || !sample.ready || !sample.playing ||
            sample.seeking || sample.occurrenceKey == null || sample.positionMs < 0
        ) {
            reset()
            return false
        }
        if (occurrenceKey != sample.occurrenceKey || lastPositionMs != sample.positionMs) {
            occurrenceKey = sample.occurrenceKey
            lastPositionMs = sample.positionMs
            lastProgressAtMs = nowMs
            return false
        }
        return nowMs - lastProgressAtMs >= timeoutMs
    }

    /** Queue replacement, repeat and seeks can retain the same media ID and position. */
    fun reset() {
        occurrenceKey = null
        lastPositionMs = null
        lastProgressAtMs = 0L
    }
}
