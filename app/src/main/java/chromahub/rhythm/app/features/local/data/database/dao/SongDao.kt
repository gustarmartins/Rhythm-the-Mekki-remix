/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import chromahub.rhythm.app.features.local.data.database.entity.SongEntity

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM songs")
    fun getAllSongsFlow(): kotlinx.coroutines.flow.Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE duration >= :minDurationMs ORDER BY title ASC")
    fun getSongsFilteredFlow(minDurationMs: Long = 0L): kotlinx.coroutines.flow.Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getSongsPagingSource(): androidx.paging.PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs WHERE duration >= :minDurationMs AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%') ORDER BY title ASC")
    fun getSongsPagingSourceSearch(query: String, minDurationMs: Long = 0L): androidx.paging.PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    /** Metadata workers may update the current library, never recreate removed or unscanned rows. */
    @Update
    suspend fun updateExisting(songs: List<SongEntity>)

    @Transaction
    suspend fun updateCurrentMetadata(songs: List<SongEntity>): List<SongEntity> {
        val current = getSongsByIds(songs.map { it.id }).associateBy { it.id }
        val accepted = songs.filter { incoming ->
            current[incoming.id]?.let { existing ->
                existing != incoming && existing.uri == incoming.uri && existing.path == incoming.path &&
                    existing.dateModified == incoming.dateModified
            } == true
        }
        updateExisting(accepted)
        return accepted
    }

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteByIds(songIds: List<String>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE artworkUri LIKE '%embedded_artwork%'")
    suspend fun countSongsWithEmbeddedArtworkUri(): Int

    @Transaction
    suspend fun replaceAll(songs: List<SongEntity>) {
        deleteAll()
        insertAll(songs)
    }
}
