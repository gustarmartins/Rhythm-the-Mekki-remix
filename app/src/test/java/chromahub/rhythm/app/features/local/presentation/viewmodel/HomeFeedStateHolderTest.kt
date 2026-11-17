/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.local.presentation.viewmodel

import android.app.Application
import android.net.Uri
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Song
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HomeFeedStateHolderTest {
    private fun song(id: String, artist: String = "Seed", albumId: String = "album-$artist") =
        Song(id, "Song $id", artist, "Greatest Hits", albumId, 180000, Uri.parse("content://media/$id"))

    private fun input() = HomeFeedInput(
        albums = (1..12).map { Album("a$it", "Album $it", "Artist $it") },
        songs = listOf(song("recent"), song("local"), song("similar", "Björk; Guest"), song("other", "Unrelated")),
        recentlyPlayed = listOf(song("recent")), favoriteIds = emptySet(),
        discoverCount = 6, recommendedCount = 3, showRecommendations = true,
        lastFmEnabled = true, lastFmKey = "test-key", splitArtists = true, artistDelimiters = ";"
    )

    private suspend fun HomeFeedStateHolder.awaitFeed() = withTimeout(5000) { refreshing.first { !it } }

    @Test fun returningToHomePreservesSelectionsAndDoesNotRequestLastFmAgain() = runBlocking {
        var calls = 0
        val holder = HomeFeedStateHolder(this, now = { 1000 }, random = Random(1)) { _, _ ->
            calls++; setOf("björk")
        }
        val source = input()
        holder.update(source); holder.awaitFeed()
        val original = holder.feed.value
        repeat(5) { holder.update(source.copy()); holder.awaitFeed() }
        assertSame(original, holder.feed.value)
        assertEquals(1, calls)
        assertEquals("similar", original.recommendations.first().id)
        assertTrue(original.usesLastFm)
    }

    @Test fun thresholdAndManualRefreshAreTheOnlyReasonsToRotateUnchangedContent() = runBlocking {
        var time = 1000L
        var calls = 0
        val holder = HomeFeedStateHolder(this, now = { time }, random = Random(2)) { _, _ -> calls++; setOf("björk") }
        val source = input()
        holder.update(source); holder.awaitFeed()
        val original = holder.feed.value
        time += HomeFeedStateHolder.FRESHNESS_MS - 1
        holder.update(source); holder.awaitFeed()
        assertSame(original, holder.feed.value)
        assertEquals(1, calls)
        time++
        holder.update(source); holder.awaitFeed()
        assertEquals(2, calls)
        holder.refresh(); holder.awaitFeed()
        assertEquals(3, calls)
        assertEquals(6, holder.feed.value.albums.size)
    }

    @Test fun metadataUpdatesAndCountChangesPreserveOrderAndReadCurrentObjects() = runBlocking {
        val holder = HomeFeedStateHolder(this, now = { 1000 }, random = Random(3)) { _, _ -> emptySet() }
        val source = input()
        holder.update(source); holder.awaitFeed()
        val ids = holder.feed.value.albums.map { it.id }
        val updated = source.copy(albums = source.albums.map { it.copy(title = "Updated ${it.id}") }, discoverCount = 8)
        holder.update(updated); holder.awaitFeed()
        assertEquals(ids, holder.feed.value.albums.take(6).map { it.id })
        assertTrue(holder.feed.value.albums.all { it.title.startsWith("Updated") })
        assertEquals(8, holder.feed.value.albums.size)
    }

    @Test fun metadataAndHistoryUpdatesDoNotKeepPostponingTheFreshnessDeadline() = runBlocking {
        var time = 1000L
        var calls = 0
        val holder = HomeFeedStateHolder(this, now = { time }) { _, _ -> calls++; setOf("björk") }
        val source = input()
        holder.update(source); holder.awaitFeed()
        time += HomeFeedStateHolder.FRESHNESS_MS / 2
        val updated = source.copy(albums = source.albums.map { it.copy(title = "Updated ${it.id}") },
            recentlyPlayed = source.recentlyPlayed.map { it.copy(title = "Updated title") })
        holder.update(updated); holder.awaitFeed()
        assertEquals(1, calls)
        time += HomeFeedStateHolder.FRESHNESS_MS / 2
        holder.update(updated); holder.awaitFeed()
        assertEquals(2, calls)
    }

    @Test fun libraryFiltersRemoveIneligibleSongsAndAlbumsWithoutReshufflingSurvivors() = runBlocking {
        val holder = HomeFeedStateHolder(this, now = { 1000 }, random = Random(4)) { _, _ -> setOf("björk") }
        val source = input()
        holder.update(source); holder.awaitFeed()
        val oldIds = holder.feed.value.albums.map { it.id }
        val removed = oldIds[2]
        holder.update(source.copy(albums = source.albums.filterNot { it.id == removed },
            songs = source.songs.filterNot { it.id == "similar" }))
        holder.awaitFeed()
        assertEquals(oldIds.filterNot { it == removed }, holder.feed.value.albums.take(5).map { it.id })
        assertFalse(holder.feed.value.recommendations.any { it.id == "similar" })
        assertFalse(holder.feed.value.usesLastFm)
    }

    @Test fun disablingLastFmOrChangingKeyDiscardsOldHintsAndUsesCurrentSettings() = runBlocking {
        val keys = mutableListOf<String>()
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { key, seed ->
            keys += key
            assertEquals("Seed", seed)
            setOf(if (key == "test-key") "björk" else "unrelated")
        }
        val source = input()
        holder.update(source); holder.awaitFeed()
        holder.update(source.copy(lastFmEnabled = false)); holder.awaitFeed()
        assertFalse(holder.feed.value.usesLastFm)
        assertEquals("local", holder.feed.value.recommendations.first().id)
        holder.update(source.copy(lastFmKey = "replacement-key")); holder.awaitFeed()
        assertEquals("other", holder.feed.value.recommendations.first().id)
        assertEquals(listOf("test-key", "replacement-key"), keys)
    }

    @Test fun offlineOrEmptyResultsKeepLocalRecommendationsAndDoNotRetryOnTabReturn() = runBlocking {
        var calls = 0
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { _, _ -> calls++; throw java.io.IOException("offline") }
        val source = input()
        holder.update(source); holder.awaitFeed()
        assertEquals("local", holder.feed.value.recommendations.first().id)
        assertFalse(holder.feed.value.usesLastFm)
        holder.update(source); holder.awaitFeed()
        assertEquals(1, calls)
    }

    @Test fun recentExclusionUsesIdsAndAlbumTitlesDoNotCreateFalseMatches() = runBlocking {
        val holder = HomeFeedStateHolder(this, now = { 1000 }, random = Random(6)) { _, _ -> emptySet() }
        val source = input().copy(recommendedCount = 10,
            recentlyPlayed = listOf(song("recent").copy(title = "Old metadata")))
        holder.update(source); holder.awaitFeed()
        assertEquals("local", holder.feed.value.recommendations.first().id)
        assertFalse(holder.feed.value.recommendations.any { it.id == "recent" })
        assertEquals(3, holder.feed.value.recommendations.size)
    }

    @Test fun hiddenRecommendationsNeverMakeNetworkRequests() = runBlocking {
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { _, _ -> error("Hidden section must stay idle") }
        holder.update(input().copy(showRecommendations = false)); holder.awaitFeed()
        assertTrue(holder.feed.value.recommendations.isEmpty())
        assertEquals(6, holder.feed.value.albums.size)
    }

    @Test fun replacedLookupCannotOverwriteNewSettings() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val blocked = CompletableDeferred<Set<String>>()
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { _, _ -> started.complete(Unit); blocked.await() }
        val source = input()
        holder.update(source)
        withTimeout(5000) { started.await() }
        assertTrue(holder.feed.value.recommendations.isNotEmpty())
        holder.update(source.copy(lastFmEnabled = false)); holder.awaitFeed()
        blocked.complete(setOf("björk"))
        assertFalse(holder.feed.value.usesLastFm)
        assertEquals("local", holder.feed.value.recommendations.first().id)
    }

    @Test fun libraryChangesDuringLookupAreReconciledWithoutCancellingOrRepeatingIt() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<Set<String>>()
        var calls = 0
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { _, _ ->
            calls++; started.complete(Unit); result.await()
        }
        val source = input()
        holder.update(source)
        withTimeout(5000) { started.await() }
        holder.update(source.copy(songs = source.songs.filterNot { it.id == "similar" }))
        result.complete(setOf("björk"))
        holder.awaitFeed()
        assertEquals(1, calls)
        assertFalse(holder.feed.value.recommendations.any { it.id == "similar" })
        assertFalse(holder.feed.value.usesLastFm)
    }

    @Test fun emptyLibraryAndMissingCredentialsNeverRequestLastFm() = runBlocking {
        val holder = HomeFeedStateHolder(this, now = { 1000 }) { _, _ -> error("No credentials or seed") }
        holder.update(input().copy(albums = emptyList(), songs = emptyList(), recentlyPlayed = emptyList()))
        holder.awaitFeed()
        assertTrue(holder.feed.value.albums.isEmpty())
        assertTrue(holder.feed.value.recommendations.isEmpty())
        holder.update(input().copy(lastFmKey = "")); holder.awaitFeed()
        assertEquals(3, holder.feed.value.recommendations.size)
        assertFalse(holder.feed.value.usesLastFm)
    }
}
