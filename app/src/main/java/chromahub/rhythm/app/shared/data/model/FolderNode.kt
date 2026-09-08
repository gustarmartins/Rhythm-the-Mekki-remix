/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.data.model

import android.net.Uri
import java.io.File

data class FolderNode(
    val name: String,
    val path: String,
    val subFolders: Map<String, FolderNode> = emptyMap(),
    val songs: List<Song> = emptyList(),
    val albumId: Long? = null,
    val coverUri: Uri? = null,
    val totalSongCount: Int = 0
) {
    val isLeaf: Boolean get() = subFolders.isEmpty() && songs.isNotEmpty()
    val isEmpty: Boolean get() = subFolders.isEmpty() && songs.isEmpty()
}
