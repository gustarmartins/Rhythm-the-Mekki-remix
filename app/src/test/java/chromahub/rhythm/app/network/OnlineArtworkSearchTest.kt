package chromahub.rhythm.app.network

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class OnlineArtworkSearchTest {
    private val correct = "https://covers.test/no-gravity.jpg"
    private val unrelated = "https://covers.test/ai-cover.jpg"

    private fun track(
        title: String = "Impossible",
        artist: String = "Shontelle",
        album: String = "No Gravity",
        cover: String? = correct
    ) = DeezerTrack(1, title, DeezerTrackArtist(1, artist), DeezerTrackAlbum(1, album, coverXl = cover))

    private class Deezer(
        val tracks: List<DeezerTrack> = emptyList(),
        val failure: Exception? = null
    ) : DeezerApiService {
        val queries = mutableListOf<String>()
        override suspend fun searchTracks(query: String, limit: Int): DeezerTrackSearchResponse {
            queries += query
            failure?.let { throw it }
            return DeezerTrackSearchResponse(tracks)
        }
        override suspend fun searchAlbums(query: String, limit: Int): DeezerAlbumSearchResponse =
            error("A track lookup must not search albums")
        override suspend fun searchArtists(query: String, limit: Int): DeezerSearchResponse =
            error("A track lookup must not search artists")
    }

    private class Youtube(
        val result: YTMusicSearchResponse,
        val failure: Exception? = null,
        val status: Int = 200
    ) : YTMusicApiService {
        val requests = mutableListOf<YTMusicSearchRequest>()
        override suspend fun search(apiKey: String, request: YTMusicSearchRequest): Response<YTMusicSearchResponse> {
            requests += request
            failure?.let { throw it }
            return if (status == 200) Response.success(result) else Response.error(status, "unavailable".toResponseBody())
        }
        override suspend fun getArtist(apiKey: String, request: YTMusicBrowseRequest): Response<YTMusicArtistResponse> = error("Unused")
        override suspend fun getAlbum(apiKey: String, request: YTMusicBrowseRequest): Response<YTMusicAlbumResponse> = error("Unused")
    }

    private fun item(
        title: String = "Impossible",
        artist: String = "Shontelle",
        cover: String? = correct,
        artistId: String? = "UCartist",
        album: String = "No Gravity"
    ): YTMusicListItem {
        fun run(text: String, browseId: String? = null) = YTMusicTextRun(
            text, browseId?.let { YTMusicNavigationEndpoint(YTMusicBrowseEndpoint(it)) }
        )
        fun column(vararg runs: YTMusicTextRun) = YTMusicFlexColumn(YTMusicFlexColumnContent(YTMusicText(runs.toList())))
        return YTMusicListItem(
            flexColumns = listOf(column(run(title)), column(run(artist, artistId), run(" • "), run(album, "MPREalbum"))),
            thumbnail = cover?.let { YTMusicThumbnailContainer(YTMusicThumbnailRenderer(YTMusicThumbnails(listOf(
                YTMusicThumbnail("https://covers.test/small.jpg", 60, 60), YTMusicThumbnail(it, 600, 600)
            )))) },
            navigationEndpoint = null,
            menu = null
        )
    }

    private fun response(vararg items: YTMusicListItem) = YTMusicSearchResponse(YTMusicSearchContents(
        YTMusicTabbedSearchResults(listOf(YTMusicSearchTab(YTMusicTabRenderer(YTMusicSectionListRenderer(
            YTMusicSectionListContents(listOf(YTMusicMusicShelfRenderer(YTMusicShelfContents(
                items.map { YTMusicResponsiveListItemRenderer(it) }
            ))))
        )))))
    ))

    @Test fun screenshotArtistSubstringCannotWinOverTheActualTrack() = runBlocking {
        val deezer = Deezer(listOf(track(artist = "Shontelle Layne", cover = unrelated), track()))
        assertEquals(correct, findOnlineSongArtwork("Impossible", "Shontelle", "Nightcore Mix", deezer, null))
        assertEquals(listOf("Impossible Shontelle"), deezer.queries)
    }

    @Test fun noFirstResultFallbackWhenEveryCandidateIsWrong() = runBlocking {
        val deezer = Deezer(listOf(
            track(artist = "Shontelle Layne"), track(artist = "James Arthur"),
            track(title = "Impossible (Coco & Breezy Remix)"), track(title = "Another Song")
        ))
        assertNull(findOnlineSongArtwork("Impossible", "Shontelle", "Nightcore Mix", deezer, null))
    }

    @Test fun albumPreferenceOnlyRanksAlreadyMatchingTracks() {
        val results = DeezerTrackSearchResponse(listOf(
            track(album = "Compilation", cover = unrelated),
            track(artist = "Shontelle Layne", album = "No Gravity", cover = unrelated), track()
        ))
        assertEquals(correct, results.extractVerifiedArtwork("Impossible", "Shontelle", "No Gravity"))
    }

    @Test fun unknownArtistOrEmptyTitleCannotAuthorizeArtwork() = runBlocking {
        val deezer = Deezer(listOf(track()))
        val youtube = Youtube(response(item()))
        for (artist in listOf("", "Unknown", "Unknown Artist", "Various Artists", "<unknown>")) {
            assertNull(findOnlineSongArtwork("Impossible", artist, "", deezer, youtube))
        }
        assertNull(findOnlineSongArtwork("!!!", "Shontelle", "", deezer, youtube))
        assertTrue(deezer.queries.isEmpty())
        assertTrue(youtube.requests.isEmpty())
    }

    @Test fun missingImagesAreSkippedAndBlankHighResolutionUrlsFallBack() {
        val lowerResolution = track().copy(album = DeezerTrackAlbum(2, "No Gravity", coverXl = " ", coverBig = correct))
        val results = DeezerTrackSearchResponse(listOf(track(cover = null), lowerResolution))
        assertEquals(correct, results.extractVerifiedArtwork("Impossible", "Shontelle", "Nightcore Mix"))
    }

    @Test fun liveDeezerFieldNamesDeserializeToTheTrackCover() {
        val result = Gson().fromJson("""{"data":[{"id":1,"title":"Impossible",
            "artist":{"id":1,"name":"Shontelle"},"album":{"id":1,"title":"No Gravity",
            "cover_xl":"$correct","cover_big":"$unrelated"}}]}""", DeezerTrackSearchResponse::class.java)
        assertEquals(correct, result.extractVerifiedArtwork("Impossible", "Shontelle", "Nightcore Mix"))
    }

    private fun recordedYoutube(kind: String): YTMusicSearchResponse =
        checkNotNull(javaClass.getResourceAsStream("/artwork/ytmusic-$kind.json"))
            .bufferedReader().use { Gson().fromJson(it, YTMusicSearchResponse::class.java) }

    @Test fun recordedSongResponseParsesThroughTheRealProviderStructure() {
        // Public response captured 2026-11-01; tracking fields removed and image URLs replaced.
        assertEquals(correct, recordedYoutube("track").extractVerifiedArtwork("Impossible", "Shontelle"))
        assertNull(recordedYoutube("track").extractVerifiedArtwork("Impossible", "Other Artist"))
    }

    @Test fun recordedAlbumResponseParsesThroughTheRealProviderStructure() {
        assertEquals(correct, recordedYoutube("album").extractVerifiedArtwork("No Gravity", "Shontelle"))
        assertNull(recordedYoutube("album").extractVerifiedArtwork("Nightcore Mix", "Shontelle"))
    }

    @Test fun verifiedDeezerMatchDoesNotContactYoutube() = runBlocking {
        val youtube = Youtube(response(item()))
        assertEquals(correct, findOnlineSongArtwork("Impossible", "Shontelle", "", Deezer(listOf(track())), youtube))
        assertTrue(youtube.requests.isEmpty())
    }

    @Test fun providerFailureAndMismatchesCanUseOnlyAVerifiedFallback() = runBlocking {
        for (deezer in listOf(Deezer(failure = IOException("offline")), Deezer(listOf(track(artist = "Shontelle Layne"))))) {
            val youtube = Youtube(response(item(artist = "Shontelle Layne", cover = unrelated), item()))
            assertEquals(correct, findOnlineSongArtwork("Impossible", "Shontelle", "Nightcore Mix", deezer, youtube))
            assertEquals("Impossible Shontelle", youtube.requests.single().query)
            assertEquals("EgWKAQIIAWoKEAoQAxAEEAkQBQ%3D%3D", youtube.requests.single().params)
        }
    }

    @Test fun disabledProvidersAndHttpErrorsLeaveTheSelectionAlone() = runBlocking {
        assertNull(findOnlineSongArtwork("Impossible", "Shontelle", "", null, null))
        assertNull(findOnlineSongArtwork("Impossible", "Shontelle", "", null, Youtube(response(item()), status = 503)))
    }

    @Test fun cancellationDoesNotFallThroughToAnotherProviderOrReportFailure() = runBlocking {
        val youtube = Youtube(response(item()))
        val errors = mutableListOf<Exception>()
        val result = runCatching {
            findOnlineSongArtwork("Impossible", "Shontelle", "", Deezer(failure = CancellationException("cancel")), youtube) { _, e -> errors += e }
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertTrue(errors.isEmpty())
        assertTrue(youtube.requests.isEmpty())
    }

    @Test fun youtubeCancellationPropagates() = runBlocking {
        val result = runCatching {
            findOnlineSongArtwork("Impossible", "Shontelle", "", null, Youtube(response(item()), CancellationException("cancel")))
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test fun youtubeAlbumOrUnlinkedTextCannotMasqueradeAsTheArtist() {
        assertNull(response(item(artist = "Other Artist", album = "Shontelle")).extractVerifiedArtwork("Impossible", "Shontelle"))
        assertNull(response(item(artistId = null)).extractVerifiedArtwork("Impossible", "Shontelle"))
        assertNull(response(item(artistId = "MPREalbum")).extractVerifiedArtwork("Impossible", "Shontelle"))
    }

    @Test fun liveCatalogSongLayoutCanIdentifyTheUnlinkedArtist() {
        val endpoint = Gson().fromJson("""{"watchEndpoint":{"videoId":"catalog-track",
            "watchEndpointMusicSupportedConfigs":{"watchEndpointMusicConfig":{
            "musicVideoType":"MUSIC_VIDEO_TYPE_ATV"}}}}""", YTMusicNavigationEndpoint::class.java)
        val catalogTrack = item(artistId = null).copy(navigationEndpoint = endpoint)
        assertEquals(correct, response(catalogTrack).extractVerifiedArtwork("Impossible", "Shontelle"))
        val wrongArtist = item(artist = "Shontelle Layne", artistId = null, album = "Shontelle")
            .copy(navigationEndpoint = endpoint)
        assertNull(response(wrongArtist).extractVerifiedArtwork("Impossible", "Shontelle"))
    }

    @Test fun unlinkedVideoUploadersAreNotCatalogArtists() {
        val endpoint = YTMusicNavigationEndpoint(null, YTMusicWatchEndpoint(YTMusicWatchConfigs(
            YTMusicWatchConfig("MUSIC_VIDEO_TYPE_UGC")
        )))
        assertNull(response(item(artistId = null).copy(navigationEndpoint = endpoint))
            .extractVerifiedArtwork("Impossible", "Shontelle"))
    }

    @Test fun youtubeRejectsCoversRemixesAndUnrelatedFirstResults() {
        assertNull(response(item(artist = "Shontelle Layne"), item(title = "Impossible (Remix)"))
            .extractVerifiedArtwork("Impossible", "Shontelle"))
        assertEquals(correct, response(item(artist = "Other Artist", cover = unrelated), item())
            .extractVerifiedArtwork("Impossible", "Shontelle"))
    }

    @Test fun youtubeSkipsMatchingRowsWithoutArtwork() {
        assertEquals(correct, response(item(cover = null), item()).extractVerifiedArtwork("Impossible", "Shontelle"))
    }

    @Test fun albumEditorsRequireTheAlbumTitleAsWellAsItsArtist() {
        assertNull(response(item(title = "Impossible")).extractVerifiedArtwork("Nightcore Mix", "Shontelle"))
        assertEquals(correct, response(item(title = "No Gravity")).extractVerifiedArtwork("No Gravity", "Shontelle"))
        assertNull(response(item(title = "No Gravity", artist = "Shontelle Layne")).extractVerifiedArtwork("No Gravity", "Shontelle"))
    }

    @Test fun unicodeAndCaseNormalizationStillAllowsExactIdentities() {
        assertEquals(correct, response(item(title = "Café", artist = "Björk")).extractVerifiedArtwork("CAFE", "BJORK"))
        assertEquals(correct, response(item(title = "東京", artist = "宇多田ヒカル")).extractVerifiedArtwork("東京", "宇多田ヒカル"))
    }
}
