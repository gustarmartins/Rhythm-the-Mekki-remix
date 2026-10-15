/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

/** OkHttp needs engine-level pings; the Ktor plugin interval does not enable them. */
internal fun musicAssistantSocketClient(pingIntervalMillis: Long = 5_000): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            pingInterval(pingIntervalMillis, TimeUnit.MILLISECONDS)
            connectTimeout(10, TimeUnit.SECONDS)
        }
    }
    install(WebSockets)
}
