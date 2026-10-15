/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.presentation.components.bottomsheets

import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerDiscoveryPolicyTest {
    @Test fun musicAssistantDiscoversItsNativeMdnsTypeAndHttpFallback() {
        assertEquals(
            listOf("_mass._tcp", "_http._tcp"),
            ServerDiscoveryPolicy.serviceTypes(StreamingServiceId.MUSIC_ASSISTANT)
        )
    }

    @Test fun aNativeMusicAssistantAdvertisementDoesNotNeedABrandNameOrResolvedPort() {
        // A named home/RPi server has no port until NSD resolution completes.
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_mass._tcp", "Living room"))
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_mass._tcp", ""))
    }

    @Test fun httpFallbackRecognizesMusicAssistantNamesRegardlessOfCase() {
        listOf("Music Assistant on RPi", "MUSIC-ASSISTANT", "MASS").forEach { name ->
            assertTrue(name, ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_http._tcp", name))
        }
    }

    @Test fun musicAssistantDoesNotOfferUnrelatedHttpOrOtherMusicServers() {
        listOf("Server", "Music library", "Jellyfin", "Navidrome", "Mass storage").forEach { name ->
            assertFalse(name, ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_http._tcp", name))
        }
        assertFalse(ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_jellyfin._tcp", "Music Assistant"))
        assertFalse(ServerDiscoveryPolicy.matches(StreamingServiceId.MUSIC_ASSISTANT, "_subsonic._tcp", "Music Assistant"))
    }

    @Test fun existingProvidersRetainTheirNativeDiscoveryAndHttpCompatibility() {
        assertEquals(listOf("_jellyfin._tcp", "_http._tcp"), ServerDiscoveryPolicy.serviceTypes(StreamingServiceId.JELLYFIN))
        assertEquals(listOf("_subsonic._tcp", "_http._tcp"), ServerDiscoveryPolicy.serviceTypes(StreamingServiceId.SUBSONIC))
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.JELLYFIN, "_jellyfin._tcp", "RPi"))
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.SUBSONIC, "_subsonic._tcp", "RPi"))
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.JELLYFIN, "_http._tcp", "Emby media server"))
        assertTrue(ServerDiscoveryPolicy.matches(StreamingServiceId.SUBSONIC, "_http._tcp", "Navidrome"))
        assertFalse(ServerDiscoveryPolicy.matches(StreamingServiceId.SUBSONIC, "_mass._tcp", "RPi"))
    }

    @Test fun anUnknownProviderDoesNotStartDuplicateHttpDiscoveriesOrOfferEveryHost() {
        assertEquals(listOf("_http._tcp"), ServerDiscoveryPolicy.serviceTypes("UNKNOWN"))
        assertFalse(ServerDiscoveryPolicy.matches("UNKNOWN", "_http._tcp", "Music Assistant"))
    }
}
