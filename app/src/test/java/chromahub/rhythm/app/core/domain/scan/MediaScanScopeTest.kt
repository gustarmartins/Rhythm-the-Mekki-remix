package chromahub.rhythm.app.core.domain.scan

import org.junit.Assert.*
import org.junit.Test

class MediaScanScopeTest {
    private fun scope(mode: String = "WHITELIST", folders: Set<String> = setOf("/storage/emulated/0/Music")) =
        MediaScanScope(mode, setOf("flac", "mp3"), 30_000L, folders, emptySet(),
            setOf("/storage/emulated/0/Downloads"), setOf("blocked"))

    @Test fun changesInDownloadsDoNotAffectMusicScope() {
        assertFalse(scope().includes("new", "/storage/emulated/0/Downloads/voice.mp3", 60_000))
        assertTrue(scope().includes("song", "/storage/emulated/0/Music/song.flac", 180_000))
    }
    @Test fun whitelistUsesFolderBoundaries() {
        assertFalse(scope().includes("backup", "/storage/emulated/0/MusicBackups/song.flac", 180_000))
        assertTrue(scope().includes("nested", "/storage/emulated/0/Music/Album/song.flac", 180_000))
    }
    @Test fun storageAliasesHaveTheSameScope() {
        assertTrue(scope().includes("a", "/sdcard/Music/song.mp3", 180_000))
        assertTrue(scope().includes("b", "/storage/self/primary/Music/song.mp3", 180_000))
    }
    @Test fun exclusionsAndDurationApplyBeforeChangeDetection() {
        assertFalse(scope("BLACKLIST").includes("recording", "/storage/emulated/0/Downloads/long.mp3", 180_000))
        assertFalse(scope().includes("blocked", "/sdcard/Music/song.mp3", 180_000))
        assertFalse(scope().includes("short", "/sdcard/Music/alert.mp3", 2_000))
        assertFalse(scope().includes("format", "/sdcard/Music/song.aac", 180_000))
    }
    @Test fun explicitlyWhitelistedIdsWorkWithoutAPath() {
        val s = scope().copy(whitelistedSongs = setOf("chosen"))
        assertTrue(s.includes("chosen", null, 180_000))
        assertFalse(s.includes("unknown", null, 180_000))
    }
    @Test fun intentionallyEmptyWhitelistIsStable() {
        val s = scope(folders = emptySet())
        assertTrue(s.emptyWhitelist)
        assertFalse(s.includes("song", "/sdcard/Music/song.flac", 180_000))
    }
    private fun snapshot(vararg ids: String, modified: Long = 9_999_999_999L, size: Long = 99L): String {
        val f = MediaLibraryFingerprint()
        ids.forEach { f.add(it, "/music/$it.flac", modified, size, 180_000, "Title", "Artist", "Album") }
        return f.value()
    }
    @Test fun providerRowOrderDoesNotTriggerARefresh() {
        assertEquals(snapshot("one", "two", "three"), snapshot("three", "one", "two"))
    }
    @Test fun unchangedFutureDatedFilesDoNotTriggerRepeatedRefreshes() {
        assertEquals(snapshot("future", modified = 9_999_999_999L), snapshot("future", modified = 9_999_999_999L))
    }
    @Test fun audioChangesAndReplacementsAreDetectedWithoutCountChanges() {
        assertNotEquals(snapshot("one"), snapshot("two"))
        assertNotEquals(snapshot("one", modified = 10L), snapshot("one", modified = 11L))
        assertNotEquals(snapshot("one", size = 10L), snapshot("one", size = 11L))
    }
    @Test fun removalChangesTheScopedSnapshot() {
        assertNotEquals(snapshot("one", "two"), snapshot("one"))
    }
}
