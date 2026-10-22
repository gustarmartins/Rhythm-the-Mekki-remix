package chromahub.rhythm.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Main-thread owner of artwork work. Completed requests may be explicitly refreshed. */
internal class ArtworkColorRequests(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> String?,
    private val publish: (String?) -> Unit,
) {
    private var requestedKey: String? = null
    private var request: Job? = null

    fun submit(key: String?) {
        if (key != null && key == requestedKey && request?.isActive == true) return
        cancel()
        requestedKey = key
        if (key == null) {
            publish(null)
            return
        }
        request = scope.launch {
            val colors = load(key)
            // Decoders/quantizers can finish after cancellation without suspending.
            currentCoroutineContext().ensureActive()
            if (requestedKey == key && colors != null) publish(colors)
        }
    }

    fun cancel() {
        request?.cancel()
        request = null
        requestedKey = null
    }
}
