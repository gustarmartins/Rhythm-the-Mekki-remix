/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream

object LyricsFileUtils {

    private const val TAG = "LyricsFileUtils"
    private const val MAX_LYRICS_FILE_BYTES = 512 * 1024 // 512 KB
    private const val MAX_LYRICS_TEXT_CHARS = 200_000
    private val ALLOWED_LYRICS_EXTENSIONS = setOf("lrc", "txt", "json", "ttml", "xml", "elrc", "srt")
    private val ALLOWED_LYRICS_MIME_TYPES = setOf(
        "application/octet-stream",
        "application/x-lrc",
        "text/plain",
        "text/x-lrc",
        "application/json",
        "application/xml",
        "text/xml",
        "application/ttml+xml"
    )

    data class LoadResult(
        val lyrics: String? = null,
        val errorMessage: String? = null
    )

    fun loadLyricsFromUri(context: Context, uri: Uri): LoadResult {
        return try {
            val contentResolver = context.contentResolver
            val metadata = queryDocumentMetadata(context, uri)
            val displayName = metadata.displayName
            val mimeType = contentResolver.getType(uri)?.lowercase().orEmpty()
            val extension = displayName
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment
                    ?.substringAfterLast('.', "")
                    ?.lowercase()
                    ?.takeIf { it.isNotBlank() }

            if (!isSupportedLyricsDocument(mimeType, extension)) {
                return LoadResult(errorMessage = "Please select a .lrc, .elrc, .ttml, .json, or .txt lyrics file")
            }

            val size = metadata.size
            if (size != null && size > MAX_LYRICS_FILE_BYTES) {
                return LoadResult(errorMessage = "Lyrics file is too large (max 512 KB)")
            }

            val bytes = contentResolver.openInputStream(uri)?.use { inputStream ->
                readBytesWithLimit(inputStream, MAX_LYRICS_FILE_BYTES)
            } ?: return LoadResult(errorMessage = "Could not read the selected file")

            if (bytes.isEmpty()) {
                return LoadResult(errorMessage = "The selected file is empty")
            }

            if (isLikelyBinaryContent(bytes)) {
                return LoadResult(errorMessage = "Selected file is not a valid text lyrics file")
            }

            val text = decodeLyricsText(bytes)
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .take(MAX_LYRICS_TEXT_CHARS)
                .trim()

            if (text.isBlank()) {
                return LoadResult(errorMessage = "The selected file has no readable lyrics")
            }

            LoadResult(lyrics = text)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid lyrics file selected: $uri", e)
            LoadResult(
                errorMessage = if (e.message?.contains("max size", ignoreCase = true) == true) {
                    "Lyrics file is too large (max 512 KB)"
                } else {
                    e.message ?: "Invalid lyrics file"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load lyrics from URI: $uri", e)
            LoadResult(errorMessage = "Error loading lyrics file")
        }
    }

    private fun isSupportedLyricsDocument(mimeType: String, extension: String?): Boolean {
        if (mimeType.startsWith("audio/") || mimeType.startsWith("video/") || mimeType.startsWith("image/")) {
            return false
        }

        if (extension != null && extension in ALLOWED_LYRICS_EXTENSIONS) {
            return true
        }

        if (mimeType in ALLOWED_LYRICS_MIME_TYPES) {
            return true
        }

        return mimeType.startsWith("text/") || mimeType == "application/octet-stream"
    }

    private fun decodeLyricsText(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val b0 = bytes[0]
            val b1 = bytes[1]
            if (b0 == 0xFF.toByte() && b1 == 0xFE.toByte()) {
                return String(bytes, Charsets.UTF_16LE)
            }
            if (b0 == 0xFE.toByte() && b1 == 0xFF.toByte()) {
                return String(bytes, Charsets.UTF_16BE)
            }
        }

        if (bytes.size >= 3) {
            if (bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                return String(bytes, Charsets.UTF_8)
            }
        }

        val utf8Text = String(bytes, Charsets.UTF_8)
        val replacementChars = utf8Text.count { it == '\uFFFD' }

        return if (replacementChars > utf8Text.length / 20) {
            String(bytes, Charsets.ISO_8859_1)
        } else {
            utf8Text
        }
    }

    private fun isLikelyBinaryContent(bytes: ByteArray): Boolean {
        if (bytes.any { it == 0.toByte() }) {
            return true
        }

        val sample = bytes.take(4096)
        if (sample.isEmpty()) {
            return false
        }

        val suspiciousCount = sample.count { byte ->
            val value = byte.toInt() and 0xFF
            value < 0x09 || (value in 0x0E..0x1F)
        }

        return suspiciousCount.toDouble() / sample.size.toDouble() > 0.18
    }

    private fun readBytesWithLimit(inputStream: java.io.InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(8 * 1024)
        var totalRead = 0

        return ByteArrayOutputStream().use { output ->
            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                if (read == 0) continue

                totalRead += read
                if (totalRead > maxBytes) {
                    throw IllegalArgumentException("File exceeds max size")
                }

                output.write(buffer, 0, read)
            }

            output.toByteArray()
        }
    }

    private data class DocumentMetadata(
        val displayName: String?,
        val size: Long?
    )

    private fun queryDocumentMetadata(context: Context, uri: Uri): DocumentMetadata {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }
                return DocumentMetadata(displayName = name, size = size)
            }
        }

        return DocumentMetadata(displayName = null, size = null)
    }
}
