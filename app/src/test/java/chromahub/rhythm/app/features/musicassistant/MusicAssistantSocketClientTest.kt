/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MusicAssistantSocketClientTest {
    @Test fun silentSocketIsDetectedAndAFreshConnectionCanOpen() = runBlocking {
        val server = ServerSocket(0)
        val firstFinished = CountDownLatch(1)
        val secondFinished = CountDownLatch(1)
        val peer = thread(isDaemon = true) {
            repeat(2) { attempt ->
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
                    val headers = generateSequence { reader.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                    val key = headers.first { it.startsWith("Sec-WebSocket-Key:", ignoreCase = true) }
                        .substringAfter(':').trim()
                    val accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                        .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.US_ASCII)))
                    socket.getOutputStream().apply {
                        write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\n" +
                            "Connection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n").toByteArray(Charsets.US_ASCII))
                        flush()
                    }
                    // TCP stays open but this peer never answers WebSocket pings.
                    // This reproduces a VPN route disappearing without a clean close.
                    (if (attempt == 0) firstFinished else secondFinished).await(10, TimeUnit.SECONDS)
                }
            }
        }
        val client = musicAssistantSocketClient(pingIntervalMillis = 100)
        try {
            withTimeout(5000) {
                val first = client.webSocketSession(urlString = "ws://127.0.0.1:${server.localPort}/")
                assertTrue("A silent peer must not leave playback buffering forever", first.incoming.receiveCatching().isClosed)
                firstFinished.countDown()
                val second = client.webSocketSession(urlString = "ws://127.0.0.1:${server.localPort}/")
                assertNotSame(first, second)
            }
        } finally {
            firstFinished.countDown()
            secondFinished.countDown()
            client.close()
            server.close()
            peer.join(2000)
        }
    }
}
