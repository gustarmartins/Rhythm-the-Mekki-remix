/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.features.local.data.database.RhythmDatabase
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.MediaScanMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

/** Uses an isolated Room DB and synthetic MediaStore; never changes the device's music files. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class LibraryScanIntegrityAndroidTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext

    private class Provider : ContentProvider() {
        var rows = 250
        var interruptAt: Int? = null
        var failure: Exception = CancellationException("simulated interrupted scan")
        var permissionCheck: (() -> Unit)? = null
        var unavailable = false
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                           selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            if (unavailable) return null
            val columns = requireNotNull(projection)
            return object : MatrixCursor(columns) {
                override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
                    if (newPosition == interruptAt) throw failure
                    if (newPosition == rows - 1) permissionCheck?.invoke()
                    return super.onMove(oldPosition, newPosition)
                }
            }.apply {
                repeat(rows) { index ->
                    addRow(columns.map { column -> when (column) {
                        MediaStore.Audio.Media._ID -> index + 900_000L
                        MediaStore.Audio.Media.TITLE -> "Regression song $index"
                        MediaStore.MediaColumns.DISPLAY_NAME -> "$index.mp3"
                        MediaStore.Audio.Media.ARTIST -> "Regression artist"
                        MediaStore.Audio.Media.ALBUM -> "Regression album"
                        MediaStore.Audio.Media.ALBUM_ID -> 900_000L
                        MediaStore.Audio.Media.DURATION -> 180_000L
                        MediaStore.Audio.Media.TRACK -> index + 1
                        MediaStore.Audio.Media.YEAR -> 2020
                        MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATE_MODIFIED -> 1_600_000_000L
                        MediaStore.Audio.Media.SIZE -> 1000L
                        MediaStore.Audio.Media.DATA -> "/rhythm-synthetic-regression/$index.mp3"
                        MediaStore.Audio.Media.ALBUM_ARTIST -> "Regression artist"
                        MediaStore.Audio.Media.DISC_NUMBER -> 1
                        else -> null
                    } }.toTypedArray<Any?>())
                }
            }
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    private suspend fun fixture(block: suspend (MediaScanEngine, RhythmDatabase, Provider, TestContext) -> Unit) {
        val provider = Provider()
        val context = TestContext(base, provider)
        val database = Room.inMemoryDatabaseBuilder(base, RhythmDatabase::class.java).build()
        val settings = AppSettings.getInstance(base)
        val previousMode = settings.mediaScanMode.value
        try {
            settings.setMediaScanMode(MediaScanMode.BLACKLIST)
            block(MediaScanEngine(context, database, settings), database, provider, context)
        } finally {
            settings.setMediaScanMode(previousMode)
            database.close()
            context.preferences.edit().clear().commit()
        }
    }

    private class TestContext(base: Context, provider: Provider) : ContextWrapper(base) {
        val preferences = base.getSharedPreferences("scan-regression-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        var permissionGranted = true
        private val resolver = ContentResolver.wrap(provider)
        override fun getContentResolver() = resolver
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
        override fun checkSelfPermission(permission: String) =
            if (permissionGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    private suspend fun scan(engine: MediaScanEngine, force: Boolean = true) =
        engine.performScan(forceRefresh = force, allowedFormats = null, minimumDuration = 0, reason = "integrity_regression")

    @Test fun cancellationAfterFiftyRowsKeepsCompleteLibraryAndRestartRepairsOldPartialRows(): Unit = runBlocking {
        fixture { engine, db, provider, context ->
            scan(engine)
            val completed = db.songDao().getAllSongs()
            provider.rows = 300
            provider.interruptAt = 50
            try { scan(engine); fail("Expected scan cancellation") } catch (_: CancellationException) { }
            assertEquals(completed, db.songDao().getAllSongs())
            val restartedJournal = LibraryScanJournal(context.preferences)
            assertTrue(restartedJournal.interrupted)
            assertTrue(restartedJournal.fullScanPending)
            // Reproduce an older build's partial DB with a still-complete provider fingerprint.
            db.songDao().replaceAll(completed.take(50))
            assertTrue(librarySnapshotNeedsRefresh(restartedJournal.fingerprint,
                restartedJournal.fingerprint!!, db.songDao().getCount(), 50, restartedJournal.interrupted))
            provider.interruptAt = null
            scan(MediaScanEngine(context, db, AppSettings.getInstance(base)), force = false)
            assertEquals(300, db.songDao().getCount())
            assertFalse(LibraryScanJournal(context.preferences).interrupted)
            assertTrue(context.preferences.getString("scoped_fingerprint", "")!!.startsWith("300:"))
        }
    }

    @Test fun failedQueryAndPermissionRevokedAtEndPreservePreviousSnapshot(): Unit = runBlocking {
        fixture { engine, db, provider, context ->
            scan(engine)
            val completed = db.songDao().getAllSongs()
            provider.unavailable = true
            try { scan(engine); fail("Expected unavailable provider") } catch (_: IOException) { }
            assertEquals(completed, db.songDao().getAllSongs())
            provider.unavailable = false
            provider.permissionCheck = { context.permissionGranted = false }
            try { scan(engine); fail("Expected permission failure") } catch (_: SecurityException) { }
            assertEquals(completed, db.songDao().getAllSongs())
            assertTrue(LibraryScanJournal(context.preferences).interrupted)
        }
    }

    @Test fun metadataBatchCannotTruncateLibraryOrResurrectDeletedSongs(): Unit = runBlocking {
        fixture { engine, db, _, _ ->
            scan(engine)
            val completed = db.songDao().getAllSongs()
            val batch = completed.take(50).map { it.copy(bitrate = 320_000) }
            db.songDao().updateCurrentMetadata(batch)
            assertEquals(250, db.songDao().getCount())
            assertEquals(50, db.songDao().getAllSongs().count { it.bitrate == 320_000 })
            db.songDao().deleteByIds(batch.map { it.id })
            assertTrue(db.songDao().updateCurrentMetadata(batch).isEmpty())
            assertEquals(200, db.songDao().getCount())
            // Same ID now denotes modified media; an old worker cannot overwrite the new row.
            db.songDao().upsertAll(listOf(completed.first().copy(dateModified = 1_700_000_000_000)))
            assertTrue(db.songDao().updateCurrentMetadata(batch).isEmpty())
            assertNull(db.songDao().getSongById(completed.first().id)!!.bitrate)
        }
    }
}
