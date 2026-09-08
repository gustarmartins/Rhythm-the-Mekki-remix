/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtmlAndElrcExportTest {

    @Test
    fun testWordByWordToEnhancedLRCAndBack() {
        val originalLines = listOf(
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("This", false, 10000L, 10500L),
                    WordByWordWord("is", false, 10500L, 11000L),
                    WordByWordWord("a", false, 11000L, 11500L),
                    WordByWordWord("test", false, 11500L, 12500L)
                ),
                lineTimestamp = 10000L,
                lineEndtime = 12500L,
                romanization = "Kore wa tesuto desu",
                translation = "This is a test"
            ),
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("Second", false, 13000L, 13800L),
                    WordByWordWord("line", false, 13800L, 15000L)
                ),
                lineTimestamp = 13000L,
                lineEndtime = 15000L
            )
        )

        val elrc = RhythmLyricsParser.toEnhancedLRCFormat(originalLines)
        assertTrue(elrc.contains("[00:10.00]<00:10.00>This <00:10.50>is <00:11.00>a <00:11.50>test"))
        assertTrue(elrc.contains("[00:10.00][Kore wa tesuto desu]"))
        assertTrue(elrc.contains("[00:10.00](This is a test)"))
        assertTrue(elrc.contains("[00:13.00]<00:13.00>Second <00:13.80>line"))

        assertTrue("Generated ELRC must be recognized as having word timestamps", LyricsParser.hasWordTimestamps(elrc))

        val parsedBack = RhythmLyricsParser.parseEnhancedLRCtoWordByWord(elrc)
        assertEquals(2, parsedBack.size)

        val firstLine = parsedBack[0]
        assertEquals(10000L, firstLine.lineTimestamp)
        assertEquals("Kore wa tesuto desu", firstLine.romanization)
        assertEquals("This is a test", firstLine.translation)
        assertEquals(4, firstLine.words.size)
        assertEquals("This", firstLine.words[0].text)
        assertEquals(10000L, firstLine.words[0].timestamp)
        assertEquals("is", firstLine.words[1].text)
        assertEquals(10500L, firstLine.words[1].timestamp)
        assertEquals("a", firstLine.words[2].text)
        assertEquals(11000L, firstLine.words[2].timestamp)
        assertEquals("test", firstLine.words[3].text)
        assertEquals(11500L, firstLine.words[3].timestamp)

        assertTrue(RhythmLyricsParser.hasWordTiming(parsedBack))
    }

    @Test
    fun testWordByWordToTtmlExportAndParseBack() {
        val originalLines = listOf(
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("Hello", false, 5000L, 5500L),
                    WordByWordWord("world", false, 5500L, 7000L)
                ),
                lineTimestamp = 5000L,
                lineEndtime = 7000L
            )
        )

        val ttml = RhythmLyricsParser.toTtmlFormat(
            wordByWordLines = originalLines,
            title = "Test Track",
            artist = "Test Artist"
        )

        assertTrue(ttml.contains("<songTitle>Test Track</songTitle>"))
        assertTrue(ttml.contains("<artist>Test Artist</artist>"))
        assertTrue(ttml.contains("itunes:timing=\"Word\""))
        assertTrue(ttml.contains("<p begin=\"00:05.000\" end=\"00:07.000\">"))
        assertTrue(ttml.contains("<span begin=\"00:05.000\" end=\"00:05.500\">Hello </span>"))
        assertTrue(ttml.contains("<span begin=\"00:05.500\" end=\"00:07.000\">world </span>"))

        val parsedLines = RhythmLyricsParser.parseTtmlFallback(ttml)
        assertEquals(1, parsedLines.size)
        val wordByWord = RhythmLyricsParser.parseWordByWordLyrics(
            com.google.gson.Gson().toJson(parsedLines)
        )
        assertEquals(1, wordByWord.size)
        assertTrue(RhythmLyricsParser.hasWordTiming(wordByWord))
        assertEquals(2, wordByWord[0].words.size)
        assertEquals(5000L, wordByWord[0].words[0].timestamp)
        assertEquals(5500L, wordByWord[0].words[1].timestamp)
    }

    @Test
    fun testLineOnlyLyricsToTtmlExportUsesLineTiming() {
        val lineOnly = listOf(
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("Line without word timing", false, 8000L, 12000L)
                ),
                lineTimestamp = 8000L,
                lineEndtime = 12000L
            )
        )

        val ttml = RhythmLyricsParser.toTtmlFormat(lineOnly)
        assertTrue(ttml.contains("itunes:timing=\"Line\""))
        assertTrue(ttml.contains("<p begin=\"00:08.000\" end=\"00:12.000\">Line without word timing</p>"))
        assertFalse(ttml.contains("<span"))

        val parsedLines = RhythmLyricsParser.parseTtmlFallback(ttml)
        assertEquals(1, parsedLines.size)
        val wordByWord = RhythmLyricsParser.parseWordByWordLyrics(
            com.google.gson.Gson().toJson(parsedLines)
        )
        assertFalse("Line-only TTML must not produce word timing", RhythmLyricsParser.hasWordTiming(wordByWord))
    }

    @Test
    fun testEnhancedLrcTimestampShifting() {
        val elrc = "[00:10.00]<00:10.00>First <00:10.50>word"
        val parsed = RhythmLyricsParser.parseEnhancedLRCtoWordByWord(elrc)
        val offsetMs = 500L
        val adjusted = parsed.map { line ->
            line.copy(
                lineTimestamp = (line.lineTimestamp + offsetMs).coerceAtLeast(0L),
                lineEndtime = (line.lineEndtime + offsetMs).coerceAtLeast(0L),
                words = line.words.map { word ->
                    word.copy(
                        timestamp = (word.timestamp + offsetMs).coerceAtLeast(0L),
                        endtime = (word.endtime + offsetMs).coerceAtLeast(0L)
                    )
                }
            )
        }
        val shiftedElrc = RhythmLyricsParser.toEnhancedLRCFormat(adjusted)
        assertTrue(shiftedElrc.contains("[00:10.50]<00:10.50>First <00:11.00>word"))
    }

    @Test
    fun testEnhancedLrcJapaneseWordPartsSpacing() {
        val japaneseLines = listOf(
            WordByWordLyricLine(
                words = listOf(
                    WordByWordWord("絶対", false, 1000L, 1500L),
                    WordByWordWord("零度", true, 1500L, 2000L)
                ),
                lineTimestamp = 1000L,
                lineEndtime = 2000L
            )
        )
        val elrc = RhythmLyricsParser.toEnhancedLRCFormat(japaneseLines)
        assertTrue(elrc.contains("[00:01.00]<00:01.00>絶対<00:01.50>零度"))

        val plain = RhythmLyricsParser.toPlainText(japaneseLines)
        assertEquals("絶対零度", plain)
    }
}
