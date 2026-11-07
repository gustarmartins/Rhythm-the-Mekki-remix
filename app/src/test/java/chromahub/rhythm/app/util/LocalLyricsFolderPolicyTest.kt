/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class LocalLyricsFolderPolicyTest {
    private val music = "content://com.android.externalstorage.documents/tree/primary%3AMusic"

    @Test fun restoredFolderSettingWithoutAndroidPermissionIsNotShownAsAccessible() {
        assertTrue(readableLyricsFolders(setOf(music), emptySet()).isEmpty())
        assertTrue(readableLyricsFolders(setOf(music), setOf("unrelated-tree")).isEmpty())
    }

    @Test fun regrantRestoresTheConfiguredFolderButDoesNotAdoptUnrequestedTrees() {
        assertEquals(setOf(music), readableLyricsFolders(setOf(music), setOf(music, "other-tree")))
        assertTrue(readableLyricsFolders(emptySet(), setOf(music)).isEmpty())
    }

    @Test fun musicGrantIncludesTelegramAndArbitrarilyNestedAlbumFolders() {
        assertEquals(listOf("Telegram"), lyricsDirectoryBelowTree(
            "/storage/emulated/0/Music/Telegram/Wave 103 VC.mp3", "primary:Music"))
        assertEquals(listOf("Japanese Mix", "Album", "Disc 2"), lyricsDirectoryBelowTree(
            "/storage/emulated/0/Music/Japanese Mix/Album/Disc 2/曲.flac", "primary:Music"))
    }

    @Test fun directMusicSidecarDoesNotRequireAnExtraDirectory() {
        assertEquals(emptyList<String>(), lyricsDirectoryBelowTree("/sdcard/Music/Song.flac", "primary:Music"))
    }

    @Test fun primaryStorageAliasesResolveToTheSameGrantedTree() {
        for (root in listOf("/sdcard", "/storage/emulated/0", "/storage/self/primary")) {
            assertEquals(listOf("Album"), lyricsDirectoryBelowTree("$root/Music/Album/Song.flac", "primary:Music"))
        }
    }

    @Test fun siblingFoldersAndDifferentStorageVolumesAreNotInsideTheGrant() {
        assertNull(lyricsDirectoryBelowTree("/sdcard/Music backups/Song.mp3", "primary:Music"))
        assertNull(lyricsDirectoryBelowTree("/sdcard2/Music/Song.mp3", "primary:Music"))
        assertNull(lyricsDirectoryBelowTree("/storage/ABCD-1234/Music/Song.mp3", "primary:Music"))
        assertNull(lyricsDirectoryBelowTree("/sdcard/Download/Song.mp3", "primary:Music"))
    }

    @Test fun removableStorageAndNestedTreeRootsRetainTheCorrectRelativePath() {
        assertEquals(listOf("Disc 1"), lyricsDirectoryBelowTree(
            "/storage/ABCD-1234/Music/Album/Disc 1/Song.flac", "ABCD-1234:Music/Album"))
        assertEquals(listOf("Album"), lyricsDirectoryBelowTree(
            "/storage/emulated/10/Music/Album/Song.flac", "primary:Music", "/storage/emulated/10"))
    }

    @Test fun pathTraversalAndMalformedPathsDoNotEscapeTheGrantedFolder() {
        assertNull(lyricsDirectoryBelowTree("/sdcard/Music/../Download/Song.mp3", "primary:Music"))
        assertNull(lyricsDirectoryBelowTree("/sdcard/Music/Album/Song.mp3", "primary:Music/.."))
        assertNull(lyricsDirectoryBelowTree("/sdcard/Music//Song.mp3", "primary:Music"))
    }
}
