/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.local.presentation.viewmodel

import android.os.SystemClock
import chromahub.rhythm.app.infrastructure.network.LastFmRecommendations
import chromahub.rhythm.app.infrastructure.network.recommendationArtistKey
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.ArtistSeparator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal data class HomeFeedInput(
    val albums: List<Album>,
    val songs: List<Song>,
    val recentlyPlayed: List<Song>,
    val favoriteIds: Set<String>,
    val discoverCount: Int,
    val recommendedCount: Int,
    val showRecommendations: Boolean,
    val lastFmEnabled: Boolean,
    val lastFmKey: String,
    val splitArtists: Boolean,
    val artistDelimiters: String
)

internal data class HomeFeedSnapshot(
    val albums: List<Album> = emptyList(),
    val recommendations: List<Song> = emptyList(),
    val usesLastFm: Boolean = false
)

/** A Home session belongs to the ViewModel, not a lazy item or a navigation composition. */
internal class HomeFeedStateHolder(
    private val scope: CoroutineScope,
    private val now: () -> Long = SystemClock::elapsedRealtime,
    private val random: Random = Random.Default,
    private val similarArtists: suspend (String, String) -> Set<String> = { key, artist ->
        LastFmRecommendations.shared.similarArtists(key, artist)
    }
) {
    private val _feed = MutableStateFlow(HomeFeedSnapshot())
    val feed = _feed.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private var input: HomeFeedInput? = null
    private var refreshedAt: Long? = null
    private var job: Job? = null
    private var generation = 0L
    private var relatedArtists = emptySet<String>()

    fun update(next: HomeFeedInput, force: Boolean = false) {
        val previous = input
        val timestamp = now()
        val expired = refreshedAt?.let { timestamp < it || timestamp - it >= FRESHNESS_MS } ?: true
        if (!force && !expired && next == previous) return
        input = next
        val rotate = force || expired
        val recommendationSettingsChanged = previous == null ||
            previous.lastFmEnabled != next.lastFmEnabled || previous.lastFmKey != next.lastFmKey ||
            previous.splitArtists != next.splitArtists || previous.artistDelimiters != next.artistDelimiters ||
            previous.showRecommendations != next.showRecommendations
        // Metadata/history emissions can arrive during the lookup. Coalesce them into its
        // final reconciliation instead of cancelling the same request and hitting the throttle.
        if (job?.isActive == true && !force && !recommendationSettingsChanged) return
        val refreshRecommendations = rotate || recommendationSettingsChanged ||
            (_feed.value.recommendations.isEmpty() && next.songs.isNotEmpty())
        if (recommendationSettingsChanged) relatedArtists = emptySet()
        job?.cancel()
        val request = ++generation
        _refreshing.value = true
        job = scope.launch {
            suspend fun publish(rotateAlbums: Boolean, rotateRecommendations: Boolean) {
                while (request == generation) {
                    val source = input ?: return
                    val selected = withContext(Dispatchers.Default) {
                        select(source, rotateAlbums, rotateRecommendations)
                    }
                    if (request != generation) return
                    if (source !== input) continue
                    _feed.value = selected
                    return
                }
            }
            try {
                publish(rotate, refreshRecommendations)
                if (request != generation) return@launch
                // Rebinding metadata/history must not keep extending the rotation deadline.
                if (rotate && (next.albums.isNotEmpty() || next.songs.isNotEmpty())) refreshedAt = timestamp
                // Local results stay available while this optional, bounded request is pending.
                if (refreshRecommendations && next.showRecommendations && next.lastFmEnabled && next.lastFmKey.isNotBlank()) {
                    val seed = withContext(Dispatchers.Default) { recommendationSeed(input ?: next) }
                    val related = if (seed != null) similarArtists(next.lastFmKey, seed) else emptySet()
                    if (request != generation) return@launch
                    relatedArtists = related
                    publish(rotateAlbums = false, rotateRecommendations = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the already published local feed. Never log credentials or request URLs.
                publish(rotateAlbums = false, rotateRecommendations = false)
            } finally {
                if (request == generation) _refreshing.value = false
            }
        }
    }

    fun refresh() { input?.let { update(it, force = true) } }

    private fun artistNames(song: Song, input: HomeFeedInput): List<String> =
        ArtistSeparator.splitArtistNames(song.artist, input.artistDelimiters, input.splitArtists)

    private fun recommendationSeed(input: HomeFeedInput): String? =
        (input.recentlyPlayed.asSequence() + input.songs.asSequence().filter { it.id in input.favoriteIds })
            .flatMap { artistNames(it, input).asSequence() }
            .firstOrNull { recommendationArtistKey(it) !in UNKNOWN_ARTISTS }

    private fun select(input: HomeFeedInput, rotateAlbums: Boolean, rotateRecommendations: Boolean): HomeFeedSnapshot {
        val old = _feed.value
        val byAlbumId = input.albums.associateBy { it.id }
        val keptAlbums = if (rotateAlbums) emptyList() else old.albums.mapNotNull { byAlbumId[it.id] }
        val keptAlbumIds = keptAlbums.mapTo(hashSetOf()) { it.id }
        val albums = (keptAlbums + input.albums.filter { it.id !in keptAlbumIds }.shuffled(random))
            .take(input.discoverCount.coerceAtLeast(1))
        if (!input.showRecommendations) return HomeFeedSnapshot(albums)

        val recentIds = input.recentlyPlayed.mapTo(hashSetOf()) { it.id }
        val seeds = input.recentlyPlayed + input.songs.filter { it.id in input.favoriteIds }
        val seedArtists = seeds.flatMap { artistNames(it, input) }.map(::recommendationArtistKey)
            .filterNot { it in UNKNOWN_ARTISTS }.toSet()
        // An album title alone is not an identity (many unrelated artists release "Greatest Hits").
        val seedAlbums = seeds.mapNotNull { it.albumId.takeIf(String::isNotBlank) }.toSet()
        val namesById = input.songs.associate { song ->
            song.id to artistNames(song, input).map(::recommendationArtistKey).toSet()
        }
        fun score(song: Song): Int = when {
            namesById[song.id].orEmpty().any { it in relatedArtists } -> 4
            namesById[song.id].orEmpty().any { it in seedArtists } -> 3
            song.albumId.isNotBlank() && song.albumId in seedAlbums -> 2
            song.id in input.favoriteIds -> 1
            else -> 0
        }
        // Keep recent songs out while there are unheard alternatives, comparing occurrence-independent IDs.
        val candidates = input.songs.distinctBy { it.id }.filter { it.id !in recentIds }
            .ifEmpty { input.songs.distinctBy { it.id } }
        val byId = candidates.associateBy { it.id }
        val kept = if (rotateRecommendations) emptyList() else old.recommendations.mapNotNull { byId[it.id] }
        val keptIds = kept.mapTo(hashSetOf()) { it.id }
        val recommendations = (kept + candidates.filter { it.id !in keptIds }.shuffled(random).sortedByDescending(::score))
            .take(input.recommendedCount.coerceAtLeast(1))
        return HomeFeedSnapshot(albums, recommendations,
            input.lastFmEnabled && recommendations.any { namesById[it.id].orEmpty().any { name -> name in relatedArtists } })
    }

    companion object {
        const val FRESHNESS_MS = 6 * 60 * 60_000L
        private val UNKNOWN_ARTISTS = setOf("", "unknown", "<unknown>", "unknown artist", "various artists")
    }
}
