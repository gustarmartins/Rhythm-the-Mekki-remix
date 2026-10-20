package chromahub.rhythm.app.features.streaming.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun streamingSearchEnabled(
    appMode: String,
    authenticated: Boolean,
    online: Boolean,
    offlineMode: Boolean
): Boolean = appMode == "STREAMING" && authenticated && online && !offlineMode

/** Runs automatic search only while the selected streaming session is usable. */
internal class StreamingSearchController<T>(
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val emptyResults: T,
    private val search: suspend (String, (T) -> Unit) -> T,
    private val onResults: (T) -> Unit,
    private val onLoading: (Boolean) -> Unit,
    private val onError: (Exception) -> Unit,
    private val debounceMillis: Long = 300L
) {
    private var job: Job? = null
    private var generation = 0L

    fun submit(query: String) {
        val request = ++generation
        job?.cancel()
        onLoading(false)
        onResults(emptyResults)
        if (query.isBlank() || !isEnabled()) {
            return
        }
        job = scope.launch {
            delay(debounceMillis)
            if (!isEnabled()) return@launch
            onLoading(true)
            try {
                val requestJob = currentCoroutineContext()[Job]
                val results = search(query) { partial ->
                    if (requestJob?.isActive == true && request == generation && isEnabled()) onResults(partial)
                }
                currentCoroutineContext().ensureActive()
                if (request == generation && isEnabled()) onResults(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation && isEnabled()) onError(e)
            } finally {
                if (request == generation) onLoading(false)
            }
        }
    }
}

/** Publish each finished category without waiting for a slow or unsupported provider category. */
internal suspend fun <T> searchStreamingSections(
    initial: T,
    sections: List<suspend () -> (T) -> T>,
    publish: (T) -> Unit,
): T = coroutineScope {
    var results = initial
    val updates = Mutex()
    val failures = sections.map { section ->
        async {
            try {
                val update = section()
                updates.withLock {
                    results = update(results)
                    publish(results)
                }
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failure
            }
        }
    }.awaitAll().filterNotNull()
    // Preserve already published results while reporting that part of the search failed.
    failures.firstOrNull()?.let { throw it }
    results
}
