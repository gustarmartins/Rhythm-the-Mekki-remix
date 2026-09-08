/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "song_artists",
    primaryKeys = ["songId", "artistName", "groupByAlbumArtist"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("songId"),
        Index("artistName"),
        Index("groupByAlbumArtist")
    ]
)
data class SongArtistEntity(
    val songId: String,
    val artistName: String,
    val groupByAlbumArtist: Boolean
)
