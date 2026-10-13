package chromahub.rhythm.app.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

enum class OptionalWork { ARTWORK, LYRICS, LIBRARY, CANVAS }

data class OptionalWorkPolicy(
    val foreground: Boolean = false,
    val saver: Boolean = false,
    val blocked: Set<OptionalWork> = emptySet()
) {
    val concurrency: Int get() = if (foreground && !saver) 3 else 1
    fun allows(kind: OptionalWork): Boolean = !saver || kind !in blocked
}

/** Battery samples are events, not a polling loop. Hysteresis avoids bouncing at the boundary. */
internal fun automaticBatterySaver(
    enabled: Boolean, percent: Int?, charging: Boolean, threshold: Int, wasActive: Boolean
): Boolean = enabled && !charging && percent != null &&
    percent <= threshold + if (wasActive) 2 else 0

/** Shared admission across repository instances. Waiting jobs consume no worker threads. */
internal class OptionalWorkGate(initial: OptionalWorkPolicy = OptionalWorkPolicy()) {
    private val lock = Any()
    private val revision = MutableStateFlow(0L)
    private var active = 0
    @Volatile var policy = initial
        private set

    fun update(next: OptionalWorkPolicy) = synchronized(lock) {
        policy = next
        revision.value++
    }

    suspend fun <T> run(kind: OptionalWork, block: suspend () -> T): T {
        while (true) {
            currentCoroutineContext().ensureActive()
            val observed = revision.value
            val acquired = synchronized(lock) {
                if (policy.allows(kind) && active < policy.concurrency) {
                    active++
                    true
                } else false
            }
            if (acquired) break
            revision.first { it != observed }
        }
        try {
            val result = block()
            // Bound sustained library CPU/IO, not just simultaneous requests.
            if (!policy.foreground) delay(250)
            return result
        } finally {
            synchronized(lock) { active--; revision.value++ }
        }
    }
}
