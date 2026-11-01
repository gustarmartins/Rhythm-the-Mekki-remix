package chromahub.rhythm.app.network

import chromahub.rhythm.app.util.ArtworkSourcePolicy
import kotlinx.coroutines.CancellationException

/** Custom compilation tags may differ, but neither the track nor artist may be guessed. */
internal fun DeezerTrackSearchResponse.extractVerifiedArtwork(
    expectedTitle: String,
    expectedArtist: String,
    expectedAlbum: String
): String? = data.asSequence()
    .filter { ArtworkSourcePolicy.matches(expectedTitle, expectedArtist, it.title, it.artist?.name) }
    .sortedByDescending {
        ArtworkSourcePolicy.matches(expectedAlbum, expectedArtist, it.album?.title, it.artist?.name)
    }
    .mapNotNull { track ->
        track.album?.run { listOf(coverXl, coverBig, coverMedium, cover).firstOrNull { !it.isNullOrBlank() } }
    }
    .firstOrNull()

/** Shared by explicit track editing and automatic track artwork lookup. */
internal suspend fun findOnlineSongArtwork(
    title: String,
    artist: String,
    album: String,
    deezer: DeezerApiService?,
    youtube: YTMusicApiService?,
    onProviderFailure: (String, Exception) -> Unit = { _, _ -> }
): String? {
    if (!ArtworkSourcePolicy.matches(title, artist, title, artist)) return null
    val query = "${title.trim()} ${artist.trim()}"
    if (deezer != null) {
        try {
            deezer.searchTracks(query).extractVerifiedArtwork(title, artist, album)?.let { return it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onProviderFailure("Deezer", e)
        }
    }
    if (youtube != null) {
        try {
            val response = youtube.search(request = YTMusicSearchRequest(
                context = YTMusicContext(YTMusicClient()),
                query = query,
                params = "EgWKAQIIAWoKEAoQAxAEEAkQBQ%3D%3D"
            ))
            if (response.isSuccessful) return response.body()?.extractVerifiedArtwork(title, artist)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onProviderFailure("YouTube Music", e)
        }
    }
    return null
}
