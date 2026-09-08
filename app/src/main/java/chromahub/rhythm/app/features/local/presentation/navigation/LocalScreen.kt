/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.presentation.navigation

/**
 * Navigation screens for the local music feature.
 */
sealed class LocalScreen(val route: String) {
    
    /**
     * Local home screen - shows recently played, quick picks, etc.
     */
    object Home : LocalScreen("local_home")
    
    /**
     * Local library screen - shows all songs, albums, artists, playlists.
     */
    object Library : LocalScreen("local_library?tab={tab}") {
        fun createRoute(tab: LocalLibraryTab = LocalLibraryTab.SONGS): String = 
            "local_library?tab=${tab.name.lowercase()}"
    }
    
    /**
     * Local search screen - search within local library.
     */
    object Search : LocalScreen("local_search")
    
    /**
     * Local player screen - full screen player.
     */
    object Player : LocalScreen("local_player")
    
    /**
     * Album detail screen.
     */
    object AlbumDetail : LocalScreen("local_album/{albumId}") {
        fun createRoute(albumId: String) = "local_album/$albumId"
    }
    
    /**
     * Artist detail screen.
     */
    object ArtistDetail : LocalScreen("local_artist/{artistId}") {
        fun createRoute(artistId: String) = "local_artist/$artistId"
    }
    
    /**
     * Playlist detail screen.
     */
    object PlaylistDetail : LocalScreen("local_playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "local_playlist/$playlistId"
    }
    
    /**
     * Folder explorer screen.
     */
    object FolderExplorer : LocalScreen("local_folders/{path}") {
        fun createRoute(path: String = "") = "local_folders/$path"
    }
    
    /**
     * Equalizer screen.
     */
    object Equalizer : LocalScreen("local_equalizer")
    
    /**
     * Listening stats screen.
     */
    object Stats : LocalScreen("local_stats")
}

/**
 * Tabs available in the local library screen.
 */
enum class LocalLibraryTab(val displayName: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    EXPLORER("Folders")
}
