package chromahub.rhythm.app.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/** Keep cancellation attached until the body is consumed, including a stalled body read. */
internal suspend fun Call.awaitLyricsBody(): String? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWith(Result.failure(e))
        }

        override fun onResponse(call: Call, response: Response) {
            val result = runCatching {
                response.use {
                    if (continuation.isActive && it.isSuccessful) it.body.string() else null
                }
            }
            continuation.resumeWith(result)
        }
    })
}
