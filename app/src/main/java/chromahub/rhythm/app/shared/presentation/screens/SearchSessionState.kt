/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver

/** Owned by the search back-stack entry, including the nested "all songs" page. */
@Stable
internal class SearchSessionState(
    query: String = "",
    showFilters: Boolean = false,
    filterSongs: Boolean = true,
    filterAlbums: Boolean = true,
    filterArtists: Boolean = true,
    filterPlaylists: Boolean = true,
    showAllSongs: Boolean = false,
    val results: LazyListState = LazyListState(),
    val songResults: LazyListState = LazyListState(),
    val otherResults: LazyListState = LazyListState(),
    val allSongs: LazyListState = LazyListState(),
    val albums: LazyListState = LazyListState(),
    val artists: LazyListState = LazyListState()
) {
    var query by mutableStateOf(query)
    var showFilters by mutableStateOf(showFilters)
    var filterSongs by mutableStateOf(filterSongs)
    var filterAlbums by mutableStateOf(filterAlbums)
    var filterArtists by mutableStateOf(filterArtists)
    var filterPlaylists by mutableStateOf(filterPlaylists)
    var showAllSongs by mutableStateOf(showAllSongs)

    companion object {
        val Saver = listSaver<SearchSessionState, Any>(
            save = { state ->
                listOf(state.query, state.showFilters, state.filterSongs, state.filterAlbums,
                    state.filterArtists, state.filterPlaylists, state.showAllSongs) +
                    listOf(state.results, state.songResults, state.otherResults, state.allSongs,
                        state.albums, state.artists).flatMap {
                        listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset)
                    }
            },
            restore = { saved ->
                fun list(offset: Int) = LazyListState(saved[offset] as Int, saved[offset + 1] as Int)
                SearchSessionState(saved[0] as String, saved[1] as Boolean, saved[2] as Boolean,
                    saved[3] as Boolean, saved[4] as Boolean, saved[5] as Boolean, saved[6] as Boolean,
                    list(7), list(9), list(11), list(13), list(15), list(17))
            }
        )
    }
}
