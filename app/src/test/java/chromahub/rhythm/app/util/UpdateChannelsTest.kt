package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateChannelsTest {
    @Test fun onlyPublishedStableAndNightlyChannelsAreAvailable() {
        assertEquals(listOf("stable", "nightly"), UpdateChannels.available)
    }

    @Test fun legacyBetaAndUnknownValuesUseStableWithoutOptingIntoNightly() {
        assertEquals("stable", UpdateChannels.normalize("beta", isNightlyBuild = true))
        assertEquals("stable", UpdateChannels.normalize("experimental"))
    }

    @Test fun cleanInstallsUseTheirBuildChannelAndExplicitChoicesSurvive() {
        assertEquals("nightly", UpdateChannels.normalize(null, isNightlyBuild = true))
        assertEquals("stable", UpdateChannels.normalize(null))
        assertEquals("stable", UpdateChannels.normalize("stable", isNightlyBuild = true))
        assertEquals("nightly", UpdateChannels.normalize("nightly"))
    }
}
