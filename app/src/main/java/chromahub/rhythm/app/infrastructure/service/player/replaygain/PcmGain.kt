/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.infrastructure.service.player.replaygain

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** Scale linear PCM in its declared wire format, preserving the sign of over-range integer peaks. */
@UnstableApi
internal fun scalePcm(
    input: ByteBuffer, output: ByteBuffer, encoding: Int, channels: Int, frames: Int,
    initialGain: Float, finalGain: Float
) {
    val originalInputOrder = input.order()
    val originalOutputOrder = output.order()
    val order = when (encoding) {
        C.ENCODING_PCM_16BIT_BIG_ENDIAN, C.ENCODING_PCM_24BIT_BIG_ENDIAN,
        C.ENCODING_PCM_32BIT_BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
        C.ENCODING_PCM_FLOAT -> ByteOrder.nativeOrder()
        else -> ByteOrder.LITTLE_ENDIAN
    }
    input.order(order)
    output.order(order)
    try {
        when (encoding) {
            C.ENCODING_PCM_8BIT -> forSamples(channels, frames, initialGain, finalGain) { gain ->
                val sample = (input.get().toInt() and 0xff) - 128
                output.put(((sample * gain).toInt().coerceIn(-128, 127) + 128).toByte())
            }
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN ->
                forSamples(channels, frames, initialGain, finalGain) { gain ->
                    val sample = (input.short * gain).toInt().coerceIn(-32768, 32767)
                    output.putShort(sample.toShort())
                }
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN ->
                forSamples(channels, frames, initialGain, finalGain) { gain ->
                    val sample = if (order == ByteOrder.BIG_ENDIAN) {
                        (input.get().toInt() shl 16) or (input.short.toInt() and 0xffff)
                    } else {
                        (input.short.toInt() and 0xffff) or (input.get().toInt() shl 16)
                    }
                    val scaled = (sample * gain).toInt().coerceIn(-8388608, 8388607)
                    if (order == ByteOrder.BIG_ENDIAN) {
                        output.put((scaled shr 16).toByte()).putShort(scaled.toShort())
                    } else {
                        output.putShort(scaled.toShort()).put((scaled shr 16).toByte())
                    }
                }
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN ->
                forSamples(channels, frames, initialGain, finalGain) { gain ->
                    output.putInt((input.int.toDouble() * gain).toInt())
                }
            C.ENCODING_PCM_FLOAT -> forSamples(channels, frames, initialGain, finalGain) { gain ->
                output.putFloat(input.float * gain)
            }
            else -> error("Unsupported linear PCM encoding $encoding")
        }
    } finally {
        // AudioProcessor buffers are shared with the surrounding Media3 chain.
        input.order(originalInputOrder)
        output.order(originalOutputOrder)
    }
}

private inline fun forSamples(channels: Int, frames: Int, initial: Float, target: Float, write: (Float) -> Unit) {
    if (frames == 0) return
    if (abs(target - initial) <= 0.0001f) {
        repeat(channels * frames) { write(target) }
    } else {
        val step = (target - initial) / frames
        var gain = initial
        repeat(frames) {
            gain += step
            repeat(channels) { write(gain) }
        }
    }
}
