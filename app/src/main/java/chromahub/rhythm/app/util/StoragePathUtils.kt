/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/** Resolve selected trees conservatively: an opaque provider ID is never a broader folder. */
object StoragePathUtils {
    fun getPathFromTreeUri(context: Context, uri: Uri): String? = try {
        if (uri.scheme == "file") uri.path?.let(::sanitizeAndNormalizePath)?.takeIf { it.startsWith('/') }
        else {
            val id = runCatching { DocumentsContract.getTreeDocumentId(uri) }
                .getOrElse { runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull().orEmpty() }
            when (uri.authority) {
                "com.android.externalstorage.documents" -> {
                    val parts = id.split(':', limit = 2)
                    val volume = parts[0]
                    val relative = parts.getOrElse(1) { "" }.trimStart('/')
                    if (relative.split('/').any { it == ".." } || volume.isBlank() || volume.contains('/')) null
                    else {
                        val base = when (volume.lowercase(java.util.Locale.ROOT)) {
                            "primary" -> primaryRoot()
                            "home" -> primaryRoot() + "/Documents"
                            else -> "/storage/$volume"
                        }
                        sanitizeAndNormalizePath(if (relative.isEmpty()) base else "$base/$relative")
                    }
                }
                "com.android.providers.downloads.documents" -> when {
                    id == "downloads" -> sanitizeAndNormalizePath(primaryRoot() + "/Download")
                    id.startsWith("raw:") -> id.removePrefix("raw:").takeIf { it.startsWith('/') }
                        ?.let(::sanitizeAndNormalizePath)
                    else -> context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                        ?.use { cursor ->
                            if (!cursor.moveToFirst()) null else {
                                val column = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                if (column < 0) null else cursor.getString(column)?.takeIf { it.startsWith('/') }
                                    ?.let(::sanitizeAndNormalizePath)
                            }
                        }
                }
                else -> null
            }
        }
    } catch (_: Exception) { null }

    private fun primaryRoot(): String = runCatching { Environment.getExternalStorageDirectory().absolutePath }
        .getOrDefault("/storage/emulated/0")

    fun sanitizeAndNormalizePath(path: String?): String {
        if (path.isNullOrBlank()) return ""
        var value = path.trim().removePrefix("raw:").replace('\\', '/').replace(Regex("/+"), "/").trimEnd('/')
        if (value.isEmpty() && path.trim().startsWith('/')) return "/"
        val root = primaryRoot()
        for (alias in listOf("/sdcard", "/storage/self/primary")) {
            if (value == alias || value.startsWith("$alias/")) value = root + value.removePrefix(alias)
        }
        while (value.startsWith(root + root)) value = value.removePrefix(root)
        return value
    }

    fun isFolderSubdirectoryOrEqual(parentFolder: String, childPath: String): Boolean {
        val parent = sanitizeAndNormalizePath(parentFolder)
        val child = sanitizeAndNormalizePath(childPath)
        if (parent.isEmpty() || child.isEmpty()) return false
        return child.equals(parent, true) || child.startsWith(if (parent.endsWith('/')) parent else "$parent/", true)
    }

    fun isPathInFolders(childPath: String?, parentFolders: Collection<String>): Boolean =
        !childPath.isNullOrBlank() && parentFolders.any { isFolderSubdirectoryOrEqual(it, childPath) }

    /** Small fixed suggestions, with no subtree traversal or library refresh. */
    fun getCommonMediaFolders(context: Context): List<String> {
        val roots = linkedSetOf(primaryRoot())
        context.getExternalFilesDirs(null).filterNotNull().mapTo(roots) {
            it.absolutePath.substringBefore("/Android/data")
        }
        return roots.flatMap { root -> listOf("Music", "Download", "Podcasts", "Audiobooks", "Recordings")
            .map { File(root, it) } }.filter { it.isDirectory && it.canRead() }
            .map { sanitizeAndNormalizePath(it.absolutePath) }.distinct().sorted()
    }
}
