package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class ArtworkNetworkRulesTest {
    private val wifi = ArtworkConnection(internet = true, wifiOrEthernet = true, metered = false)
    private val lte = ArtworkConnection(internet = true, cellular = true)

    @Test fun pendingOnboardingChoiceBlocksAllAutomaticArtwork() {
        assertFalse(allowsArtworkDownload(wifi, false, false))
        assertFalse(allowsArtworkDownload(lte, true, false))
    }
    @Test fun wifiOnlyDoesNotLeakToMeteredWifiCellularOrUnknownVpn() {
        assertTrue(allowsArtworkDownload(wifi, false, true))
        assertFalse(allowsArtworkDownload(wifi.copy(metered = true), false, true))
        assertFalse(allowsArtworkDownload(lte, false, true))
        assertFalse(allowsArtworkDownload(lte.copy(metered = false), false, true))
        assertFalse(allowsArtworkDownload(ArtworkConnection(internet = true, metered = false), false, true))
    }
    @Test fun explicitMobileConsentAllowsCellularButDoesNotInventConnectivity() {
        assertTrue(allowsArtworkDownload(lte, true, true))
        assertFalse(allowsArtworkDownload(ArtworkConnection(), true, true))
    }
    @Test fun routeSwitchStopsThenResumesWithoutChangingTheSavedChoice() {
        val observations = listOf(wifi, lte, ArtworkConnection(), wifi).map {
            allowsArtworkDownload(it, allowMobileData = false, setupCompleted = true)
        }
        assertEquals(listOf(true, false, false, true), observations)
    }
}
