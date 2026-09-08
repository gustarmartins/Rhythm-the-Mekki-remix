/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service.player.replaygain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.exp
import kotlin.math.ln

class AdaptiveDynamicRangeCompression {
    companion object {
        private const val TAG = "AdaptiveDRC"
        // The minimum accepted absolute input value to prevent numerical issues
        // when the input is close to zero.
        private const val kMinLogAbsValue = 0.032767f
        // Fixed-point arithmetic limits
        private const val kFixedPointLimit = 32767.0f
    }

    private var slope = 0.0f
    private var samplingRate = 0.0f
    private var state = 0.0f
    private var compressorGain = 1.0f
    private var alphaAttack = 0.0f
    private var alphaRelease = 0.0f
    private var inited = false

    fun init(samplingRate: Int, tauAttack: Float, tauRelease: Float, compressionRatio: Float) {
        this.samplingRate = samplingRate.toFloat()
        this.state = 0.0f
        this.slope = 1.0f / compressionRatio - 1.0f
        this.compressorGain = 1.0f
        
        if (tauAttack > 0.0f) {
            val taufs = tauAttack * this.samplingRate
            this.alphaAttack = exp(-1.0f / taufs)
        } else {
            this.alphaAttack = 0.0f
        }
        
        if (tauRelease > 0.0f) {
            val taufs = tauRelease * this.samplingRate
            this.alphaRelease = exp(-1.0f / taufs)
        } else {
            this.alphaRelease = 0.0f
        }
        
        this.inited = true
    }

    fun flush() {
        if (inited) {
            init(samplingRate.toInt(), 0.0014f, 0.093f, 2.0f)
        }
    }

    fun compress(
        channelCount: Int,
        inputAmp: Float,
        kneeThresholdDb: Float,
        postAmp: Float,
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        frameCount: Int
    ) {
        if (!inited) {
            throw IllegalStateException("called compress() before init()")
        }

        val scale = 32768.0f // 1 << 15
        val inverseScale = 1.0f / scale
        
        // Converts from dB to natural log-base
        val kneeThreshold = 0.1151292546497023f * kneeThresholdDb + 10.39717719f

        val oldInputOrder = inputBuffer.order()
        val oldOutputOrder = outputBuffer.order()
        inputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.order(ByteOrder.nativeOrder())

        val inputFloatBuffer = inputBuffer.asFloatBuffer()
        val outputFloatBuffer = outputBuffer.asFloatBuffer()

        for (i in 0 until frameCount) {
            var maxAbsVal = 0.0f
            
            // Find max absolute value in the frame
            for (c in 0 until channelCount) {
                val index = i * channelCount + c
                if (index < inputFloatBuffer.limit()) {
                    val sample = inputFloatBuffer.get(index)
                    val vVal = sample * inputAmp * scale
                    val absVal = abs(vVal)
                    if (absVal > maxAbsVal) {
                        maxAbsVal = absVal
                    }
                }
            }

            // A fast approximation to log / standard ln
            val maxAbsXDb = ln(max(maxAbsVal, kMinLogAbsValue))
            
            // Subtract Threshold from log-encoded input to get the amount of overshoot
            val overshoot = maxAbsXDb - kneeThreshold
            
            // Hard half-wave rectifier
            val rect = max(overshoot, 0.0f)
            
            // Multiply rectified overshoot with slope
            val cv = rect * slope
            val prevState = state
            val alpha = if (cv <= state) alphaAttack else alphaRelease
            state = alpha * state + (1.0f - alpha) * cv
            compressorGain *= exp(state - prevState)

            // Apply compressor gain to all channels in the frame
            for (c in 0 until channelCount) {
                val index = i * channelCount + c
                if (index < inputFloatBuffer.limit()) {
                    val sample = inputFloatBuffer.get(index)
                    val vVal = sample * inputAmp * scale
                    val x = vVal * compressorGain * postAmp
                    val clamped = max(-kFixedPointLimit, min(kFixedPointLimit, x))
                    outputFloatBuffer.put(index, clamped * inverseScale)
                }
            }
        }

        inputBuffer.order(oldInputOrder)
        outputBuffer.order(oldOutputOrder)
    }

    fun reset() {
        inited = false
    }

    fun release() {
        // Nothing to release in pure Kotlin
    }
}
