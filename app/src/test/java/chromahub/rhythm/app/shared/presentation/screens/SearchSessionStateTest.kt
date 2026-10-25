/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.screens

import android.os.Bundle
import android.os.Parcel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class SearchSessionStateTest {
    @Test fun playerNavigationAndProcessRestorationKeepSearchContextAndEveryScrollPosition() {
        val original = SearchSessionState(
            query = "Twice Brave", showFilters = true, filterAlbums = false,
            filterPlaylists = false, showAllSongs = true,
            results = LazyListState(7, 21), songResults = LazyListState(3, 12),
            otherResults = LazyListState(2, 16), allSongs = LazyListState(29, 31),
            albums = LazyListState(2, 5), artists = LazyListState(4, 9)
        )
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(SearchSessionState.Saver) { scope.save(original) }!!
        // Exercise the same Bundle/Parcel boundary used for an Android saved back-stack entry.
        val bundle = Bundle().apply { putSerializable("search", saved as java.io.Serializable) }
        val parcel = Parcel.obtain()
        val restored = try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            val roundTrip = parcel.readBundle(javaClass.classLoader)!!
            @Suppress("DEPRECATION")
            SearchSessionState.Saver.restore(roundTrip.getSerializable("search")!!)!!
        } finally { parcel.recycle() }
        assertEquals("Twice Brave", restored.query)
        assertTrue(restored.showFilters)
        assertTrue(restored.filterSongs)
        assertFalse(restored.filterAlbums)
        assertTrue(restored.filterArtists)
        assertFalse(restored.filterPlaylists)
        assertTrue(restored.showAllSongs)
        assertEquals(listOf(7, 3, 2, 29, 2, 4),
            listOf(restored.results, restored.songResults, restored.otherResults,
                restored.allSongs, restored.albums, restored.artists).map { it.firstVisibleItemIndex })
        assertEquals(listOf(21, 12, 16, 31, 5, 9),
            listOf(restored.results, restored.songResults, restored.otherResults,
                restored.allSongs, restored.albums, restored.artists).map { it.firstVisibleItemScrollOffset })
    }
}
