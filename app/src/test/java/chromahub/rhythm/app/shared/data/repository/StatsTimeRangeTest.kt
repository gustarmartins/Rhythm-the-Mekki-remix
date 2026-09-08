/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class StatsTimeRangeTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val nowMillis = 1_743_120_000_000L

    @Test
    fun today_resolvesStartOfCurrentDay() {
        val (start, end) = StatsTimeRange.TODAY.resolveBounds(nowMillis, zoneId)

        assertEquals(1_743_120_000_000L, start)
        assertEquals(nowMillis, end)
    }

    @Test
    fun week_resolvesSevenDayWindowStart() {
        val (start, end) = StatsTimeRange.WEEK.resolveBounds(nowMillis, zoneId)

        assertEquals(1_742_601_600_000L, start)
        assertEquals(nowMillis, end)
    }

    @Test
    fun month_resolvesThirtyDayWindowStart() {
        val (start, end) = StatsTimeRange.MONTH.resolveBounds(nowMillis, zoneId)

        assertEquals(1_740_614_400_000L, start)
        assertEquals(nowMillis, end)
    }

    @Test
    fun allTime_hasNoStartBound() {
        val (start, end) = StatsTimeRange.ALL_TIME.resolveBounds(nowMillis, zoneId)

        assertNull(start)
        assertEquals(nowMillis, end)
    }

    @Test
    fun boundedRanges_neverExceedNow() {
        StatsTimeRange.entries
            .filter { it != StatsTimeRange.ALL_TIME }
            .forEach { range ->
                val (start, end) = range.resolveBounds(nowMillis, zoneId)
                assertTrue(start != null)
                assertTrue(start!! <= end)
                assertEquals(nowMillis, end)
            }
    }
}
