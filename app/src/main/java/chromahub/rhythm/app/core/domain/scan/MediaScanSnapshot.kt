/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.core.domain.scan

import android.database.Cursor
import android.provider.MediaStore

internal fun MediaLibraryFingerprint.addMediaStoreRow(cursor: Cursor, id: String, path: String?, scope: MediaScanScope) {
    fun number(column: String): Long = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getLong) ?: 0L
    fun text(column: String): String? = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getString)
    add(id, path?.let(scope::normalizedPath), number(MediaStore.Audio.Media.DATE_MODIFIED),
        number(MediaStore.Audio.Media.SIZE), number(MediaStore.Audio.Media.DURATION),
        text(MediaStore.Audio.Media.TITLE), text(MediaStore.Audio.Media.ARTIST), text(MediaStore.Audio.Media.ALBUM))
}
