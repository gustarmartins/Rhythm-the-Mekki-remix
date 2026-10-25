/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class SearchHistoryTest {
    @Test fun latestTypedSearchIsRetainedWithoutAnyResultClick() {
        assertEquals(listOf("Twice Brave", "Medicina"), updatedSearchHistory(listOf("Medicina"), "  Twice Brave  "))
    }

    @Test fun revisitingAQueryMovesItToTheFrontWithoutCaseDuplicates() {
        assertEquals(listOf("brave", "Jump"), updatedSearchHistory(listOf("Jump", "Brave", " BRAVE "), "brave"))
    }

    @Test fun clearingTheFieldDoesNotEraseHistoryOrCreateAnEmptyEntry() {
        val history = listOf("Brave", "Jump")
        assertEquals(history, updatedSearchHistory(history, "  "))
    }

    @Test fun debounceClickAndDisposalFlushesAreIdempotent() {
        val once = updatedSearchHistory(listOf("Jump"), "Brave")
        assertEquals(once, updatedSearchHistory(updatedSearchHistory(once, "Brave"), "Brave"))
    }

    @Test fun historyStaysBoundedAndKeepsTheNewestQuery() {
        assertEquals(listOf("new") + (0..8).map { "query$it" },
            updatedSearchHistory((0..9).map { "query$it" }, "new"))
    }
}
