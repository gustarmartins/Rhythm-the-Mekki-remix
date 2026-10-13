package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class NightlyVersionTest {
    @Test fun exactAndroidCodeSurvivesTagAndFlavorSuffix() {
        assertEquals(600004201, VersionComparator.explicitVersionCode("v5.5.494.1300-nightly-r4201-abcdef12-vc600004201"))
        assertEquals(600004201, VersionComparator.explicitVersionCode("5.5.494.1300-nightly-r4201-abcdef12-vc600004201-gh"))
        assertNull(VersionComparator.explicitVersionCode("5.5.494.1294-metadata-identity-gh"))
    }
    @Test fun publishedNightlyBeatsInstalledMetadataBuildAndEarlierAttempt() {
        val first = "5.5.494.1300-nightly-r4201-abcdef12-vc600004201"
        val retry = "5.5.494.1300-nightly-r4202-abcdef12-vc600004202"
        assertTrue(VersionComparator.isNewer(first, "5.5.494.1294-metadata-identity-gh"))
        assertTrue(VersionComparator.isNewer(retry, first + "-gh"))
        assertFalse(VersionComparator.isNewer(first, first + "-gh"))
    }
}
