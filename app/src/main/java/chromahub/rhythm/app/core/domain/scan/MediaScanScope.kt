/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.core.domain.scan

import java.util.Locale

/** The same scope is used for scanning and change detection. */
internal data class MediaScanScope(
    val mode: String,
    val allowedFormats: Set<String>?,
    val minimumDuration: Long,
    val whitelistedFolders: Set<String>,
    val whitelistedSongs: Set<String>,
    val blacklistedFolders: Set<String>,
    val blacklistedSongs: Set<String>,
    val storageRoot: String = "/storage/emulated/0"
) {
    val emptyWhitelist: Boolean get() = mode == "WHITELIST" && whitelistedFolders.isEmpty() && whitelistedSongs.isEmpty()
    fun normalizedPath(path: String): String {
        var p = path.trim().replace('\\', '/').trimEnd('/').lowercase(Locale.ROOT)
        for (alias in listOf("/sdcard", "/storage/self/primary")) {
            if (p == alias || p.startsWith("$alias/")) p = storageRoot.lowercase(Locale.ROOT) + p.removePrefix(alias)
        }
        return p
    }
    private fun inFolder(path: String, folder: String): Boolean {
        val normalizedFolder = normalizedPath(folder)
        return path == normalizedFolder || path.startsWith("$normalizedFolder/")
    }
    fun includes(id: String, path: String?, duration: Long): Boolean {
        if (id in blacklistedSongs || duration < minimumDuration || emptyWhitelist) return false
        val normalized = path?.let(::normalizedPath)
        if (normalized != null && allowedFormats != null) {
            val extension = normalized.substringAfterLast('.', "")
            if (extension.isNotEmpty() && extension !in allowedFormats) return false
        }
        if (mode == "WHITELIST" && id !in whitelistedSongs &&
            (normalized == null || whitelistedFolders.none { inFolder(normalized, it) })) return false
        if (mode == "BLACKLIST" && normalized != null && blacklistedFolders.any { inFolder(normalized, it) }) return false
        return true
    }
}

/** Order-independent provider snapshot; comparisons do not depend on the wall clock. */
internal class MediaLibraryFingerprint {
    private var count = 0
    private var xor = 0L
    private var sum = 0L
    fun add(id: String, path: String?, modified: Long, size: Long, duration: Long, title: String?, artist: String?, album: String?) {
        var hash = -3750763034362895579L
        val value = listOf(id, path.orEmpty(), modified.toString(), size.toString(), duration.toString(),
            title.orEmpty(), artist.orEmpty(), album.orEmpty()).joinToString("\u0000")
        for (c in value) { hash = (hash xor c.code.toLong()) * 1099511628211L }
        count++
        xor = xor xor hash
        sum += hash
    }
    fun value(): String = "$count:$xor:$sum"
}
