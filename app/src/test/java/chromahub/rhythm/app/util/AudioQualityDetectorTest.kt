/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioQualityDetectorTest {

    @Test
    fun testDolbyAtmosLossyEac3Joc() {
        // EAC3-JOC 5.1 surround at 768 kbps, 48 kHz (typical Apple Music / Tidal lossy Atmos stream)
        val quality = AudioQualityDetector.detectQuality(
            codec = "EAC3-JOC",
            sampleRateHz = 48000,
            bitrateKbps = 768,
            bitDepth = 16,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_ATMOS, quality.qualityType)
        assertEquals("Dolby Atmos", quality.qualityLabel)
        assertEquals("768 kbps / 48 kHz Spatial Audio", quality.qualityDescription)
        assertFalse("Lossy EAC3-JOC must not be marked lossless", quality.isLossless)
        assertFalse("Lossy EAC3-JOC must not be marked Hi-Res", quality.isHiRes)
        assertTrue("Must be identified as Dolby", quality.isDolby)
        assertFalse("Must not be DTS", quality.isDTS)
        assertEquals("Spatial Audio", quality.category)
        assertEquals("Dolby Atmos", AudioQualityDetector.getQualityBadge(quality))
    }

    @Test
    fun testDolbyAtmosLossyVariants() {
        // Various naming conventions for lossy Dolby Atmos
        val variants = listOf("E-AC-3-JOC", "EC-3-JOC", "EC3-JOC", "EAC3_JOC", "JOC", "ATMOS", "AC-4 IMS")
        for (variant in variants) {
            val quality = AudioQualityDetector.detectQuality(
                codec = variant,
                sampleRateHz = 48000,
                bitrateKbps = 768,
                bitDepth = 16,
                channelCount = 6
            )
            assertEquals("Failed for variant $variant", AudioQualityDetector.QualityType.DOLBY_ATMOS, quality.qualityType)
            assertEquals("Failed for variant $variant", "Dolby Atmos", quality.qualityLabel)
            assertFalse("Variant $variant must be lossy", quality.isLossless)
            assertTrue("Variant $variant must be Dolby", quality.isDolby)
            assertEquals("Spatial Audio", quality.category)
        }
    }

    @Test
    fun testDolbyAtmosTrueHDLossless() {
        // TrueHD Atmos is lossless spatial audio
        val quality = AudioQualityDetector.detectQuality(
            codec = "TrueHD Atmos",
            sampleRateHz = 48000,
            bitrateKbps = 4800,
            bitDepth = 24,
            channelCount = 8
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_ATMOS, quality.qualityType)
        assertEquals("Dolby Atmos", quality.qualityLabel)
        assertTrue("TrueHD Atmos must be lossless", quality.isLossless)
        assertTrue("TrueHD Atmos must be Hi-Res", quality.isHiRes)
        assertTrue(quality.isDolby)
        assertEquals("Lossless Spatial Audio", quality.category)
        assertEquals("24-bit / 48 kHz Lossless Spatial Audio", quality.qualityDescription)
    }

    @Test
    fun testDolbyTrueHDNonAtmos() {
        // TrueHD without Atmos
        val quality = AudioQualityDetector.detectQuality(
            codec = "TrueHD",
            sampleRateHz = 96000,
            bitrateKbps = 3500,
            bitDepth = 24,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_LOSSLESS, quality.qualityType)
        assertEquals("Dolby TrueHD", quality.qualityLabel)
        assertTrue("TrueHD must be lossless", quality.isLossless)
        assertTrue(quality.isHiRes)
        assertTrue(quality.isDolby)
        assertEquals("Lossless Surround", quality.category)
        assertEquals("Dolby TrueHD", AudioQualityDetector.getQualityBadge(quality))
    }

    @Test
    fun testDolbyDigitalPlusNonAtmos() {
        // E-AC-3 standard surround without Atmos
        val quality = AudioQualityDetector.detectQuality(
            codec = "E-AC-3",
            sampleRateHz = 48000,
            bitrateKbps = 640,
            bitDepth = 16,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_LOSSY_SURROUND, quality.qualityType)
        assertEquals("Dolby Digital Plus", quality.qualityLabel)
        assertFalse(quality.isLossless)
        assertTrue(quality.isDolby)
        assertEquals("Dolby Audio", quality.category)
        assertEquals("Dolby Digital Plus", AudioQualityDetector.getQualityBadge(quality))
    }

    @Test
    fun testDolbyDigitalAC3() {
        // AC-3 standard 5.1
        val quality = AudioQualityDetector.detectQuality(
            codec = "AC-3",
            sampleRateHz = 48000,
            bitrateKbps = 448,
            bitDepth = 16,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_LOSSY_SURROUND, quality.qualityType)
        assertEquals("Dolby Digital", quality.qualityLabel)
        assertFalse(quality.isLossless)
        assertTrue(quality.isDolby)
        assertEquals("Dolby Audio", quality.category)
        assertEquals("Dolby Digital", AudioQualityDetector.getQualityBadge(quality))
    }

    @Test
    fun testDolbyAC4NonAtmos() {
        // AC-4 stereo/surround without Atmos
        val quality = AudioQualityDetector.detectQuality(
            codec = "AC-4",
            sampleRateHz = 48000,
            bitrateKbps = 256,
            bitDepth = 16,
            channelCount = 2
        )

        assertEquals(AudioQualityDetector.QualityType.DOLBY_LOSSY_SURROUND, quality.qualityType)
        assertEquals("Dolby AC-4", quality.qualityLabel)
        assertFalse(quality.isLossless)
        assertTrue(quality.isDolby)
        assertEquals("Dolby Audio", quality.category)
    }

    @Test
    fun testMpegH3DAudio() {
        // MPEG-H 3D Audio
        val quality = AudioQualityDetector.detectQuality(
            codec = "MPEG-H",
            sampleRateHz = 48000,
            bitrateKbps = 512,
            bitDepth = 16,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.MPEG_H_3D, quality.qualityType)
        assertEquals("MPEG-H 3D Audio", quality.qualityLabel)
        assertFalse(quality.isLossless)
        assertFalse(quality.isDolby)
        assertEquals("Spatial Audio", quality.category)
        assertEquals("MPEG-H 3D", AudioQualityDetector.getQualityBadge(quality))
    }

    @Test
    fun testDtsVariants() {
        // DTS standard lossy
        val dts = AudioQualityDetector.detectQuality(
            codec = "DTS",
            sampleRateHz = 48000,
            bitrateKbps = 1509,
            bitDepth = 16,
            channelCount = 6
        )
        assertEquals(AudioQualityDetector.QualityType.DTS_SURROUND, dts.qualityType)
        assertEquals("DTS", dts.qualityLabel)
        assertFalse(dts.isLossless)
        assertTrue(dts.isDTS)
        assertFalse(dts.isDolby)

        // DTS:X
        val dtsx = AudioQualityDetector.detectQuality(
            codec = "DTS:X",
            sampleRateHz = 48000,
            bitrateKbps = 1500,
            bitDepth = 16,
            channelCount = 8
        )
        assertEquals(AudioQualityDetector.QualityType.DTS_SURROUND, dtsx.qualityType)
        assertEquals("DTS:X", dtsx.qualityLabel)
        assertFalse(dtsx.isLossless)
        assertTrue(dtsx.isDTS)

        // DTS-HD Master Audio (Lossless)
        val dtshd = AudioQualityDetector.detectQuality(
            codec = "DTS-HD MA",
            sampleRateHz = 96000,
            bitrateKbps = 4500,
            bitDepth = 24,
            channelCount = 8
        )
        assertEquals(AudioQualityDetector.QualityType.DTS_SURROUND, dtshd.qualityType)
        assertEquals("DTS-HD Master Audio", dtshd.qualityLabel)
        assertTrue(dtshd.isLossless)
        assertTrue(dtshd.isHiRes)
        assertTrue(dtshd.isDTS)
        assertEquals("Lossless Surround", dtshd.category)
    }

    @Test
    fun testMultichannelLosslessNotMislabeledAsDolby() {
        // 5.1 FLAC track must be labeled Multichannel Lossless, NEVER Dolby Surround
        val flac51 = AudioQualityDetector.detectQuality(
            codec = "FLAC",
            sampleRateHz = 96000,
            bitrateKbps = 4500,
            bitDepth = 24,
            channelCount = 6
        )

        assertEquals(AudioQualityDetector.QualityType.LOSSLESS_SURROUND, flac51.qualityType)
        assertEquals("Multichannel Lossless 5.1", flac51.qualityLabel)
        assertTrue(flac51.isLossless)
        assertFalse("FLAC 5.1 must not be marked Dolby", flac51.isDolby)
        assertFalse("FLAC 5.1 must not be marked DTS", flac51.isDTS)
        assertEquals("Lossless Surround", flac51.category)

        // 7.1 WAV track
        val wav71 = AudioQualityDetector.detectQuality(
            codec = "WAV",
            sampleRateHz = 48000,
            bitrateKbps = 6144,
            bitDepth = 24,
            channelCount = 8
        )
        assertEquals(AudioQualityDetector.QualityType.LOSSLESS_SURROUND, wav71.qualityType)
        assertEquals("Multichannel Lossless 7.1", wav71.qualityLabel)
        assertFalse(wav71.isDolby)
    }

    @Test
    fun testStudioMasterAndHiRes() {
        // 24-bit 192 kHz FLAC -> Studio Master
        val master = AudioQualityDetector.detectQuality(
            codec = "FLAC",
            sampleRateHz = 192000,
            bitrateKbps = 6000,
            bitDepth = 24,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.HI_RES_STUDIO_MASTER, master.qualityType)
        assertEquals("Studio Master", master.qualityLabel)
        assertTrue(master.isLossless)
        assertTrue(master.isHiRes)

        // 24-bit 96 kHz ALAC -> Hi-Res Lossless
        val hiRes = AudioQualityDetector.detectQuality(
            codec = "ALAC",
            sampleRateHz = 96000,
            bitrateKbps = 2800,
            bitDepth = 24,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.HI_RES_LOSSLESS, hiRes.qualityType)
        assertEquals("Hi-Res Lossless", hiRes.qualityLabel)
        assertTrue(hiRes.isLossless)
        assertTrue(hiRes.isHiRes)
    }

    @Test
    fun testCdQualityLossless() {
        // 16-bit 44.1 kHz FLAC -> CD Quality
        val cdQuality = AudioQualityDetector.detectQuality(
            codec = "FLAC",
            sampleRateHz = 44100,
            bitrateKbps = 850,
            bitDepth = 16,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.CD_QUALITY_LOSSLESS, cdQuality.qualityType)
        assertEquals("Lossless", cdQuality.qualityLabel)
        assertTrue(cdQuality.isLossless)
        assertFalse(cdQuality.isHiRes)
    }

    @Test
    fun testOtherLosslessCodecs() {
        // WavPack (WV)
        val wavpack = AudioQualityDetector.detectQuality(
            codec = "WV",
            sampleRateHz = 44100,
            bitrateKbps = 750,
            bitDepth = 16,
            channelCount = 2
        )
        assertTrue("WavPack must be lossless", wavpack.isLossless)

        // TAK
        val tak = AudioQualityDetector.detectQuality(
            codec = "TAK",
            sampleRateHz = 44100,
            bitrateKbps = 720,
            bitDepth = 16,
            channelCount = 2
        )
        assertTrue("TAK must be lossless", tak.isLossless)

        // TTA
        val tta = AudioQualityDetector.detectQuality(
            codec = "TTA",
            sampleRateHz = 44100,
            bitrateKbps = 800,
            bitDepth = 16,
            channelCount = 2
        )
        assertTrue("TTA must be lossless", tta.isLossless)
    }

    @Test
    fun testTraditionalLossyAudio() {
        // MP3 320 kbps
        val mp3 = AudioQualityDetector.detectQuality(
            codec = "MP3",
            sampleRateHz = 44100,
            bitrateKbps = 320,
            bitDepth = 16,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.LOSSY_COMPRESSED, mp3.qualityType)
        assertEquals("320 kbps", mp3.qualityLabel)
        assertFalse(mp3.isLossless)
        assertFalse(mp3.isDolby)
        assertEquals("Lossy", mp3.category)
        assertEquals("320kbps", AudioQualityDetector.getQualityBadge(mp3))

        // AAC 256 kbps
        val aac = AudioQualityDetector.detectQuality(
            codec = "AAC",
            sampleRateHz = 44100,
            bitrateKbps = 256,
            bitDepth = 16,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.LOSSY_COMPRESSED, aac.qualityType)
        assertEquals("256 kbps", aac.qualityLabel)
        assertFalse(aac.isLossless)
        assertFalse(aac.isDolby)

        // Opus 128 kbps
        val opus = AudioQualityDetector.detectQuality(
            codec = "Opus",
            sampleRateHz = 48000,
            bitrateKbps = 128,
            bitDepth = 16,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.LOSSY_COMPRESSED, opus.qualityType)
        assertEquals("128 kbps", opus.qualityLabel)
        assertFalse(opus.isLossless)
        assertFalse(opus.isHiRes)
    }

    @Test
    fun testDsdAudiophile() {
        val dsd = AudioQualityDetector.detectQuality(
            codec = "DSD",
            sampleRateHz = 2822400,
            bitrateKbps = 5645,
            bitDepth = 1,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.DSD_HIGH_RES, dsd.qualityType)
        assertEquals("DSD64", dsd.qualityLabel)
        assertTrue(dsd.isLossless)
        assertTrue(dsd.isHiRes)
        assertEquals(1, dsd.bitDepthEstimate)
        assertEquals("Hi-Res Master", dsd.category)
    }

    @Test
    fun testSuspiciousLosslessBitDepthDowngradedToLossy() {
        // If a file claims to be FLAC but bit depth is calculated/reported < 12
        val suspicious = AudioQualityDetector.detectQuality(
            codec = "FLAC",
            sampleRateHz = 44100,
            bitrateKbps = 64, // impossible for real FLAC
            bitDepth = 8,
            channelCount = 2
        )
        assertEquals(AudioQualityDetector.QualityType.LOSSY_COMPRESSED, suspicious.qualityType)
        assertEquals("Lossy", suspicious.qualityLabel)
        assertFalse(suspicious.isLossless)
    }
}
