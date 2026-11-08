package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RhythmLyricsParserTest {

    @Test
    fun longRadioTimelinePreservesEveryLineBeyondOneHour() {
        val lrc = (0 until 1_000).joinToString("\n") { index ->
            val seconds = index * 4
            "[%02d:%02d.00]Radio line %d".format(java.util.Locale.ROOT,
                seconds / 60, seconds % 60, index)
        }
        val parsed = LyricsParser.parseLyrics(lrc)
        assertEquals(1_000, parsed.size)
        assertEquals(3_996_000L, parsed.last().timestamp)
        assertEquals("Radio line 999", parsed.last().text)
    }

    @Test
    fun longEnhancedRadioTimelineKeepsWordTimingAndLineOnlyFallbacks() {
        fun timestamp(milliseconds: Int): String = "%02d:%02d.%02d".format(
            java.util.Locale.ROOT, milliseconds / 60_000,
            milliseconds / 1_000 % 60, milliseconds % 1_000 / 10)
        val lrc = (0 until 1_000).joinToString("\n") { index ->
            val start = index * 4_000
            if (index % 10 == 0) "[${timestamp(start)}]Radio line $index"
            else "[${timestamp(start)}]<${timestamp(start)}>Radio <${timestamp(start + 500)}>line $index<${timestamp(start + 1_000)}>"
        }
        val parsed = LyricsParser.parseEnhancedLRC(lrc)
        assertEquals(1_000, parsed.size)
        assertEquals(3_996_000L, parsed.last().lineTimestamp)
        assertEquals(listOf("Radio", "line 999"), parsed.last().words.map { it.text })
        assertEquals("Radio line 990", parsed[990].words.single().text)
    }

    @Test
    fun neteasePartFlags_doNotRemoveSpacesBetweenFullLatinWords() {
        val line = WordByWordLyricLine(
            words = listOf(
                WordByWordWord("All", false, 12_220L, 12_400L),
                WordByWordWord("the", true, 12_400L, 12_560L),
                WordByWordWord("things", true, 12_560L, 12_800L),
                WordByWordWord("she", true, 12_800L, 13_000L),
                WordByWordWord("said", true, 13_000L, 13_250L),
                WordByWordWord(",", true, 13_250L, 13_300L)
            ),
            lineTimestamp = 12_220L,
            lineEndtime = 13_300L
        )

        assertEquals("All the things she said,", RhythmLyricsParser.toPlainText(listOf(line)))
        assertEquals(
            "[00:12.22]All the things she said,",
            RhythmLyricsParser.toLRCFormat(listOf(line))
        )
    }

    @Test
    fun explicitSyllableBoundaries_stillJoinPartsWithinLatinWords() {
        val line = WordByWordLyricLine(
            words = listOf(
                WordByWordWord("Some", false, 1_000L, 1_200L),
                WordByWordWord("thing", true, 1_200L, 1_400L),
                WordByWordWord("new", false, 1_400L, 1_700L)
            ),
            lineTimestamp = 1_000L,
            lineEndtime = 1_700L
        )

        assertEquals("Something new", RhythmLyricsParser.toPlainText(listOf(line)))
    }

    @Test
    fun lrcCompatibilityOutput_preservesRomanizationAtTheSameTimestamp() {
        val lines = listOf(
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("君", false, 1_000L, 1_200L),
                    WordByWordWord("が", true, 1_200L, 1_400L),
                    WordByWordWord("好き", true, 1_400L, 1_800L)
                ),
                lineTimestamp = 1_000L,
                lineEndtime = 1_800L,
                romanization = "kimi ga suki"
            )
        )

        assertEquals(
            "[00:01.00]君が好き\n[00:01.00][kimi ga suki]",
            RhythmLyricsParser.toLRCFormat(lines)
        )
    }

    @Test
    fun lrcCompatibilityOutput_isReadBackWithRomanization() {
        val parsed = LyricsParser.parseLyrics(
            "[00:01.00]君が好き\n[00:01.00][kimi ga suki]"
        )

        assertEquals("君が好き", parsed.single().text)
        assertEquals("kimi ga suki", parsed.single().romanization)
    }

    @Test
    fun timedRomanizationTrack_staysAttachedToItsJapaneseLine() {
        val parsed = LyricsParser.parseLyrics(
            "[00:00.93]どんな時だって\n" +
                "[00:06.14]運命忘れて\n" +
                "[00:00.93][do n na to ki da tte]\n" +
                "[00:06.14][u n me i wa su re te]"
        )

        assertEquals(2, parsed.size)
        assertEquals("どんな時だって", parsed[0].text)
        assertEquals("do n na to ki da tte", parsed[0].romanization)
        assertEquals("運命忘れて", parsed[1].text)
        assertEquals("u n me i wa su re te", parsed[1].romanization)
    }
}
