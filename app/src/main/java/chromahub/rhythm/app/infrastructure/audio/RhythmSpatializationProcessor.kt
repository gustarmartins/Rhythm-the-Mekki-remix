/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

/**
 * Rhythm Spatialization Processor - Real-time 3D audio enhancement
 * 
 * Uses advanced mid-side (M/S) processing for stereo widening to create immersive spatial audio.
 * Provides immediate spatial enhancement without file I/O latency, optimized for the Rhythm player.
 * 
 * Algorithm: Mid-side stereo widening with soft clipping
 * - Mid (M) = (L + R) / 2 (mono/center content)
 * - Side (S) = (L - R) / 2 (stereo/width content)
 * - Enhanced L = M + S * width
 * - Enhanced R = M - S * width
 * 
 * Width range: 1.0x to 3.0x based on strength (0-1000)
 * Works on: All devices with stereo audio
 * Latency: <1ms per buffer
 */
@OptIn(UnstableApi::class)
class RhythmSpatializationProcessor : RhythmAudioProcessor() {
    
    companion object {
        private const val TAG = "RhythmSpatialization"
    }
    
    // Parent processor for dynamic configuration sharing (crossfade thread safety)
    @Volatile
    private var parentProcessor: RhythmSpatializationProcessor? = null

    // Spatialization strength (0-1000)
    @Volatile
    private var strength: Short = 0
    @Volatile
    private var enabled: Boolean = false
    
    /**
     * Set the parent processor for dynamic synchronization
     */
    fun setParent(parent: RhythmSpatializationProcessor?) {
        this.parentProcessor = parent
    }

    /**
     * Enable or disable spatialization
     */
    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "Spatialization enabled: $enable")
        this.enabled = enable
    }
    
    /**
     * Set spatialization strength
     * @param strength Strength value from 0 to 1000
     */
    fun setStrength(strength: Short) {
        this.strength = strength.coerceIn(0, 1000)
        Log.d(TAG, "Spatialization strength set to: ${this.strength}")
    }
    
    /**
     * Get current strength
     */
    fun getStrength(): Short = parentProcessor?.getStrength() ?: strength
    
    override fun isEnabled(): Boolean = parentProcessor?.isEnabled() ?: enabled

    override fun isBypassed(): Boolean {
        return !isEnabled() || getStrength() == 0.toShort() || channelCount != 2
    }
    
    /**
     * Check if spatialization is available for current audio format
     * @return true if audio is stereo (2 channels)
     */
    fun isAvailable(): Boolean = channelCount == 2
    
    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val currentEnabled = isEnabled()
        val currentStrength = getStrength()
        if (!currentEnabled || currentStrength == 0.toShort() || channelCount != 2) {
            return // Only works for stereo, pass through otherwise
        }
        
        // Convert strength (0-1000) to width multiplier (1.0-3.0)
        // Using a carefully tuned curve for natural and musical spatial imaging
        val width = when {
            currentStrength == 0.toShort() -> 1.0f
            currentStrength <= 300 -> 1.0f + (currentStrength / 300.0f) * 0.5f  // 0-300 = 1.0-1.5x (subtle)
            currentStrength <= 700 -> 1.5f + ((currentStrength - 300) / 400.0f) * 0.8f  // 300-700 = 1.5-2.3x (medium)
            else -> 2.3f + ((currentStrength - 700) / 300.0f) * 0.7f  // 700-1000 = 2.3-3.0x (wide)
        }
        
        // Process stereo pairs (L, R, L, R, ...)
        for (i in 0 until sampleCount - 1 step 2) {
            // Convert to normalized float (-1.0 to 1.0)
            val left = samples[i] / 32768.0f
            val right = samples[i + 1] / 32768.0f
            
            // M/S encoding - separate center and side information
            val mid = (left + right) * 0.5f  // Center/mono content (vocals, bass, kick)
            val side = (left - right) * 0.5f  // Stereo/width content (reverb, panning, ambience)
            
            // Apply stereo widening by amplifying the side signal
            val wideSide = side * width
            
            // M/S decoding with widened side - reconstruct enhanced stereo
            val newLeft = mid + wideSide
            val newRight = mid - wideSide
            
            // Apply soft clipping to prevent harsh distortion at extreme widths
            val clippedLeft = softClip(newLeft)
            val clippedRight = softClip(newRight)
            
            // Convert back to 16-bit PCM
            samples[i] = (clippedLeft * 32767.0f).toInt().toShort()
            samples[i + 1] = (clippedRight * 32767.0f).toInt().toShort()
        }
    }
    
    /**
     * Soft clipping using saturating math (tanh-like approximation) to prevent harsh distortion.
     * Gracefully transitions from linear (|x| <= 1) to compression (|x| > 1).
     * Formula: sign(x) * (1.0 - 1.0/(1.0 + abs(x)))
     * 
     * At x=0: output=0. At x=1: output≈0.5. As x→∞: output→1.0 (saturates smoothly).
     * This is superior to hard clipping which creates audible aliasing at clipping boundary.
     * 
     * @param x Input signal (-∞ to +∞)
     * @return Soft-clipped signal (-1.0 to 1.0)
     */
    private fun softClip(x: Float): Float {
        if (x == 0f) return 0f
        val absValue = kotlin.math.abs(x)
        
        // Check if we're already in linear region
        val threshold = 0.8f
        if (absValue <= threshold) {
            return x
        }
        
        // In compression region: apply smooth saturation
        // Gracefully transitions from linear to an asymptote of 1.0
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        
        return kotlin.math.sign(x) * limited
    }
}
