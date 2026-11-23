package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.*
import org.junit.Test

class PlayNextQueuePolicyTest {
    private class Queue(var tokens: MutableList<String?>, var order: List<Int>, var current: Int) {
        var pending = emptyList<String>()
        fun add(vararg entries: String, listPlayback: Boolean = false) {
            val plan = planPlayNextInsertion(tokens, current, order, pending, entries.size, listPlayback)
            tokens.addAll(plan.insertionIndex, entries.toList())
            if (current >= plan.insertionIndex) current += entries.size
            order = plan.playbackOrder
            pending = if (listPlayback) plan.remainingPending else plan.remainingPending + entries
        }
        fun future() = order.drop(order.indexOf(current) + 1).map { tokens[it] }
        fun next() { current = order[order.indexOf(current) + 1] }
    }

    @Test fun medicinaThenJumpBraveJetRocketKeepsRequestOrderBeforeAlbum() {
        val q = Queue(mutableListOf(null, "album-tail"), listOf(0, 1), 0)
        q.add("Jump"); q.add("Brave"); q.add("Jet Rocket")
        assertEquals(listOf("Jump", "Brave", "Jet Rocket", "album-tail"), q.future())
        assertEquals(0, q.current)
    }

    @Test fun batchesAndSingleSelectionsShareOneFifoSegment() {
        val q = Queue(mutableListOf(null, "tail"), listOf(0, 1), 0)
        q.add("Jump", "Brave"); q.add("Jet Rocket"); q.add("four", "five")
        assertEquals(listOf("Jump", "Brave", "Jet Rocket", "four", "five", "tail"), q.future())
    }

    @Test fun individualRequestsInterruptAPlayingAlbumAndTheAlbumThenResumes() {
        val album = List(105) { "$LIST_PLAYBACK_TOKEN_PREFIX$it" }
        // Current is last in physical order, with a different next track in shuffle traversal.
        val q = Queue(mutableListOf("tail", "history", "current"), listOf(1, 2, 0), 2)
        album.chunked(24).forEach { q.add(*it.toTypedArray(), listPlayback = true) }
        assertEquals(album + "tail", q.future())
        q.next()
        q.add("Brave-1", "Brave-2")
        q.add("other-request")
        assertEquals(listOf("Brave-1", "Brave-2", "other-request") + album.drop(1) + "tail", q.future())
        assertEquals(album.first(), q.tokens[q.current])
        assertEquals(listOf("Brave-1", "Brave-2", "other-request"), q.pending)
    }

    @Test fun laterAlbumsFollowEarlierListsAndLeaveManualRequestsAtTheFront() {
        val q = Queue(mutableListOf("current", "tail"), listOf(0, 1), 0)
        val first = LIST_PLAYBACK_TOKEN_PREFIX + "album-one"
        val second = LIST_PLAYBACK_TOKEN_PREFIX + "album-two"
        q.add(first, listPlayback = true)
        q.add("manual")
        q.add(second, listPlayback = true)
        assertEquals(listOf("manual", first, second, "tail"), q.future())
        val traversal = q.order.map { q.tokens[it]!! }
        val saved = SavedPlaybackQueue(traversal, traversal, q.pending, traversal, true)
        for (shuffle in listOf(false, true)) {
            val plan = planQueueShuffle(saved.tokens, 0, saved.pending, saved.original, shuffle)
            assertEquals(traversal, plan.order)
            assertEquals(listOf("manual"), plan.pending)
        }
    }

    @Test fun nativeShuffleWithCurrentAtLastPhysicalIndexKeepsContextTraversal() {
        val q = Queue(mutableListOf("a", "b", null), listOf(1, 2, 0), 2)
        q.add("Jump"); q.add("Brave"); q.add("Jet Rocket")
        assertEquals(listOf("Jump", "Brave", "Jet Rocket", "a"), q.future())
        assertEquals("b", q.tokens[q.order.first()])
        assertEquals(q.tokens.indices.toList(), q.order.sorted())
    }

    @Test fun requestAfterAdvancementFollowsRemainingManualSongs() {
        val q = Queue(mutableListOf(null, "tail"), listOf(0, 1), 0)
        q.add("Jump", "Brave"); q.next(); q.add("Jet Rocket")
        assertEquals("Jump", q.tokens[q.current])
        assertEquals(listOf("Brave", "Jet Rocket", "tail"), q.future())
        assertEquals(listOf("Brave", "Jet Rocket"), q.pending)
    }

    @Test fun skippingPastPendingEntriesDoesNotResurrectThem() {
        val q = Queue(mutableListOf(null, "tail"), listOf(0, 1), 0)
        q.add("Jump", "Brave"); q.next(); q.next(); q.next(); q.add("Jet Rocket")
        assertEquals(listOf("Jet Rocket"), q.future())
        assertEquals(listOf("Jet Rocket"), q.pending)
    }

    @Test fun duplicateSongsUseDistinctOccurrenceTokens() {
        val q = Queue(mutableListOf(null, "context-Jump"), listOf(0, 1), 0)
        q.add("manual-Jump-1"); q.add("manual-Jump-2"); q.add("Brave")
        assertEquals(listOf("manual-Jump-1", "manual-Jump-2", "Brave", "context-Jump"), q.future())
    }

    @Test fun deletedPendingOccurrenceCannotBecomeInsertionAnchor() {
        val tokens = listOf(null, "Jump", "tail")
        val plan = planPlayNextInsertion(tokens, 0, listOf(0, 1, 2), listOf("Jump", "deleted-Brave"), 1)
        assertEquals(2, plan.insertionIndex)
        assertEquals(listOf("Jump"), plan.remainingPending)
        assertEquals(listOf(0, 1, 2, 3), plan.playbackOrder)
    }

    @Test fun queueReplacementDropsTokensFromPreviousQueue() {
        val plan = planPlayNextInsertion(listOf(null, "new-album-tail"), 0, listOf(0, 1), listOf("old-Jump"), 1)
        assertEquals(emptyList<String>(), plan.remainingPending)
        assertEquals(1, plan.insertionIndex)
    }

    @Test fun emptyQueueStartsWithRequestedBatchInOrder() {
        val plan = planPlayNextInsertion(emptyList(), -1, emptyList(), emptyList(), 3)
        assertEquals(0, plan.insertionIndex)
        assertEquals(listOf(0, 1, 2), plan.playbackOrder)
    }

    @Test fun legacyVirtualOrderRemapsHistoryAndCurrentOccurrence() {
        val plan = planPlayNextInsertion(listOf("tail", "history", null), 2, listOf(1, 2, 0), emptyList(), 2)
        assertEquals(3, plan.insertionIndex)
        assertEquals(1, plan.displayCurrentIndex)
        assertEquals(2, plan.displayInsertionIndex)
        assertEquals(listOf(1, 2, 3, 4, 0), plan.playbackOrder)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTimelinePermutationIsRejected() {
        planPlayNextInsertion(listOf(null, "tail"), 0, listOf(0, 0), emptyList(), 1)
    }
}
