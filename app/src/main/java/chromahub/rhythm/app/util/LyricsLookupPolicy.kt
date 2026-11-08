package chromahub.rhythm.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

// Includes admission to the optional-work gate, not just time on the network.
internal const val LYRICS_LOOKUP_TIMEOUT_MS = 8_000L
internal const val LYRICS_PROVIDER_TIMEOUT_MS = 4_000L

internal suspend fun <T> boundedLyricsLookup(
    timeoutMs: Long = LYRICS_LOOKUP_TIMEOUT_MS,
    lookup: suspend () -> T?
): T? = withTimeoutOrNull(timeoutMs) { lookup() }

/** Start every enabled provider, then select in preference order within one provider budget. */
internal suspend fun <T> firstAvailableLyricsProvider(
    providers: List<suspend () -> T?>,
    timeoutMs: Long = LYRICS_PROVIDER_TIMEOUT_MS,
    preferred: (T) -> Boolean = { true }
): T? = coroutineScope {
    val requests = providers.map { fetch ->
        async {
            try {
                withTimeoutOrNull(timeoutMs) { fetch() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }
    try {
        var backup: T? = null
        for (request in requests) {
            val candidate = request.await() ?: continue
            if (preferred(candidate)) return@coroutineScope candidate
            if (backup == null) backup = candidate
        }
        backup
    } finally {
        requests.forEach { it.cancel() }
    }
}
