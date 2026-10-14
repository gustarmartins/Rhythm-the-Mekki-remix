/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import chromahub.rhythm.app.infrastructure.audio.RhythmBassBoostProcessor
import chromahub.rhythm.app.infrastructure.audio.RhythmMonoAudioProcessor
import chromahub.rhythm.app.infrastructure.audio.RhythmSpatializationProcessor
import chromahub.rhythm.app.shared.data.model.AppSettings
import io.music_assistant.sendspin.api.*

/** Reuse Rhythm's PCM effects without changing frame counts or Sendspin's synchronization clock. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class MusicAssistantAudioSink(context: Context, private val output: AudioSink) : AudioSink {
    private val settings = AppSettings.getInstance(context)
    override fun open(format: SinkFormat): SinkHandle {
        // The bundled Sendspin capability list advertises 48kHz/16-bit stereo only.
        require(format.isPcm && format.bitDepth == 16)
        val bass = RhythmBassBoostProcessor()
        val spatial = RhythmSpatializationProcessor()
        val mono = RhythmMonoAudioProcessor()
        val processors = listOf(bass, spatial, mono)
        val audioFormat = AudioProcessor.AudioFormat(format.sampleRate, format.channels, C.ENCODING_PCM_16BIT)
        processors.forEach { it.configure(audioFormat); it.flush() }
        val sink = output.open(format)
        return object : SinkHandle by sink {
            private var previous: List<Any>? = null
            private var samples = ShortArray(0)
            private var processed = ByteArray(0)
            private fun configure() {
                val values = listOf(settings.bassBoostEnabled.value, settings.bassBoostStrength.value,
                    settings.virtualizerEnabled.value, settings.virtualizerStrength.value, settings.monoAudioEnabled.value)
                if (values == previous) return
                previous = values
                bass.setEnabled(settings.bassBoostEnabled.value)
                bass.setStrength(settings.bassBoostStrength.value.toShort())
                spatial.setEnabled(settings.virtualizerEnabled.value)
                spatial.setStrength(settings.virtualizerStrength.value.toShort())
                mono.setEnabled(settings.monoAudioEnabled.value)
            }
            override fun write(pcm: ByteArray, offset: Int, length: Int): Int {
                configure()
                if (processors.all { it.isBypassed() }) return sink.write(pcm, offset, length)
                require(length % format.bytesPerFrame == 0)
                if (samples.size < length / 2) samples = ShortArray(length / 2)
                if (processed.size < length) processed = ByteArray(length)
                val count = length / 2
                for (i in 0 until count) samples[i] = ((pcm[offset + i * 2].toInt() and 255) or (pcm[offset + i * 2 + 1].toInt() shl 8)).toShort()
                processors.filterNot { it.isBypassed() }.forEach { it.processSamples(samples, count) }
                for (i in 0 until count) {
                    processed[i * 2] = samples[i].toByte()
                    processed[i * 2 + 1] = (samples[i].toInt() shr 8).toByte()
                }
                // A partial AudioTrack write must not cause the same samples to be processed twice.
                var written = 0
                while (written < length) {
                    val n = sink.write(processed, written, length - written)
                    if (n <= 0) return -1
                    written += n
                }
                return length
            }
            override fun flush() { processors.forEach { it.flush() }; sink.flush() }
            override fun close() { try { processors.forEach { it.reset() } } finally { sink.close() } }
        }
    }
}
