package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.*
import org.junit.Test

class LibraryQueueContinuationTest {
    @Test fun shortMostPlayedContextContinuesWithRemainingDeviceLibrary() {
        val library = (1..1905).map { "song$it" }
        val plan = planLibraryContinuation(library, library.take(5), "song5")
        assertEquals(1900, plan.songIds.size)
        assertEquals("song6", plan.songIds.first())
        assertEquals("song1905", plan.songIds.last())
        assertFalse(plan.startsNewCycle)
    }
    @Test fun queuedManualRequestsAreNotDuplicatedByTheLibraryPool() {
        val plan = planLibraryContinuation(listOf("Medicina", "Jump", "Brave", "Jet Rocket", "More"),
            listOf("Medicina", "Jump", "Brave", "Jet Rocket"), "Medicina")
        assertEquals(listOf("More"), plan.songIds)
    }
    @Test fun exhaustedLibraryStartsAnotherCycleWithoutImmediatelyRepeatingCurrentSong() {
        val plan = planLibraryContinuation(listOf("one", "two", "three"), listOf("one", "two", "three"), "three")
        assertEquals(listOf("one", "two"), plan.songIds)
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
}
