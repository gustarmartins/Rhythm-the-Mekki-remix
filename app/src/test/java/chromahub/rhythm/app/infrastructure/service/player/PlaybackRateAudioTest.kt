/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/** Measure the PCM path used by Rhythm, rather than only checking slider state. */
class PlaybackRateAudioTest {
    private fun render(speed: Float, pitch: Float): Pair<Double, Double> {
        val sampleRate = 48000
        val processor = SonicAudioProcessor()
        processor.setSpeed(speed)
        processor.setPitch(pitch)
        processor.configure(AudioProcessor.AudioFormat(sampleRate, 1, C.ENCODING_PCM_16BIT))
        processor.flush()
        val input = ByteBuffer.allocateDirect(sampleRate * 2 * 2).order(ByteOrder.nativeOrder())
        repeat(sampleRate * 2) { input.putShort((sin(2 * PI * 440 * it / sampleRate) * 16000).toInt().toShort()) }
        input.flip()
        val samples = ArrayList<Short>()
        fun drain() {
            val output = processor.output.order(ByteOrder.nativeOrder())
            while (output.remaining() >= 2) samples.add(output.short)
        }
        processor.queueInput(input)
        drain()
        processor.queueEndOfStream()
        while (!processor.isEnded) drain()
        val middle = samples.subList(samples.size / 4, samples.size * 3 / 4)
        val crossings = middle.zipWithNext().count { (a, b) -> a <= 0 && b > 0 }
        processor.reset()
        return samples.size.toDouble() / sampleRate to crossings.toDouble() * sampleRate / middle.size
    }

    @Test fun changingSpeedPreservesPitchWhenUnlinked() {
        val (seconds, hz) = render(1.25f, 1f)
        assertEquals(1.6, seconds, 0.05)
        assertEquals(440.0, hz, 8.0)
    }

    @Test fun changingPitchPreservesTempoWhenUnlinked() {
        val (seconds, hz) = render(1f, 0.75f)
        assertEquals(2.0, seconds, 0.05)
        assertEquals(330.0, hz, 8.0)
    }

    @Test fun linkedValuesChangeTempoAndPitchTogether() {
        val (seconds, hz) = render(1.25f, 1.25f)
        assertEquals(1.6, seconds, 0.05)
        assertEquals(550.0, hz, 8.0)
    }
}
