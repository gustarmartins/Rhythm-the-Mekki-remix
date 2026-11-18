/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

import android.app.Application
import android.content.Context
import chromahub.rhythm.app.features.local.data.database.entity.SongEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StableDateAddedResolverTest {
    private val original = 1_600_000_000_000L
    private val observed = 1_700_000_000_000L
    private val prefs: android.content.SharedPreferences get() {
        val app: Application = RuntimeEnvironment.getApplication()
        return app.getSharedPreferences("date-test", Context.MODE_PRIVATE)
    }
    @Before fun reset() { prefs.edit().clear().commit() }
    private fun row(id: String = "old", path: String = "/music/old.flac", title: String = "Song", artist: String = "Artist") = SongEntity(
        id, title, artist, "Album", "album", 180000, "content://media/$id", null, 0, 0,
        null, original, original, null, null, null, null, null, path = path)
    private fun resolver(exists: Boolean = false) = StableDateAddedResolver(prefs, now = { observed }, pathExists = { exists })
    @Test fun movedRecordingRetainsItsOriginalAdditionDate() {
        val r = resolver(); r.prepareForScan(listOf(row()))
        assertEquals(original, r.resolveDateAdded("/new/moved.flac", observed, "new", "Song", "Artist", "Album", 180000))
    }
    @Test fun aNewCopyDoesNotInheritTheDateOfAFileThatStillExists() {
        val r = resolver(exists = true); r.prepareForScan(listOf(row()))
        assertEquals(observed, r.resolveDateAdded("/new/copy.flac", observed, "new", "Song", "Artist", "Album", 180000))
    }
    @Test fun reusedMediaStoreIdDoesNotMatchDifferentRecordingOrPath() {
        val r = resolver(); r.prepareForScan(listOf(row()))
        assertEquals(observed, r.resolveDateAdded("/new/other.flac", observed, "old", "Other", "Someone", "Album", 180000))
    }
    @Test fun retaggingTheSameMediaStoreFileKeepsHistory() {
        val r = resolver(); r.prepareForScan(listOf(row()))
        assertEquals(original, r.resolveDateAdded("/music/old.flac", observed, "old", "New title", "New artist", "Album", 180000))
    }
    @Test fun replacementAtSamePathDoesNotReuseAStalePathCache() {
        prefs.edit().putLong(StableDateAddedResolver.pathKey("/music/old.flac"), original).commit()
        val r = resolver(); r.prepareForScan(listOf(row()))
        assertEquals(observed, r.resolveDateAdded("/music/old.flac", observed, "replacement", "Different", "Different", "Album", 180000))
    }
    @Test fun ambiguousRecordingsAndGenericFilenamesDoNotProveRelocation() {
        val r = resolver(); r.prepareForScan(listOf(row(), row("other", "/else/track01.flac")))
        assertEquals(observed, r.resolveDateAdded("/new/track01.flac", observed, "new", "Song", "Artist", "Album", 180000))
    }
    @Test fun persistentPathDateSurvivesReindexingWithoutRescanningAudio() {
        val r = resolver(); r.prepareForScan(listOf(row()))
        r.resolveDateAdded("/music/old.flac", observed, "old", "Song", "Artist", "Album", 180000); r.flush()
        assertEquals(original, resolver().resolveDateAdded("/music/old.flac", observed))
    }
    @Test fun timestampUnitsNormalizeWithoutUsingCacheAgeAsAScanReason() {
        for (input in listOf(1_600_000_000L, original, original * 1000, original * 1_000_000))
            assertEquals(original, StableDateAddedResolver.normalizeTimestamp(input))
        assertEquals(0L, StableDateAddedResolver.normalizeTimestamp(-1))
    }
}
