package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LibraryQueueContinuationTest {
    @Test fun shortMostPlayedContextContinuesWithRemainingDeviceLibrary() {
        val library = (1..1905).map { "song$it" }
        val plan = planLibraryContinuation(library, library.take(5), "song5")
        assertEquals(1900, plan.songIds.size)
        assertEquals(library.drop(5).toSet(), plan.songIds.toSet())
        assertNotEquals(library.drop(5), plan.songIds)
        assertFalse(plan.startsNewCycle)
    }
    @Test fun queuedManualRequestsAreNotDuplicatedByTheLibraryPool() {
        val plan = planLibraryContinuation(listOf("Medicina", "Jump", "Brave", "Jet Rocket", "More"),
            listOf("Medicina", "Jump", "Brave", "Jet Rocket"), "Medicina")
        assertEquals(listOf("More"), plan.songIds)
    }
    @Test fun exhaustedLibraryStartsAnotherCycleWithoutImmediatelyRepeatingCurrentSong() {
        val library = listOf("one", "two", "three")
        val plan = planLibraryContinuation(library, listOf("three"), "three", playedIds = library.toSet())
        assertEquals(setOf("one", "two"), plan.songIds.toSet())
        assertTrue(plan.startsNewCycle)
    }
    @Test fun aSingleAvailableSongCanContinuePlaying() {
        assertEquals(listOf("only"), planLibraryContinuation(listOf("only"), listOf("only"), "only").songIds)
    }
    @Test fun removedOrUnavailableSongsDoNotReappear() {
        assertEquals(listOf("available"), planLibraryContinuation(listOf("available"), listOf("missing"), "missing").songIds)
    }
    @Test fun emptyLibraryDoesNotClearOrReplaceExistingQueue() {
        assertTrue(planLibraryContinuation(emptyList(), listOf("queued"), "queued").songIds.isEmpty())
    }
    @Test fun duplicateDatabaseIdsDoNotInflateTheContinuation() {
        assertEquals(listOf("next"), planLibraryContinuation(listOf("next", "next"), listOf("current"), "current").songIds)
    }

    @Test fun sameSearchLaunchDoesNotRecreateAnAlphabeticalOrPopularitySequence() {
        val library = (1..2000).map { "song$it" }
        val first = planLibraryContinuation(library, listOf("song700"), "song700", random = Random(1))
        val second = planLibraryContinuation(library, listOf("song700"), "song700", random = Random(2))
        assertEquals(1999, first.songIds.size)
        assertEquals(first.songIds.toSet(), second.songIds.toSet())
        assertNotEquals(first.songIds.take(50), second.songIds.take(50))
    }

    @Test fun replacingSearchQueuesStillTraversesAllTwoThousandSongsBeforeRepeating() {
        val library = (1..2000).map { "song$it" }
        val played = linkedSetOf("song1")
        var current = "song1"
        repeat(41) { launch ->
            val plan = planLibraryContinuation(library, listOf(current), current,
                playedIds = played.toSet(), allowNewCycle = false, random = Random(launch))
            val heard = plan.songIds.take(49)
            assertTrue(heard.none { it in played })
            played.addAll(heard)
            current = heard.lastOrNull() ?: current
        }
        assertEquals(library.toSet(), played)
        val next = planLibraryContinuation(library, listOf(current), current,
            playedIds = played, random = Random(42))
        assertTrue(next.startsNewCycle)
        assertEquals(1999, next.songIds.size)
        assertFalse(current in next.songIds)
    }

    @Test fun offeringATailDoesNotMarkUnplayedSongsAsHeard() {
        val library = (1..2000).map { "song$it" }
        val played = setOf("song1", "song2")
        val offered = planLibraryContinuation(library, listOf("song2"), "song2", playedIds = played)
        val replacement = planLibraryContinuation(library, listOf("song3"), "song3", playedIds = played)
        assertEquals(offered.songIds.toSet() - "song3", replacement.songIds.toSet())
    }

    @Test fun compactedHistoryAndNewQueueKeepCycleExclusions() {
        val library = (1..2000).map { "song$it" }
        val plan = planLibraryContinuation(library, listOf("song1500"), "song1500",
            playedIds = library.take(1500).toSet())
        assertEquals(library.drop(1500).toSet(), plan.songIds.toSet())
        assertFalse(plan.startsNewCycle)
    }

    @Test fun contextBuilderDoesNotResetCycleWhileItsListStillHasUnplayedSongs() {
        val plan = planLibraryContinuation(listOf("a", "b", "c"), listOf("b", "c"), "b",
            playedIds = setOf("a"), allowNewCycle = false)
        assertTrue(plan.songIds.isEmpty())
        assertFalse(plan.startsNewCycle)
    }

    @Test fun playedHistoryIsNotTreatedAsAnUpcomingQueueInLaterCycles() {
        val library = (1..2000).map { "song$it" }
        val first = planLibraryContinuation(library, listOf("song2000"), "song2000",
            playedIds = library.toSet(), random = Random(1))
        val secondCurrent = first.songIds.last()
        val second = planLibraryContinuation(library, listOf(secondCurrent), secondCurrent,
            playedIds = library.toSet(), random = Random(2))
        assertTrue(first.startsNewCycle && second.startsNewCycle)
        assertNotEquals(first.songIds.take(50), second.songIds.take(50))
        assertEquals(library.toSet() - secondCurrent, second.songIds.toSet())
    }

    @Test fun suggestionsCannotShrinkTheLibraryToRelatedArtists() {
        val library = (1..2000).map { "song$it" }
        val related = library.take(500).toSet()
        val plan = planLibraryContinuation(library, emptyList(), null,
            preferredIds = related, random = Random(5))
        assertEquals(library.toSet(), plan.songIds.toSet())
        assertEquals(13, plan.songIds.take(50).count { it in related })
        assertEquals(2000, plan.songIds.size)
    }

    @Test fun playedRecommendationsAndQueuedManualDuplicatesRemainExcluded() {
        val plan = planLibraryContinuation(listOf("a", "b", "c", "d", "e"), listOf("b", "b", "d"), "b",
            playedIds = setOf("a"), preferredIds = setOf("a", "b", "d", "missing"))
        assertEquals(setOf("c", "e"), plan.songIds.toSet())
    }

    @Test fun newlyAddedSongPlaysBeforeResettingAnOtherwiseExhaustedCycle() {
        val plan = planLibraryContinuation(listOf("a", "b", "new"), listOf("b"), "b", playedIds = setOf("a", "b", "deleted"))
        assertEquals(listOf("new"), plan.songIds)
        assertFalse(plan.startsNewCycle)
    }

    @Test fun skippedSearchPrefixRemainsEligibleOnceSelectedContextFinishes() {
        val plan = planLibraryContinuation(listOf("first", "second", "selected", "last"), listOf("last"), "last",
            playedIds = setOf("selected", "last"))
        assertEquals(setOf("first", "second"), plan.songIds.toSet())
    }

    @Test fun staleCachedRowsCannotBringExcludedFoldersBackIntoAutomaticPlayback() {
        val scope = chromahub.rhythm.app.core.domain.scan.MediaScanScope(
            "WHITELIST", setOf("flac", "mp3"), 30_000L,
            setOf("/storage/emulated/0/Music"), emptySet(), emptySet(), setOf("hidden"))
        val rows = listOf(
            Triple("playing", "/sdcard/Music/current.flac", 180_000L),
            Triple("next", "/sdcard/Music/next.mp3", 180_000L),
            Triple("recording", "/sdcard/Recordings/voice.mp3", 180_000L),
            Triple("hidden", "/sdcard/Music/hidden.mp3", 180_000L),
            Triple("short", "/sdcard/Music/short.mp3", 2_000L),
            Triple("format", "/sdcard/Music/other.wav", 180_000L))
        val library = rows.filter { (id, path, duration) -> scope.includes(id, path, duration) }.map { it.first }
        assertEquals(listOf("next"), planLibraryContinuation(library, listOf("playing"), "playing").songIds)
        val emptyScope = scope.copy(whitelistedFolders = emptySet())
        assertTrue(rows.none { (id, path, duration) -> emptyScope.includes(id, path, duration) })
    }
}
