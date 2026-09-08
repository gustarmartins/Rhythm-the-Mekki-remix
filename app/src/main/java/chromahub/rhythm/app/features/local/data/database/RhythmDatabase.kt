/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import chromahub.rhythm.app.features.local.data.database.dao.ArtistDao
import chromahub.rhythm.app.features.local.data.database.dao.PlaylistDao
import chromahub.rhythm.app.features.local.data.database.dao.SongArtistDao
import chromahub.rhythm.app.features.local.data.database.dao.SongDao
import chromahub.rhythm.app.features.local.data.database.entity.ArtistEntity
import chromahub.rhythm.app.features.local.data.database.entity.PlaylistEntity
import chromahub.rhythm.app.features.local.data.database.entity.PlaylistSongEntity
import chromahub.rhythm.app.features.local.data.database.entity.SongArtistEntity
import chromahub.rhythm.app.features.local.data.database.entity.SongEntity

@Database(entities = [SongEntity::class, ArtistEntity::class, SongArtistEntity::class, PlaylistEntity::class, PlaylistSongEntity::class], version = 9, exportSchema = false)
abstract class RhythmDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun artistDao(): ArtistDao
    abstract fun songArtistDao(): SongArtistDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: RhythmDatabase? = null

        // Migration from version 1 to 2: Add artist and song-artist tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create artists table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `artists` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `artworkUri` TEXT, 
                        `numberOfAlbums` INTEGER NOT NULL, 
                        `numberOfTracks` INTEGER NOT NULL, 
                        `groupByAlbumArtist` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)

                // Create song_artists table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `song_artists` (
                        `songId` TEXT NOT NULL, 
                        `artistName` TEXT NOT NULL, 
                        `groupByAlbumArtist` INTEGER NOT NULL, 
                        PRIMARY KEY(`songId`, `artistName`, `groupByAlbumArtist`)
                    )
                """)
            }
        }

        // Migration from version 2 to 3: No schema changes, just version bump
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes needed, just ensure tables exist
            }
        }

        // Migration from version 3 to 4: Switch from destructive to proper migrations
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes, just preserve existing data
            }
        }

        // Migration from version 4 to 5: Persist multi-disc ordering metadata.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN discNumber INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Migration from version 5 to 6: Persist song-level modified timestamp.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN dateModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE songs SET dateModified = dateAdded WHERE dateModified = 0")
            }
        }

        // Migration from version 6 to 7: Add path column to songs table
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN path TEXT")
            }
        }

        // Migration from version 7 to 8: Add playlists and playlist_songs tables
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `dateCreated` INTEGER NOT NULL, 
                        `dateModified` INTEGER NOT NULL, 
                        `artworkUri` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
                        `playlistId` TEXT NOT NULL, 
                        `songId` TEXT NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        PRIMARY KEY(`playlistId`, `songId`)
                    )
                """)
            }
        }

        // Migration from version 8 to 9: Fix dateAdded and dateModified timestamps stored in seconds instead of milliseconds
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE songs SET dateAdded = dateAdded * 1000 WHERE dateAdded > 0 AND dateAdded < 100000000000")
                db.execSQL("UPDATE songs SET dateModified = dateModified * 1000 WHERE dateModified > 0 AND dateModified < 100000000000")
            }
        }

        fun getInstance(context: Context): RhythmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RhythmDatabase::class.java,
                    "rhythm_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
