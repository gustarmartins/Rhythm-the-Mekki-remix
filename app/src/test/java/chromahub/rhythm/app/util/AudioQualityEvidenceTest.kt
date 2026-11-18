/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class AudioQualityEvidenceTest {
    @Test fun lossyAtmosNeverBecomesLosslessFromBitrateOrBitDepth() {
        val result = AudioQualityDetector.detectQuality("EAC3-JOC", 48000, 4000, 24, 8)
        assertEquals(AudioQualityDetector.QualityType.DOLBY_ATMOS, result.qualityType)
        assertFalse(result.isLossless)
    }
    @Test fun trueHdAtmosRetainsItsExplicitLosslessCodecEvidence() {
        val result = AudioQualityDetector.detectQuality("TrueHD Atmos", 48000, 4000, 24, 8)
        assertTrue(result.isLossless)
        assertEquals(AudioQualityDetector.QualityType.DOLBY_ATMOS, result.qualityType)
    }
    @Test fun ordinaryMultichannelFlacIsNotRenamedDolby() {
        val result = AudioQualityDetector.detectQuality("FLAC", 96000, 4000, 24, 6)
        assertFalse(result.isDolby)
        assertEquals(AudioQualityDetector.QualityType.LOSSLESS_SURROUND, result.qualityType)
    }
}
