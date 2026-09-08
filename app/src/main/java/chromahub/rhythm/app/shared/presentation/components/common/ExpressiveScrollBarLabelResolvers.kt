/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.screens.ArtistSortOption
import chromahub.rhythm.app.features.local.presentation.screens.LibraryPlaylistSortOrder
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistSortOrder

fun songFastScrollLabel(song: Song?, sortOrder: MusicViewModel.SortOrder): String? =
    when (sortOrder) {
        MusicViewModel.SortOrder.TITLE_ASC,
        MusicViewModel.SortOrder.TITLE_DESC -> extractFastScrollGlyph(song?.title)

        MusicViewModel.SortOrder.ARTIST_ASC,
        MusicViewModel.SortOrder.ARTIST_DESC -> extractFastScrollGlyph(song?.artist)

        MusicViewModel.SortOrder.ALBUM_ASC,
        MusicViewModel.SortOrder.ALBUM_DESC -> extractFastScrollGlyph(song?.album)

        else -> null
    }

fun albumFastScrollLabel(album: Album?, sortOrder: MusicViewModel.SortOrder): String? =
    when (sortOrder) {
        MusicViewModel.SortOrder.TITLE_ASC,
        MusicViewModel.SortOrder.TITLE_DESC,
        MusicViewModel.SortOrder.ALBUM_ASC,
        MusicViewModel.SortOrder.ALBUM_DESC -> extractFastScrollGlyph(album?.title)

        MusicViewModel.SortOrder.ARTIST_ASC,
        MusicViewModel.SortOrder.ARTIST_DESC -> extractFastScrollGlyph(album?.artist)

        else -> null
    }

fun artistFastScrollLabel(artist: Artist?, sortOrder: ArtistSortOption): String? =
    when (sortOrder) {
        ArtistSortOption.NAME_ASC,
        ArtistSortOption.NAME_DESC -> extractFastScrollGlyph(artist?.name)
        else -> null
    }

fun playlistFastScrollLabel(playlist: Playlist?, sortOrder: LibraryPlaylistSortOrder): String? =
    when (sortOrder) {
        LibraryPlaylistSortOrder.NAME_ASC,
        LibraryPlaylistSortOrder.NAME_DESC -> extractFastScrollGlyph(playlist?.name)
        else -> null
    }

fun playlistDetailFastScrollLabel(song: Song?, sortOrder: PlaylistSortOrder): String? =
    when (sortOrder) {
        PlaylistSortOrder.TITLE_ASC,
        PlaylistSortOrder.TITLE_DESC -> extractFastScrollGlyph(song?.title)

        PlaylistSortOrder.ARTIST_ASC,
        PlaylistSortOrder.ARTIST_DESC -> extractFastScrollGlyph(song?.artist)

        PlaylistSortOrder.ALBUM_ASC,
        PlaylistSortOrder.ALBUM_DESC -> extractFastScrollGlyph(song?.album)

        else -> null
    }
