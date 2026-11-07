/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

/** Restoring a settings string does not restore Android's read grant. */
internal fun readableLyricsFolders(configured: Set<String>, readableGrants: Set<String>): Set<String> =
    configured.intersect(readableGrants)

/** Locate the song's directory below a granted SAF tree, including nested folders and aliases. */
internal fun lyricsDirectoryBelowTree(
    songPath: String,
    treeDocumentId: String,
    primaryStorageRoot: String = "/storage/emulated/0"
): List<String>? {
    val volume = treeDocumentId.substringBefore(':')
    val treePath = treeDocumentId.substringAfter(':', "").trim('/').split('/').filter(String::isNotEmpty)
    val roots = if (volume.equals("primary", true))
        listOf(primaryStorageRoot.trimEnd('/'), "/sdcard", "/storage/self/primary")
    else listOf("/storage/$volume")
    val root = roots.firstOrNull { songPath.startsWith("$it/") } ?: return null
    val songParts = songPath.removePrefix("$root/").split('/')
    if ((treePath + songParts).any { it == "." || it == ".." || it.isEmpty() }) return null
    val directory = songParts.dropLast(1)
    if (directory.take(treePath.size) != treePath) return null
    return directory.drop(treePath.size)
}
