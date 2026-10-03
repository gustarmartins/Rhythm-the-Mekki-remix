/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Verifies that a FLAC tag rewrite preserved the lossless audio stream exactly.
 * Metadata blocks may legitimately change size and content, so they are skipped.
 */
object FlacIntegrityVerifier {
    data class AudioIdentity(
        val streamInfoAudioFields: ByteArray,
        val encodedAudioSha256: ByteArray,
        val encodedAudioBytes: Long
    )

    fun readAudioIdentity(file: File): AudioIdentity? =
        runCatching { file.inputStream().buffered().use(::readAudioIdentity) }.getOrNull()

    fun preservesAudio(before: AudioIdentity?, afterFile: File): Boolean {
        if (before == null) return false
        val after = readAudioIdentity(afterFile) ?: return false
        return before.streamInfoAudioFields.contentEquals(after.streamInfoAudioFields) &&
            before.encodedAudioSha256.contentEquals(after.encodedAudioSha256) &&
            before.encodedAudioBytes == after.encodedAudioBytes
    }

    internal fun readAudioIdentity(source: InputStream): AudioIdentity {
        val input = if (source is BufferedInputStream) source else BufferedInputStream(source)
        val marker = ByteArray(4)
        input.readFully(marker)
        require(marker.contentEquals(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()))) {
            "Not a native FLAC stream"
        }

        var streamInfoAudioFields: ByteArray? = null
        var lastBlock = false
        var blockCount = 0
        while (!lastBlock) {
            val header = input.read()
            if (header < 0) throw EOFException("Missing FLAC metadata block")
            lastBlock = header and 0x80 != 0
            val blockType = header and 0x7f
            val length = (input.readByte() shl 16) or (input.readByte() shl 8) or input.readByte()
            require(length >= 0) { "Invalid FLAC metadata length" }

            if (blockType == 0) {
                require(blockCount == 0 && length == 34 && streamInfoAudioFields == null) {
                    "Invalid FLAC STREAMINFO block"
                }
                val streamInfo = ByteArray(length)
                input.readFully(streamInfo)
                // Sample rate, channels, bit depth, total samples, and decoded-audio MD5.
                streamInfoAudioFields = streamInfo.copyOfRange(10, 34)
            } else {
                input.skipFully(length.toLong())
            }
            blockCount++
            require(blockCount <= 128) { "Unreasonable FLAC metadata block count" }
        }

        val audioFields = requireNotNull(streamInfoAudioFields) { "Missing FLAC STREAMINFO block" }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var audioBytes = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
            audioBytes += read
        }
        require(audioBytes > 0) { "FLAC stream has no encoded audio frames" }

        return AudioIdentity(audioFields, digest.digest(), audioBytes)
    }

    private fun InputStream.readByte(): Int {
        val value = read()
        if (value < 0) throw EOFException("Truncated FLAC metadata header")
        return value
    }

    private fun InputStream.readFully(destination: ByteArray) {
        var offset = 0
        while (offset < destination.size) {
            val read = read(destination, offset, destination.size - offset)
            if (read < 0) throw EOFException("Truncated FLAC metadata block")
            offset += read
        }
    }

    private fun InputStream.skipFully(byteCount: Long) {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                if (read() < 0) throw EOFException("Truncated FLAC metadata block")
                remaining--
            }
        }
    }
}
