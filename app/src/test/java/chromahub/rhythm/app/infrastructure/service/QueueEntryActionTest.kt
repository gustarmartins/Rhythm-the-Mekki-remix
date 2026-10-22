/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.*
import org.junit.Test

class QueueEntryActionTest {
    private val queue = listOf("played", "current", "duplicate-first", "context", "duplicate-second", "tail")
    private fun edit(selected: String, action: String, pending: List<String> = emptyList(), target: String? = null) =
        planQueueEntryEdit(queue, 1, pending, selected, action, target)

    @Test fun playAdvancesToExactOccurrenceWithoutReplacingQueue() {
        val result = edit("duplicate-second", QueueEntryCommand.PLAY, listOf("duplicate-first", "duplicate-second", "tail"))
        assertEquals(queue, result.order)
        assertEquals(4, result.currentIndex)
        assertEquals(listOf("tail"), result.pending)
    }

    @Test fun playNextMovesExistingOccurrenceAndRetainsItsDuplicate() {
        val result = edit("duplicate-second", QueueEntryCommand.NEXT)
        assertEquals(listOf("played", "current", "duplicate-second", "duplicate-first", "context", "tail"), result.order)
        assertEquals(1, result.currentIndex)
        assertEquals(listOf("duplicate-second"), result.pending)
    }

    @Test fun repeatedRequestsAreFifoIncludingRequestsAlreadyInContext() {
        var result = QueueEntryEdit(listOf("Medicina", "context", "Jet Rocket", "Brave", "Jump"), 0, emptyList())
        for (song in listOf("Jump", "Brave", "Jet Rocket")) {
            result = planQueueEntryEdit(result.order, result.currentIndex, result.pending, song, QueueEntryCommand.NEXT)
        }
        assertEquals(listOf("Medicina", "Jump", "Brave", "Jet Rocket", "context"), result.order)
        assertEquals(listOf("Jump", "Brave", "Jet Rocket"), result.pending)
    }

    @Test fun queuePromotionFollowsEarlierExternalManualAdditions() {
        val result = edit("tail", QueueEntryCommand.NEXT, listOf("duplicate-first", "context"))
        assertEquals(listOf("played", "current", "duplicate-first", "context", "tail", "duplicate-second"), result.order)
    }

    @Test fun repeatedTapOnPendingOccurrenceIsIdempotent() {
        val first = edit("tail", QueueEntryCommand.NEXT)
        assertEquals(first, planQueueEntryEdit(first.order, first.currentIndex, first.pending, "tail", QueueEntryCommand.NEXT))
    }

    @Test fun tappingCurrentInPlayNextModeDoesNotEnqueueOrMoveIt() {
        assertEquals(QueueEntryEdit(queue, 1, emptyList()), edit("current", QueueEntryCommand.NEXT))
    }

    @Test fun historyPromotionPreservesCurrentIdentityAndAllOtherOccurrences() {
        val result = edit("played", QueueEntryCommand.NEXT)
        assertEquals(listOf("current", "played", "duplicate-first", "context", "duplicate-second", "tail"), result.order)
        assertEquals(0, result.currentIndex)
    }

    @Test fun removeSecondDuplicateKeepsFirstAndCurrentOccurrence() {
        val result = edit("duplicate-second", QueueEntryCommand.REMOVE)
        assertEquals(listOf("played", "current", "duplicate-first", "context", "tail"), result.order)
        assertEquals("current", result.order[result.currentIndex])
    }

    @Test fun delayedRemovalAfterAnotherRemovalStillTargetsToken() {
        val first = edit("played", QueueEntryCommand.REMOVE)
        val second = planQueueEntryEdit(first.order, first.currentIndex, first.pending, "duplicate-second", QueueEntryCommand.REMOVE)
        assertEquals(listOf("current", "duplicate-first", "context", "tail"), second.order)
        assertEquals(0, second.currentIndex)
    }

    @Test(expected = IllegalArgumentException::class) fun currentCannotBeRemoved() {
        edit("current", QueueEntryCommand.REMOVE)
    }

    @Test(expected = IllegalArgumentException::class) fun advancingToSelectedRowBeforeDelayedRemovalProtectsIt() {
        planQueueEntryEdit(queue, 4, emptyList(), "duplicate-second", QueueEntryCommand.REMOVE)
    }

    @Test(expected = IllegalArgumentException::class) fun staleTokenNeverFallsBackToSongIdOrIndex() {
        edit("deleted-occurrence", QueueEntryCommand.PLAY)
    }

    @Test(expected = IllegalArgumentException::class) fun staleDragTargetIsRejected() {
        edit("tail", QueueEntryCommand.MOVE, target = "deleted-occurrence")
    }

    @Test(expected = IllegalArgumentException::class) fun dragCannotCrossCurrentOccurrence() {
        edit("tail", QueueEntryCommand.MOVE, target = "current")
    }

    @Test fun explicitDragCanReorderManualRequests() {
        val result = edit("context", QueueEntryCommand.MOVE, listOf("duplicate-first", "context"), "duplicate-first")
        assertEquals(listOf("context", "duplicate-first"), result.pending)
        assertEquals(listOf("played", "current", "context", "duplicate-first", "duplicate-second", "tail"), result.order)
    }

    @Test fun movingManualRequestBackIntoContextReleasesItsReservation() {
        val result = edit("duplicate-first", QueueEntryCommand.MOVE, listOf("duplicate-first", "context"), "tail")
        assertEquals(listOf("context"), result.pending)
    }

    @Test fun goingBackDoesNotResurrectConsumedManualRequests() {
        val forward = edit("duplicate-second", QueueEntryCommand.PLAY, listOf("duplicate-first", "duplicate-second", "tail"))
        val back = planQueueEntryEdit(forward.order, forward.currentIndex, forward.pending, "played", QueueEntryCommand.PLAY)
        assertEquals(listOf("tail"), back.pending)
    }

    @Test fun consumedAndDeletedReservationsArePrunedBeforePromotion() {
        val result = edit("tail", QueueEntryCommand.NEXT, listOf("played", "current", "deleted", "context"))
        assertEquals(listOf("context", "tail"), result.pending)
    }

    @Test(expected = IllegalArgumentException::class) fun invalidDuplicateTokensAreRejected() {
        planQueueEntryEdit(listOf("a", "a"), 0, emptyList(), "a", QueueEntryCommand.NEXT)
    }

    @Test(expected = IllegalArgumentException::class) fun emptyQueueIsRejected() {
        planQueueEntryEdit(emptyList(), -1, emptyList(), "a", QueueEntryCommand.PLAY)
    }

    @Test fun arbitraryShuffleTraversalRetainsCurrentAndEveryOccurrenceAcrossPromotions() {
        val random = kotlin.random.Random(42)
        repeat(100) {
            val order = (0 until 100).map { "entry-$it" }.shuffled(random)
            val current = random.nextInt(order.size)
            var result = QueueEntryEdit(order, current, emptyList())
            repeat(20) {
                val selected = order.random(random)
                result = planQueueEntryEdit(result.order, result.currentIndex, result.pending, selected, QueueEntryCommand.NEXT)
                assertEquals(order[current], result.order[result.currentIndex])
                assertEquals(order.toSet(), result.order.toSet())
                assertEquals(order.size, result.order.size)
                assertEquals(result.pending, result.order.drop(result.currentIndex + 1).take(result.pending.size))
            }
        }
    }
}
