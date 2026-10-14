/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import org.junit.Assert.*
import org.junit.Test

class MusicAssistantServerTest {
    @Test fun acceptsLanAndTailscaleAddresses() {
        assertEquals("http://raspberrypi:8095/", MusicAssistantServer.normalize(" raspberrypi "))
        assertEquals("http://100.100.20.30:8095/", MusicAssistantServer.normalize("100.100.20.30"))
        assertEquals("http://pi.example.ts.net:8095/", MusicAssistantServer.normalize("pi.example.ts.net:8095"))
        assertEquals("http://[fd7a:115c:a1e0::1]:8095/", MusicAssistantServer.normalize("[fd7a:115c:a1e0::1]"))
        assertEquals("http://[::1]:9000/", MusicAssistantServer.normalize("[::1]:9000"))
    }

    @Test fun preservesExplicitPortsHttpsAndProxyPaths() {
        assertEquals("https://music.example.org/", MusicAssistantServer.normalize("https://music.example.org"))
        assertEquals("http://pi/", MusicAssistantServer.normalize("http://pi"))
        assertEquals("https://music.example.org:8443/music/", MusicAssistantServer.normalize("https://music.example.org:8443/music/"))
        assertEquals("http://pi:8095/music/", MusicAssistantServer.normalize("pi/music"))
    }

    @Test fun neverPersistsCredentialsOrOAuthReturnTokensInServerAddress() {
        listOf("", "https://user:secret@pi/", "http://user@pi/", "http://pi/?token=secret", "https://pi/#access_token=secret",
            "javascript:alert(1)", "file:///sdcard/music", "content://settings", "intent://pi", "ftp://pi",
            "pi:65536", "pi:bad", "http://pi\\@evil.test", "http://bad host/", "http://pi/path with spaces"
        ).forEach { assertNull(it, MusicAssistantServer.normalize(it)) }
    }

    @Test fun originsMatchExactSchemeHostAndPortNotTextPrefixes() {
        val server = "https://ma.example:8443/music/"
        assertTrue(MusicAssistantServer.sameOrigin(server, "https://ma.example:8443/login?return_url=%2F"))
        assertTrue(MusicAssistantServer.sameOrigin(server, "https://MA.EXAMPLE:8443/music/#/library"))
        listOf("https://ma.example.evil.test:8443/", "https://ma.example:8443@evil.test/", "http://ma.example:8443/",
            "https://ma.example/", "file:///music/", "javascript:alert(1)", "https://user:pass@ma.example:8443/"
        ).forEach { assertFalse(it, MusicAssistantServer.sameOrigin(server, it)) }
        assertTrue(MusicAssistantServer.sameOrigin("https://ma.example/", "https://ma.example:443/login"))
    }

    @Test fun externalNavigationCannotLaunchArbitraryAndroidIntents() {
        assertTrue(MusicAssistantServer.externalLink("https://www.music-assistant.io/"))
        listOf("intent://launch#Intent;end", "tel:123", "market://app", "file:///secret", "data:text/html,x", "javascript:alert(1)",
            "https://user:secret@example.org/").forEach { assertFalse(it, MusicAssistantServer.externalLink(it)) }
    }

    @Test fun stoppingAndLeavingReleasePlayerUnlessExplicitlyKeptAlive() {
        val lifetime = MusicAssistantLifetime()
        assertFalse(lifetime.backgroundEnabled)
        assertTrue(lifetime.shouldReleaseWhenHidden(changingConfigurations = false))
        assertFalse(lifetime.shouldReleaseWhenHidden(changingConfigurations = true))
        lifetime.enableBackground()
        assertFalse(lifetime.shouldReleaseWhenHidden(changingConfigurations = false))
        lifetime.stop()
        assertTrue(lifetime.shouldReleaseWhenHidden(changingConfigurations = false))
        assertFalse(MusicAssistantLifetime().backgroundEnabled) // No process-death/reboot auto-resume.
    }
}
