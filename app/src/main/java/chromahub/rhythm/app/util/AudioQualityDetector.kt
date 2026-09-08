/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log

/**
 * Audio quality categorization utility based on sample rate, bitrate, bit depth, and codec
 * 
 * LOSSLESS AUDIO DEFINITION:
 * Lossless audio refers to a file format that preserves all the original audio data from the 
 * source without any degradation, unlike lossy formats such as MP3 or AAC, which remove some 
 * data to reduce file size. Apple Music uses the Apple Lossless Audio Codec (ALAC) for its 
 * lossless tier, which ensures the audio is a bit-perfect copy of the original master recording.
 * 
 * STANDARD LOSSLESS vs HIGH-RESOLUTION LOSSLESS:
 * - Standard Lossless (CD Quality): 16-bit depth, 44.1 kHz sample rate
 *   * CD quality is defined as 16-bit depth and a 44.1 kHz sample rate
 *   * A 16-bit audio file can achieve approximately 96 dB of dynamic range
 *   * 44.1 kHz sample rate can reproduce frequencies up to 22.05 kHz (just above human hearing ~20 kHz)
 *   * Typical bitrate: ~1,411 kbps uncompressed, 200-900 kbps compressed (ALAC/FLAC)
 * 
 * - High-Resolution Lossless: 24-bit depth, 96 kHz or higher sample rate
 *   * Uses higher bit depths and sample rates than standard CD quality
 *   * 24-bit depth provides approximately 144 dB of dynamic range (48 dB more than 16-bit)
 *   * Sample rates of 96 kHz, 192 kHz, or higher capture more audio information
 *   * Higher sample rates can reproduce frequencies beyond human hearing (though audibility is debated)
 *   * Typical bitrate: 2,000-4,600 kbps at 96 kHz, 4,600-9,200+ kbps at 192 kHz
 * 
 * CALCULATING THE DIFFERENCE:
 * The difference between standard lossless and high-resolution lossless is calculated by 
 * comparing the bit depth and sample rate metadata:
 * - Bit depth determines the dynamic range (signal-to-noise ratio)
 * - Sample rate determines the frequency range that can be accurately reproduced
 * - A song with 16-bit/44.1 kHz is standard lossless (CD quality)
 * - A song with 24-bit/96 kHz or 24-bit/192 kHz is high-resolution lossless
 * 
 * Quality Levels (Based on industry standards):
 * - Lossy Compressed: MP3, AAC, OGG (128-320 kbps, 44.1 kHz, 16-bit after decoding)
 * - CD Quality (Lossless): FLAC, ALAC, WAV (~1,411 kbps, 44.1 kHz, 16-bit)
 * - Hi-Res Lossless: FLAC/ALAC (2,000-4,600 kbps, 48-96 kHz, 24-bit)
 * - Studio Master: FLAC/ALAC (4,600-9,200+ kbps, 192 kHz, 24-bit)
 * - Dolby Digital (Lossy): AC-3, E-AC-3 (192-640 kbps, 48 kHz, 16-bit compressed)
 * - Dolby TrueHD/Atmos (Lossless): (4,000-18,000 kbps, 48-192 kHz, 24-bit)
 * - DTS-HD Master Audio (Lossless): (4,000-18,000 kbps, 48-192 kHz, 24-bit)
 */
object AudioQualityDetector {
    private const val TAG = "AudioQualityDetector"

    /**
     * Represents the audio quality tier
     */
    enum class QualityType {
        LOSSY_COMPRESSED,      // MP3, AAC, OGG (128-320 kbps)
        CD_QUALITY_LOSSLESS,   // FLAC, ALAC 16/44.1
        HI_RES_LOSSLESS,       // FLAC, ALAC 24/48+ kHz
        HI_RES_STUDIO_MASTER,  // FLAC, ALAC 24/192 kHz
        LOSSLESS_SURROUND,     // e.g., FLAC 5.1
        DOLBY_LOSSY_SURROUND,  // AC-3, E-AC-3
        DOLBY_LOSSLESS,        // TrueHD, Atmos
        DTS_SURROUND,          // DTS variants
        DSD_HIGH_RES,          // DSD64, DSD128, DSD256, DSD512 (1-bit high-end audiophile)
        UNKNOWN
    }

