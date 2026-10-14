package chromahub.rhythm.app.features.streaming.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

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
    private val search: suspend (String) -> T,
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
        if (query.isBlank() || !isEnabled()) {
            onResults(emptyResults)
            return
        }
        job = scope.launch {
            delay(debounceMillis)
            if (!isEnabled()) return@launch
            onLoading(true)
            try {
                val results = search(query)
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
