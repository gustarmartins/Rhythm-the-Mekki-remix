/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Shared server-address validation for native and isolated web sessions. */
object MusicAssistantServer {
    fun normalize(input: String): String? {
        val value = input.trim()
        if (value.isEmpty() || value.any { it.isWhitespace() || it == '\\' }) return null
        val explicitScheme = value.contains("://")
        if (explicitScheme && !value.startsWith("http://", true) && !value.startsWith("https://", true)) return null
        val url = (if (explicitScheme) value else "http://$value").toHttpUrlOrNull() ?: return null
        if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) return null
        val authority = value.substringAfter("://").substringBefore('/')
        val explicitPort = if (authority.startsWith('[')) authority.substringAfter(']').startsWith(':')
            else authority.contains(':')
        return url.newBuilder().apply {
            // A bare Pi/Tailscale hostname means the standard MA web port. Explicit URLs are exact.
            if (!explicitScheme && !explicitPort) port(8095)
            if (!url.encodedPath.endsWith('/')) addPathSegment("")
        }.build().toString()
    }

    fun sameOrigin(server: String, target: String): Boolean {
        val base = server.toHttpUrlOrNull() ?: return false
        val url = target.toHttpUrlOrNull() ?: return false
        return url.username.isEmpty() && url.password.isEmpty() && base.origin() == url.origin()
    }

    fun externalLink(target: String): Boolean {
        val url = target.toHttpUrlOrNull() ?: return false
        return url.username.isEmpty() && url.password.isEmpty()
    }

    private fun HttpUrl.origin() = Triple(scheme, host, port)
}

/** Lifecycle decisions kept separate from Android, including rotation and explicit stop. */
internal class MusicAssistantLifetime {
    var backgroundEnabled = false
        private set
    var explicitlyStopped = false
        private set
    fun openPlayer() { explicitlyStopped = false }
    fun stopPlayer() { explicitlyStopped = true; stop() }
    fun enableBackground() { backgroundEnabled = true }
    fun stop() { backgroundEnabled = false }
    fun shouldReleaseWhenHidden(changingConfigurations: Boolean) = !backgroundEnabled && !changingConfigurations
}