    /**
     * Detailed audio quality information
     */
    data class AudioQuality(
        val qualityType: QualityType,
        val qualityLabel: String,        // e.g., "Hi-Res Lossless", "CD Quality"
        val qualityDescription: String,  // e.g., "24-bit/96 kHz Studio Master"
        val isLossless: Boolean,
        val isHiRes: Boolean,
        val isDolby: Boolean,
        val isDTS: Boolean,
        val bitDepthEstimate: Int,       // 16 or 24 bit (or 0 if unknown)
        val category: String             // "Lossless", "Lossy", "Surround"
    )

    /**
     * Determine audio quality based on codec, sample rate, bitrate, and bit depth
     * 
     * @param codec Audio codec (e.g., "FLAC", "MP3", "AAC", "AC-3")
     * @param sampleRateHz Sample rate in Hz (e.g., 44100, 48000, 96000)
     * @param bitrateKbps Bitrate in kbps (e.g., 320, 1411, 4608)
     * @param bitDepth Bit depth if known (16, 24, or 0 if unknown)
     * @param channelCount Number of audio channels (1=mono, 2=stereo, 6=5.1, etc.)
     * @return AudioQuality with detailed quality information
     */
    fun detectQuality(
        codec: String,
        sampleRateHz: Int,
        bitrateKbps: Int,
        bitDepth: Int = 0,
        channelCount: Int = 2
    ): AudioQuality {
        val normalizedCodec = codec.uppercase().trim()
        val sampleRateKhz = sampleRateHz / 1000.0

        // CRITICAL: First check if codec is explicitly LOSSY - these can NEVER be lossless
        // regardless of bitrate or bit depth. Lossy codecs discard data during encoding.
        val isLossyCodec = normalizedCodec in listOf("MP3", "AAC", "OGG", "OPUS", "VORBIS", "AC-3", "AC3", "E-AC-3", "EAC3", "AC-4", "AC4", "MP2", "AMR", "AMR-NB", "AMR-WB", "MPEG-H", "MHM1", "MHM") ||
                          (normalizedCodec.contains("WMA") && !normalizedCodec.contains("LOSSLESS"))
        
        // Determine if codec is inherently lossless
        // These codecs preserve all original audio data bit-perfectly
        val isLosslessCodec = !isLossyCodec && (
            normalizedCodec in listOf(
                "FLAC", "ALAC", "WAV", "PCM", "APE", "DSD", "AIFF", "WMA LOSSLESS",
                "TRUEHD", "MLP", "DTS-HD", "DTS-HD MA", "DTS:X", "MIDI",
                "APPLE LOSSLESS", "FLAC LOSSLESS"
            ) || normalizedCodec.contains("LOSSLESS")
        )
        
        // IMPORTANT: Only use bitrate heuristics if codec is UNKNOWN/EMPTY and NOT a known lossy codec
        // High-bitrate AAC (320kbps) is still lossy, not lossless!
        // This heuristic should rarely trigger as most files have codec information
        val isLikelyLosslessFromBitrate = !isLossyCodec &&
                                          !isLosslessCodec &&
                                          (normalizedCodec.isEmpty() || normalizedCodec == "UNKNOWN") &&
                                          sampleRateHz in 44000..48000 && 
                                          bitrateKbps > 900  // Very high threshold to avoid false positives
        
        val finalIsLossless = isLosslessCodec || isLikelyLosslessFromBitrate
        
//        Log.d(TAG, "Codec detection: original='$codec', normalized='$normalizedCodec', " +
//                "isLossyCodec=$isLossyCodec, isLosslessCodec=$isLosslessCodec, " +
//                "likelyLossless=$isLikelyLosslessFromBitrate, final=$finalIsLossless") +
//                "final=$finalIsLossless")

        // Detect Dolby variants
        val isDolbyCodec = normalizedCodec in listOf(
            "AC-3", "AC3", "AC-4", "AC4", "E-AC-3", "EAC3", "DOLBY DIGITAL", "DOLBY DIGITAL PLUS",
            "TRUEHD", "DOLBY TRUEHD", "ATMOS", "DOLBY ATMOS", "MLP"
        )

        // Detect DTS variants
        val isDTSCodec = normalizedCodec.contains("DTS")

        // Estimate bit depth if not provided
        val estimatedBitDepth = when {
            normalizedCodec.contains("DSD") || normalizedCodec == "DSD" -> 1
            bitDepth > 0 -> {
                Log.d(TAG, "Using provided bit depth: $bitDepth-bit")
                bitDepth
            }
            // Use bitrate-based calculation for lossless with improved thresholds
            // This calculation determines bit depth from the bitrate, sample rate, and channels:
            // Formula: bits per sample = (bitrate in bps) / (sample rate * channels)
            // Standard CD quality: 16-bit/44.1kHz/stereo = 1,411 kbps → 16 bits/sample
            //   Calculation: (1,411,000 bps) / (44,100 Hz * 2 channels) = 16.0 bits/sample
            // Hi-Res lossless: 24-bit/96kHz/stereo = 4,608 kbps → 24 bits/sample
            //   Calculation: (4,608,000 bps) / (96,000 Hz * 2 channels) = 24.0 bits/sample
            // 
            // CRITICAL: Lossless formats like FLAC and ALAC are compressed (typically 45%-70% of raw size).
            // We scale the bitrate using a 0.55 compression factor to get the original raw bitrate for depth calculation.
            finalIsLossless && bitrateKbps > 0 && sampleRateHz > 0 && channelCount > 0 -> {
                val isCompressedLossless = normalizedCodec != "WAV" && normalizedCodec != "PCM" && normalizedCodec != "AIFF" && normalizedCodec != "DSD"
                val rawBitrate = if (isCompressedLossless) (bitrateKbps * 1000.0 / 0.55) else (bitrateKbps * 1000.0)
                val calculated = (rawBitrate / (sampleRateHz * channelCount)).toInt()
                val result = when {
                    calculated >= 22 -> 24  // 24-bit Hi-Res (22+ bits/sample for safety margin)
                    calculated >= 14 -> 16  // 16-bit CD Quality (14-21 bits/sample)
                    calculated >= 12 -> 16  // 16-bit with high compression (12-13 bits/sample)
                    else -> {
                        // Bit depth too low - likely incorrect metadata or not true lossless
                        Log.w(TAG, "Calculated bit depth too low ($calculated bits/sample) for claimed lossless codec '$normalizedCodec'. " +
                                "Bitrate: ${bitrateKbps}kbps, SampleRate: ${sampleRateHz}Hz, Channels: $channelCount. " +
                                "This may indicate corrupted metadata or lossy file misidentified as lossless.")
                        0  // Return 0 to indicate invalid/suspicious bit depth
                    }
                }
                Log.d(TAG, "Bit depth from bitrate (adjusted for compression=$isCompressedLossless): codec=$normalizedCodec, " +
                        "bitrate=${bitrateKbps}kbps, sampleRate=${sampleRateHz}Hz, " +
                        "channels=$channelCount, calculated=$calculated bits/sample → $result-bit")
                result
            }
            // Fallback to sample rate heuristics for lossless codecs
            // Hi-Res lossless typically starts at 48kHz (though CD quality is 44.1kHz)
            finalIsLossless && sampleRateHz >= 48000 -> {
                Log.d(TAG, "Bit depth from sample rate: ${sampleRateHz}Hz → 24-bit (Hi-Res assumed)")
                24
            }
            finalIsLossless -> {
                Log.d(TAG, "Bit depth default for lossless: 16-bit (CD Quality)")
                16
            }
            else -> 16                    // Default assumption for lossy
        }

        // Determine quality type and details
        Log.d(TAG, "Quality detection: codec=$normalizedCodec, sampleRate=${sampleRateHz}Hz, " +
                "bitrate=${bitrateKbps}kbps, bitDepth=$estimatedBitDepth-bit, " +
                "channels=$channelCount, isLossless=$finalIsLossless")
        
        return when {
            // DSD (Direct Stream Digital) - Top-tier high fidelity
            normalizedCodec.contains("DSD") || normalizedCodec == "DSD" -> {
                val dsdVersion = when {
                    sampleRateHz >= 22000000 -> "DSD512"
                    sampleRateHz >= 11000000 -> "DSD256"
                    sampleRateHz >= 5000000 -> "DSD128"
                    else -> "DSD64"
                }
                AudioQuality(
                    qualityType = QualityType.DSD_HIGH_RES,
                    qualityLabel = dsdVersion,
                    qualityDescription = "1-bit / ${(sampleRateHz / 1000000.0)} MHz DSD Audio",
                    isLossless = true,
                    isHiRes = true,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = 1,
                    category = "Hi-Res Master"
                )
            }

            // Dolby TrueHD / Atmos (Lossless surround)
            normalizedCodec in listOf("TRUEHD", "DOLBY TRUEHD", "ATMOS", "DOLBY ATMOS", "MLP") -> {
                AudioQuality(
                    qualityType = QualityType.DOLBY_LOSSLESS,
                    qualityLabel = if (normalizedCodec.contains("ATMOS")) "Dolby Atmos" else "Dolby TrueHD",
                    qualityDescription = "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Lossless Surround",
                    isLossless = true,
                    isHiRes = true,
                    isDolby = true,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Lossless Surround"
                )
            }

            // Dolby Digital (AC-3, E-AC-3) - Lossy surround
            normalizedCodec in listOf("AC-3", "AC3", "E-AC-3", "EAC3", "DOLBY DIGITAL", "DOLBY DIGITAL PLUS") -> {
                val label = when {
                    normalizedCodec.contains("E-AC-3") || normalizedCodec.contains("EAC3") ||
                    normalizedCodec.contains("PLUS") -> "Dolby Digital Plus"
                    else -> "Dolby Digital"
                }
                AudioQuality(
                    qualityType = QualityType.DOLBY_LOSSY_SURROUND,
                    qualityLabel = label,
                    qualityDescription = "$bitrateKbps kbps / ${sampleRateKhz.toInt()} kHz Surround",
                    isLossless = false,
                    isHiRes = false,
                    isDolby = true,
                    isDTS = false,
                    bitDepthEstimate = 16,
                    category = "Lossy Surround"
                )
            }

            // DTS (various forms)
            isDTSCodec -> {
                val isLosslessDTS = normalizedCodec.contains("HD") || normalizedCodec.contains("MA")
                AudioQuality(
                    qualityType = QualityType.DTS_SURROUND,
                    qualityLabel = if (isLosslessDTS) "DTS-HD Master Audio" else "DTS",
                    qualityDescription = if (isLosslessDTS)
                        "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Lossless Surround"
                    else
                        "$bitrateKbps kbps Surround",
                    isLossless = isLosslessDTS,
                    isHiRes = isLosslessDTS,
                    isDolby = false,
                    isDTS = true,
                    bitDepthEstimate = if (isLosslessDTS) estimatedBitDepth else 16,
                    category = if (isLosslessDTS) "Lossless Surround" else "Lossy Surround"
                )
            }

            // Dolby Surround (5.1/7.1 FLAC, ALAC, etc.)
            finalIsLossless && channelCount > 2 -> {
                val channelLabel = when (channelCount) {
                    6 -> "5.1"
                    8 -> "7.1"
                    else -> "${channelCount}ch"
                }
                AudioQuality(
                    qualityType = QualityType.LOSSLESS_SURROUND,
                    qualityLabel = "Dolby Surround $channelLabel",
                    qualityDescription = "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Surround",
                    isLossless = true,
                    isHiRes = estimatedBitDepth >= 24 || sampleRateHz > 48000,
                    isDolby = true,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Dolby Surround"
                )
            }

            // Hi-Res Studio Master (192 kHz, 24-bit lossless, >4600 kbps)
            // Studio Master quality represents the highest tier of lossless audio with 24-bit depth
            // and 192 kHz sample rate, matching or exceeding professional studio master recordings.
            // At 192 kHz, can theoretically reproduce frequencies up to 96 kHz (far beyond human hearing).
            finalIsLossless && sampleRateHz >= 192000 && estimatedBitDepth >= 24 -> {
                AudioQuality(
                    qualityType = QualityType.HI_RES_STUDIO_MASTER,
                    qualityLabel = "Studio Master",
                    qualityDescription = "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Lossless",
                    isLossless = true,
                    isHiRes = true,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Lossless"
                )
            }

            // Hi-Res Lossless (48-96 kHz, typically 24-bit, >2000 kbps)
            // High-resolution lossless uses higher bit depths (24-bit, ~144 dB dynamic range) and 
            // sample rates (96 kHz+) than CD quality, capturing more audio information and providing
            // a wider frequency range and dynamic range. Sample rates above 48 kHz can reproduce
            // frequencies beyond the 20 kHz limit of human hearing.
            // 
            // CRITICAL: Hi-Res Lossless requires BOTH:
            // - Sample rate ≥ 48 kHz (above CD's 44.1 kHz), AND
            // - Bit depth = 24-bit (not 16-bit)
            // A file with 48kHz/16-bit is NOT Hi-Res, it's just high sample rate CD quality
            // 
            // VALIDATION: Bit depth must be valid (>= 16) to qualify as Hi-Res Lossless
            finalIsLossless && sampleRateHz >= 48000 && estimatedBitDepth >= 24 -> {
                Log.d(TAG, "Detected: HI_RES_LOSSLESS (sampleRate=${sampleRateHz}Hz, bitDepth=$estimatedBitDepth-bit)")
                AudioQuality(
                    qualityType = QualityType.HI_RES_LOSSLESS,
                    qualityLabel = "Hi-Res Lossless",
                    qualityDescription = "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Lossless",
                    isLossless = true,
                    isHiRes = true,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Lossless"
                )
            }

            // CD Quality Lossless (44.1 kHz, 16-bit, ~1411 kbps)
            // This is the standard for lossless audio - CD quality with bit-perfect reproduction
            // Standard CD: 16-bit depth provides ~96 dB dynamic range, 44.1 kHz captures up to 22.05 kHz
            // 
            // VALIDATION: Bit depth must be valid (>= 12) to qualify as lossless
            // Files with bit depth < 12 are likely corrupt or have incorrect metadata
            finalIsLossless && estimatedBitDepth >= 12 -> { // Catches any remaining lossless formats with valid bit depth
                Log.d(TAG, "Detected: CD_QUALITY_LOSSLESS (sampleRate=${sampleRateHz}Hz, bitDepth=$estimatedBitDepth-bit)")
                AudioQuality(
                    qualityType = QualityType.CD_QUALITY_LOSSLESS,
                    qualityLabel = "Lossless",
                    qualityDescription = "${estimatedBitDepth}-bit / ${sampleRateKhz.toInt()} kHz Lossless",
                    isLossless = true,
                    isHiRes = false,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Lossless"
                )
            }
            
            // Invalid/Suspicious Lossless (bit depth too low)
            // If a file claims to be lossless but has calculated bit depth < 12,
            // treat it as lossy to avoid misclassification
            finalIsLossless && estimatedBitDepth < 12 -> {
                Log.w(TAG, "File claims lossless codec but bit depth is too low ($estimatedBitDepth-bit). " +
                        "Treating as LOSSY. Codec: $normalizedCodec, Bitrate: ${bitrateKbps}kbps, " +
                        "SampleRate: ${sampleRateHz}Hz")
                AudioQuality(
                    qualityType = QualityType.LOSSY_COMPRESSED,
                    qualityLabel = "Lossy",
                    qualityDescription = "$bitrateKbps kbps / ${sampleRateKhz.toInt()} kHz",
                    isLossless = false,
                    isHiRes = false,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = estimatedBitDepth,
                    category = "Lossy"
                )
            }

            // Lossy Compressed (MP3, AAC, etc.)
            else -> {
                AudioQuality(
                    qualityType = QualityType.LOSSY_COMPRESSED,
                    qualityLabel = "Lossy",
                    qualityDescription = "$bitrateKbps kbps / ${sampleRateKhz.toInt()} kHz",
                    isLossless = false,
                    isHiRes = false,
                    isDolby = false,
                    isDTS = false,
                    bitDepthEstimate = 16, // Lossy formats don't have a meaningful bit depth after encoding
                    category = "Lossy"
                )
            }
        }
    }

