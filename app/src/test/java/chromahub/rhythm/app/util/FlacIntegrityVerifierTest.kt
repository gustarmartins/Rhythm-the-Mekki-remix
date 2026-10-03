/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlacIntegrityVerifierTest {
    @Test
    fun metadataChangesPreserveTheSameEncodedAudio() {
        val audio = ByteArray(4096) { index -> (index * 31).toByte() }
        val before = flac(vorbisComment = byteArrayOf(1, 2, 3), audio = audio)
        val after = flac(vorbisComment = ByteArray(900) { 7 }, audio = audio)
        val beforeIdentity = FlacIntegrityVerifier.readAudioIdentity(ByteArrayInputStream(before))
        val afterFile = writeTempFlac(after)

        assertTrue(FlacIntegrityVerifier.preservesAudio(beforeIdentity, afterFile))
    }

    @Test
    fun changedEncodedAudioIsRejected() {
        val beforeAudio = ByteArray(2048) { index -> index.toByte() }
        val afterAudio = beforeAudio.copyOf().also { it[100] = (it[100] + 1).toByte() }
        val beforeIdentity = FlacIntegrityVerifier.readAudioIdentity(
            ByteArrayInputStream(flac(byteArrayOf(1), beforeAudio))
        )

        assertFalse(
            FlacIntegrityVerifier.preservesAudio(
                beforeIdentity,
                writeTempFlac(flac(byteArrayOf(2), afterAudio))
            )
        )
    }

    @Test
    fun changedStreamInfoIdentityIsRejected() {
        val audio = ByteArray(1024) { 42 }
        val before = flac(byteArrayOf(1), audio, streamInfoSeed = 3)
        val after = flac(byteArrayOf(2), audio, streamInfoSeed = 4)
        val beforeIdentity = FlacIntegrityVerifier.readAudioIdentity(ByteArrayInputStream(before))

        assertFalse(FlacIntegrityVerifier.preservesAudio(beforeIdentity, writeTempFlac(after)))
    }

    @Test
    fun truncatedFlacIsNotAccepted() {
        val truncated = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte(), 0)
        assertTrue(FlacIntegrityVerifier.readAudioIdentity(writeTempFlac(truncated)) == null)
    }

    private fun flac(
        vorbisComment: ByteArray,
        audio: ByteArray,
        streamInfoSeed: Int = 3
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()))

        val streamInfo = ByteArray(34) { index -> (streamInfoSeed + index).toByte() }
        writeBlock(output, type = 0, last = false, payload = streamInfo)
        writeBlock(output, type = 4, last = true, payload = vorbisComment)
        output.write(audio)
        return output.toByteArray()
    }

    private fun writeBlock(
        output: ByteArrayOutputStream,
        type: Int,
        last: Boolean,
        payload: ByteArray
    ) {
        output.write(type or if (last) 0x80 else 0)
        output.write(payload.size shr 16 and 0xff)
        output.write(payload.size shr 8 and 0xff)
        output.write(payload.size and 0xff)
        output.write(payload)
    }

    private fun writeTempFlac(bytes: ByteArray): File {
        val directory = createTempDirectory("rhythm-flac-integrity").toFile()
        return File(directory, "fixture.flac").apply { writeBytes(bytes) }
    }
}
