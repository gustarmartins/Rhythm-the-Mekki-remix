package chromahub.rhythm.app.network

import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class LyricsHttpTest {
    @Test fun successfulBodyAndHttpFailureAreHandled() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().body("response text").build())
            server.enqueue(MockResponse.Builder().code(404).body("missing").build())
            server.start()
            val client = OkHttpClient()
            assertEquals("response text", client.newCall(Request.Builder().url(server.url("/ok")).build()).awaitLyricsBody())
            assertNull(client.newCall(Request.Builder().url(server.url("/missing")).build()).awaitLyricsBody())
        }
    }

    @Test fun cancellingWhileBodyIsStalledCancelsTheUnderlyingCall() = runBlocking {
        withTimeout(5_000) {
            MockWebServer().use { server ->
                server.enqueue(MockResponse.Builder().body("delayed body")
                    .bodyDelay(30, TimeUnit.SECONDS).build())
                server.start()
                val headersReceived = CompletableDeferred<Unit>()
                val client = OkHttpClient.Builder().eventListener(object : okhttp3.EventListener() {
                    override fun responseHeadersEnd(call: okhttp3.Call, response: okhttp3.Response) {
                        headersReceived.complete(Unit)
                    }
                }).build()
                val call = client.newCall(Request.Builder().url(server.url("/slow")).build())
                val lookup = async { call.awaitLyricsBody() }
                headersReceived.await()
                lookup.cancelAndJoin()
                assertTrue(call.isCanceled())
            }
        }
    }
}