    /**
     * Get a simple quality badge label for UI display
     */
    fun getQualityBadge(quality: AudioQuality): String {
        return when (quality.qualityType) {
            QualityType.DSD_HIGH_RES -> quality.qualityLabel // e.g. "DSD64"
            QualityType.HI_RES_STUDIO_MASTER -> "Studio Master"
            QualityType.HI_RES_LOSSLESS -> "Hi-Res"
            QualityType.CD_QUALITY_LOSSLESS -> "Lossless"
            QualityType.LOSSLESS_SURROUND -> quality.qualityLabel // "Dolby Surround 5.1" etc.
            QualityType.DOLBY_LOSSLESS -> when {
                quality.qualityLabel.contains("Atmos") -> "Dolby Atmos"
                else -> "Dolby TrueHD"
            }
            QualityType.DOLBY_LOSSY_SURROUND -> when {
                quality.qualityLabel.contains("Plus") -> "Dolby Digital Plus"
                else -> "Dolby Digital"
            }
            QualityType.DTS_SURROUND -> when {
                quality.isLossless -> "DTS-HD Master Audio"
                else -> "DTS"
            }
            QualityType.LOSSY_COMPRESSED -> when {
                quality.qualityDescription.contains("320") -> "320kbps"
                quality.qualityDescription.contains("256") -> "256kbps"
                quality.qualityDescription.contains("192") -> "192kbps"
                quality.qualityDescription.contains("128") -> "128kbps"
                else -> "Lossy"
            }
            QualityType.UNKNOWN -> ""
        }
    }

    /**
     * Determine if audio qualifies as "Hi-Res" (≥48 kHz or lossless with >2000 kbps)
     */
    fun isHiResAudio(sampleRateHz: Int, bitrateKbps: Int, isLossless: Boolean): Boolean {
        return sampleRateHz >= 48000 || (isLossless && bitrateKbps >= 2000)
    }

    /**
     * Get color indicator for quality level (for UI theming)
     * Returns: "excellent", "good", "standard", "unknown"
     */
    fun getQualityColorIndicator(quality: AudioQuality): String {
        return when (quality.qualityType) {
            QualityType.DSD_HIGH_RES,
            QualityType.HI_RES_STUDIO_MASTER, 
            QualityType.DOLBY_LOSSLESS,
            QualityType.LOSSLESS_SURROUND -> "excellent"
            
            QualityType.HI_RES_LOSSLESS, 
            QualityType.CD_QUALITY_LOSSLESS -> "good"
            
            QualityType.LOSSY_COMPRESSED,
            QualityType.DOLBY_LOSSY_SURROUND,
            QualityType.DTS_SURROUND -> "standard"
            
            QualityType.UNKNOWN -> "unknown"
        }
    }
}
