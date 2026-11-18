/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.documentfile.provider.DocumentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GrantedLyricDocumentsTest {
    private val authority = "test.rhythm.lyrics"
    private val context: Context = RuntimeEnvironment.getApplication()
    private val provider = LyricsProvider()
    private lateinit var directory: DocumentFile

    @Before fun setUp() {
        provider.attachInfo(context, ProviderInfo().apply { authority = this@GrantedLyricDocumentsTest.authority })
        ShadowContentResolver.registerProviderInternal(authority, provider)
        val tree = DocumentsContract.buildTreeDocumentUri(authority, "primary:Lyrics")
        directory = requireNotNull(DocumentFile.fromTreeUri(context, tree))
    }

    @Test fun manyMissesAndDuplicateNamesUseOneDirectoryQuery() {
        val names = listOf("custom.lrc", "track.lrc") + (1..35).map { "missing-$it.txt" }
        val found = grantedLyricDocuments(context, directory, names)
        assertEquals(listOf("custom.lrc", "track.lrc"), names.filter(found::containsKey))
        assertEquals("different:opaque/id", DocumentsContract.getDocumentId(found.getValue("custom.lrc")))
        assertEquals("first", DocumentsContract.getDocumentId(found.getValue("track.lrc")))
        assertNull(found["Missing-1.txt"])
        assertEquals(1, provider.directoryQueries)
        assertEquals(0, provider.documentQueries)
    }

    @Test fun anIdOnlyProviderIsEnumeratedOnceAfterRefusingTheProjection() {
        provider.idOnly = true
        val found = grantedLyricDocuments(context, directory, listOf("absent.lrc", "track.lrc", "custom.lrc"))
        assertEquals("first", DocumentsContract.getDocumentId(found.getValue("track.lrc")))
        assertEquals("different:opaque/id", DocumentsContract.getDocumentId(found.getValue("custom.lrc")))
        assertEquals(2, provider.directoryQueries)
        assertEquals(3, provider.documentQueries)
    }

    private class LyricsProvider : ContentProvider() {
        var idOnly = false
        var directoryQueries = 0
        var documentQueries = 0
        private val rows = listOf("first" to "track.lrc", "duplicate" to "track.lrc", "different:opaque/id" to "custom.lrc")

        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val children = uri.lastPathSegment == "children"
            if (children) directoryQueries++ else documentQueries++
            val columns = if (children && idOnly) arrayOf(Document.COLUMN_DOCUMENT_ID)
                else projection?.toList()?.toTypedArray() ?: arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
            return MatrixCursor(columns).apply {
                for ((id, name) in rows) {
                    if (children || DocumentsContract.getDocumentId(uri) == id) {
                        addRow(columns.map { if (it == Document.COLUMN_DOCUMENT_ID) id else name }.toTypedArray())
                    }
                }
            }
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read-only")
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("Read-only")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("Read-only")
    }
}
