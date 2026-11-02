package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.LyricsData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTimingPolicyTest {
    @Test
    fun multiLineTimelineCollapsedAtZero_isRejected() {
        assertFalse(LyricsTimingPolicy.hasUsableTimeline(listOf(0L, 0L, 0L)))
    }

    @Test
    fun timelineMayStartAtZeroWhenLaterLinesAdvance() {
        assertTrue(LyricsTimingPolicy.hasUsableTimeline(listOf(0L, 2_500L, 7_900L)))
    }

    @Test
    fun rockefellerStreetNightcore_rescalesOverflowingLrcAndIgnoresMislabeledShortCatalogDuration() {
        val originalLrc = """
            [00:09.63]Daylight is fading away, night silhouettes in the sky
            [00:52.03]1273 down the Rockefeller street
            [03:08.42]Everything is more than surreal
            [03:10.83]
        """.trimIndent()
        val raw = LyricsData(
            plainLyrics = "Daylight is fading away",
            syncedLyrics = originalLrc,
            source = "LRCLib",
            sourceDurationMs = 147_000L // Mislabeled LRCLib duration shorter than 03:10.83 (190,830ms)
        )
        val resolvedSource = LyricsTimingPolicy.resolveSourceDurationMs(
            lyrics = raw,
            catalogDurationsMs = listOf(147_000L, 192_993L)
        )
        assertEquals(192_993L, resolvedSource)

        val scaled = LyricsTimingPolicy.adjustToTrackDuration(
            lyrics = raw.copy(sourceDurationMs = resolvedSource),
            targetDurationMs = 146_495L,
            isTempoEdited = LyricsTimingPolicy.isTempoEditedTrack(
                title = "Rockefeller Street",
                album = "Nightcore Mix"
            )
        )
        val lines = scaled.syncedLyrics!!.lines()
        assertTrue("Expected scaled first line near 00:07.31, got ${lines[0]}", lines[0].startsWith("[00:07.31]"))
        assertTrue("Expected scaled chorus line near 00:39.49, got ${lines[1]}", lines[1].startsWith("[00:39.49]"))
        assertTrue("Expected scaled final vocal line near 02:23.02, got ${lines[2]}", lines[2].startsWith("[02:23.02]"))
    }

    @Test
    fun wordByWordLyrics_areScaledForNightcoreTrack() {
        val wordJson = """[{"text":[{"text":"1273","part":false,"timestamp":52030,"endtime":53000}],"timestamp":52030,"endtime":53000}]"""
        val raw = LyricsData(
            plainLyrics = "1273",
            syncedLyrics = "[00:52.03]1273",
            wordByWordLyrics = wordJson,
            source = "Lyrically (Apple Music)",
            sourceDurationMs = 192_993L
        )
        val scaled = LyricsTimingPolicy.adjustToTrackDuration(
            lyrics = raw,
            targetDurationMs = 146_495L,
            isTempoEdited = true
        )
        val parsed = RhythmLyricsParser.parseWordByWordLyrics(scaled.wordByWordLyrics!!)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].lineTimestamp in 39_480L..39_510L)
        assertTrue(parsed[0].words[0].timestamp in 39_480L..39_510L)
    }

    @Test
    fun normalSpeedTrackWithMinorDurationPadding_isNotScaled() {
        val lrc = "[00:10.00]First line\n[02:50.00]Last line"
        val raw = LyricsData(
            plainLyrics = "First line\nLast line",
            syncedLyrics = lrc,
            source = "LRCLib",
            sourceDurationMs = 180_000L
        )
        val result = LyricsTimingPolicy.adjustToTrackDuration(
            lyrics = raw,
            targetDurationMs = 178_500L,
            isTempoEdited = false
        )
        assertEquals(lrc, result.syncedLyrics)
    }
}
