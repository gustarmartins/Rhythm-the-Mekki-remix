/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun forkStableUpgradesInstalledFastLyricsNightly() {
        val stable = "v5.5.480.1260-bluetooth-lyrics-stable"
        val installed = "5.4.443.1174-nightly-fast-lyrics-20260829-r2-gh"
        assertTrue(VersionComparator.isNewer(stable, installed))
        assertFalse(VersionComparator.isNewer(installed, stable))
        assertEquals(ChannelTier.STABLE, VersionComparator.parse(stable).tier)
        assertEquals(0, VersionComparator.compare(stable, "5.5.480.1260-bluetooth-lyrics-gh"))
    }

    @Test
    fun testNumericVersionPrecedence() {
        // Higher major
        assertTrue(VersionComparator.isNewer("6.0.0.0", "5.4.465.1226"))
        // Higher minor
        assertTrue(VersionComparator.isNewer("5.5.0.0", "5.4.465.1226"))
        // Higher patch
        assertTrue(VersionComparator.isNewer("5.4.466.0", "5.4.465.1226"))
        // Higher subpatch/build
        assertTrue(VersionComparator.isNewer("5.4.465.1227", "5.4.465.1226"))

        // Older version is not newer
        assertFalse(VersionComparator.isNewer("5.4.462.1216", "5.4.465.1226"))
    }

    @Test
    fun testChannelTierPrecedenceForEqualBaseVersion() {
        val baseStable = "5.4.465.1226"
        val baseBeta = "5.4.465.1226-beta"
        val baseNightly = "5.4.465.1226-nightly-r100"

        // Stable > Beta
        assertTrue(VersionComparator.isNewer(baseStable, baseBeta))
        assertFalse(VersionComparator.isNewer(baseBeta, baseStable))

        // Beta > Nightly
        assertTrue(VersionComparator.isNewer(baseBeta, baseNightly))
        assertFalse(VersionComparator.isNewer(baseNightly, baseBeta))

        // Stable > Nightly
        assertTrue(VersionComparator.isNewer(baseStable, baseNightly))
        assertFalse(VersionComparator.isNewer(baseNightly, baseStable))
    }

    @Test
    fun testHigherVersionBeatsStabilityTier() {
        // A higher version nightly is newer than an older stable
        assertTrue(VersionComparator.isNewer("5.5.0.0-nightly-r10", "5.4.465.1226"))

        // A higher version beta is newer than an older stable
        assertTrue(VersionComparator.isNewer("5.4.466.1228-beta", "5.4.465.1226"))

        // An older stable is NOT newer than a higher version nightly
        assertFalse(VersionComparator.isNewer("5.4.462.1216", "5.4.465.1225-nightly-r1343-6d4fa27"))
    }

    @Test
    fun testNightlyRunComparison() {
        val n1 = "5.4.465.1225-nightly-r1343-6d4fa27"
        val n2 = "5.4.465.1225-nightly-r1344-45b7845"

        assertTrue(VersionComparator.isNewer(n2, n1))
        assertFalse(VersionComparator.isNewer(n1, n2))
    }

    @Test
    fun testRealGitTagsParsing() {
        val stableTag = "v5.4.462.1216-stable"
        val betaTag = "v5.4.457.1200-beta"
        val olderStableTag = "v5.3.440.1160-stable"

        assertTrue(VersionComparator.isNewer(stableTag, betaTag))
        assertTrue(VersionComparator.isNewer(betaTag, olderStableTag))
        assertTrue(VersionComparator.isNewer(stableTag, olderStableTag))
    }

    @Test
    fun testCrossChannelUpgrades() {
        // User on 5.4.465 Nightly upgrading to 5.4.465 Beta -> valid
        assertTrue(VersionComparator.isNewer("5.4.465.1225-beta", "5.4.465.1225-nightly-r1343-6d4fa27"))

        // User on 5.4.465 Nightly upgrading to 5.4.465 Stable -> valid
        assertTrue(VersionComparator.isNewer("5.4.465.1226", "5.4.465.1225-nightly-r1343-6d4fa27"))

        // User on 5.4.465 Beta upgrading to 5.4.465 Stable -> valid
        assertTrue(VersionComparator.isNewer("5.4.465.1226", "5.4.465.1225-beta"))

        // User on 5.4.465 Stable should NOT be offered 5.4.465 Beta or Nightly
        assertFalse(VersionComparator.isNewer("5.4.465.1225-beta", "5.4.465.1226"))
        assertFalse(VersionComparator.isNewer("5.4.465.1225-nightly-r1343-6d4fa27", "5.4.465.1226"))
    }

    @Test
    fun testGitHubPreReleaseFlag() {
        // When GitHub release has prerelease=true even if tag name doesn't contain "beta"
        val releaseTag = "v5.4.465.1226"
        val stableVersion = VersionComparator.parse(releaseTag, isPreRelease = false)
        val betaVersion = VersionComparator.parse(releaseTag, isPreRelease = true)

        assertEquals(ChannelTier.STABLE, stableVersion.tier)
        assertEquals(ChannelTier.BETA, betaVersion.tier)

        // Stable > Beta for same version string
        assertTrue(VersionComparator.isNewer(releaseTag, releaseTag, isCandidatePreRelease = false, isCurrentPreRelease = true))
        assertFalse(VersionComparator.isNewer(releaseTag, releaseTag, isCandidatePreRelease = true, isCurrentPreRelease = false))
    }
}
