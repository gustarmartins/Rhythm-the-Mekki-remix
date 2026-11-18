/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import android.app.Application
import android.net.Uri
import android.os.Environment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StoragePathUtilsTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private fun tree(authority: String, id: String) = Uri.parse("content://$authority/tree/" + Uri.encode(id))
    @Test fun primaryAndRemovableTreesPreserveColonsInFolderNames() {
        val uri = tree("com.android.externalstorage.documents", "primary:Music/Album: Live")
        assertEquals(Environment.getExternalStorageDirectory().absolutePath + "/Music/Album: Live", StoragePathUtils.getPathFromTreeUri(context, uri))
        assertEquals("/storage/1234-5678/Music", StoragePathUtils.getPathFromTreeUri(context, tree("com.android.externalstorage.documents", "1234-5678:Music")))
    }
    @Test fun opaqueDownloadsIdsAndUnknownProvidersDoNotBroadenTheScope() {
        assertNull(StoragePathUtils.getPathFromTreeUri(context, tree("com.android.providers.downloads.documents", "msf:123")))
        assertNull(StoragePathUtils.getPathFromTreeUri(context, tree("unknown.provider", "primary:Music")))
    }
    @Test fun documentTraversalAndFolderPrefixCollisionsAreRejected() {
        assertNull(StoragePathUtils.getPathFromTreeUri(context, tree("com.android.externalstorage.documents", "primary:Music/../../")))
        assertTrue(StoragePathUtils.isFolderSubdirectoryOrEqual("/Music", "/Music/Albums"))
        assertFalse(StoragePathUtils.isFolderSubdirectoryOrEqual("/Music", "/Music2/Albums"))
        assertFalse(StoragePathUtils.isPathInFolders("/Music/a", emptyList()))
    }
    @Test fun explicitRawDownloadTreeAndLegacyAliasesRemainSupported() {
        assertEquals("/storage/emulated/0/Download/Album", StoragePathUtils.getPathFromTreeUri(context,
            tree("com.android.providers.downloads.documents", "raw:/storage/emulated/0/Download/Album")))
        assertEquals(Environment.getExternalStorageDirectory().absolutePath + "/Music",
            StoragePathUtils.sanitizeAndNormalizePath("/sdcard//Music/"))
    }
}
