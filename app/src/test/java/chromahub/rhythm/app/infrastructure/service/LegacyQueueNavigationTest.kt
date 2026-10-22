/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service

import org.junit.Assert.*
import org.junit.Test

class LegacyQueueNavigationTest {
    @Test fun repeatAllPreviousWrapsWithoutDuplicatingOldCurrent() {
        assertEquals(LegacyPrevious(listOf("b"), "c", listOf("a")),
            planLegacyPrevious(emptyList(), "a", listOf("b", "c"), true))
    }

    @Test fun normalPreviousRetainsRemainingUpcomingOrder() {
        assertEquals(LegacyPrevious(listOf("a"), "b", listOf("c", "d")),
            planLegacyPrevious(listOf("a", "b"), "c", listOf("d"), false))
    }

    @Test fun noRepeatDoesNotWrap() {
        assertNull(planLegacyPrevious(emptyList(), "a", listOf("b"), false))
    }

    @Test fun singleOccurrenceHasNoPrevious() {
        assertNull(planLegacyPrevious(emptyList(), "a", emptyList(), true))
    }

    @Test fun repeatedWrapsRetainEachOccurrenceExactlyOnce() {
        var state = LegacyPrevious(emptyList(), "same-first", listOf("same-second", "other"))
        repeat(20) {
            state = planLegacyPrevious(state.history, state.current, state.upcoming, true)!!
            val all = state.history + state.current + state.upcoming
            assertEquals(3, all.size)
            assertEquals(setOf("same-first", "same-second", "other"), all.toSet())
        }
    }
}
