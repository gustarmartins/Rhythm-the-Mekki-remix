/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One persisted transaction and one engine update for tempo, pitch and their link. */
data class PlaybackRate(val speed: Float = 1f, val pitch: Float = 1f, val synced: Boolean = false) {
    fun normalized(): PlaybackRate {
        val safeSpeed = sanitize(speed)
        return copy(speed = safeSpeed, pitch = if (synced) safeSpeed else sanitize(pitch))
    }

    fun withSpeed(value: Float) = copy(speed = value).normalized()
    fun withPitch(value: Float) = copy(pitch = value, speed = if (synced) value else speed).normalized()
    fun withSync(enabled: Boolean) = copy(synced = enabled).normalized()

    companion object {
        fun sanitize(value: Float) = if (value.isFinite()) value.coerceIn(0.25f, 3f) else 1f
    }
}

/** The combined flow is authoritative; scalar flows are only for existing UI readers. */
class PlaybackRateStore(initial: PlaybackRate, private val persist: (PlaybackRate) -> Unit) {
    private val current = MutableStateFlow(initial.normalized())
    val state = current.asStateFlow()
    private val speedState = MutableStateFlow(current.value.speed)
    val speed = speedState.asStateFlow()
    private val pitchState = MutableStateFlow(current.value.pitch)
    val pitch = pitchState.asStateFlow()
    private val syncState = MutableStateFlow(current.value.synced)
    val synced = syncState.asStateFlow()

    @Synchronized
    fun update(transform: (PlaybackRate) -> PlaybackRate) {
        val next = transform(current.value).normalized()
        if (next == current.value) return
        persist(next)
        speedState.value = next.speed
        pitchState.value = next.pitch
        syncState.value = next.synced
        current.value = next
    }
}
