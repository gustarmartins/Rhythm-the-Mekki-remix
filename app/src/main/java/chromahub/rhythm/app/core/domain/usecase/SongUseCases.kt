/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.core.domain.usecase

import chromahub.rhythm.app.core.domain.model.PlayableItem
import chromahub.rhythm.app.core.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all songs from a music repository.
 */
class GetSongsUseCase(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<PlayableItem>> = repository.getSongs()
}

/**
 * Use case for searching songs.
 */
class SearchSongsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(query: String): List<PlayableItem> =
        repository.searchSongs(query)
}

/**
 * Use case for getting a song by ID.
 */
class GetSongByIdUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(songId: String): PlayableItem? =
        repository.getSongById(songId)
}
