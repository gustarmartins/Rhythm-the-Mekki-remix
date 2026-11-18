/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.documentfile.provider.DocumentFile

/** Returns the first child with each requested name, as DocumentFile.findFile does. */
internal fun grantedLyricDocuments(
    context: Context,
    directory: DocumentFile,
    candidateNames: List<String>
): Map<String, Uri> {
    val remaining = candidateNames.toMutableSet()
    if (remaining.isEmpty()) return emptyMap()
    val found = mutableMapOf<String, Uri>()
    val parent = directory.uri
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
    val projection = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
    val listed = try {
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
            if (idColumn < 0 || nameColumn < 0) return@use false
            while (remaining.isNotEmpty() && cursor.moveToNext()) {
                val id = cursor.getString(idColumn) ?: continue
                val name = cursor.getString(nameColumn) ?: DocumentFile.fromSingleUri(
                    context, DocumentsContract.buildDocumentUriUsingTree(parent, id)
                )?.name ?: continue
                if (remaining.remove(name)) found[name] = DocumentsContract.buildDocumentUriUsingTree(parent, id)
            }
            true
        } ?: return emptyMap()
    } catch (_: IllegalArgumentException) {
        false
    }
    if (!listed) {
        // Some providers only support listing IDs. Enumerate once and ask each child for its name.
        for (child in directory.listFiles()) {
            val name = child.name ?: continue
            if (remaining.remove(name)) found[name] = child.uri
            if (remaining.isEmpty()) break
        }
    }
    return found
}
