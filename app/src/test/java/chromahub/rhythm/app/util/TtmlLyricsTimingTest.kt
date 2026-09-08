/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtmlLyricsTimingTest {

    @Test
    fun testTtmlLineTimingHasNoWordTiming() {
        val ttmlLine = """
            <?xml version="1.0" encoding="utf-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Line">
                <head>
                    <metadata>
                        <iTunesMetadata>
                            <songwriters>
                                <songwriter>Artist Name</songwriter>
                            </songwriters>
                        </iTunesMetadata>
                    </metadata>
                </head>
                <body>
                    <div>
                        <p begin="00:10.000" end="00:13.000">This is a lyric line</p>
                        <p begin="00:13.500" end="00:16.000">Second line of lyrics</p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val rhythmParsed = RhythmLyricsParser.parseTtmlLyrics(ttmlLine)
        assertEquals(2, rhythmParsed.size)

        val wordByWordLines = RhythmLyricsParser.parseWordByWordLyrics(
            com.google.gson.Gson().toJson(rhythmParsed)
        )
        assertEquals(2, wordByWordLines.size)
        assertFalse("TTML with Line timing must not have word timing", RhythmLyricsParser.hasWordTiming(wordByWordLines))
        assertFalse("Line 1 must not be word-timed", RhythmLyricsParser.isLineWordTimed(wordByWordLines[0]))
        assertFalse("Line 2 must not be word-timed", RhythmLyricsParser.isLineWordTimed(wordByWordLines[1]))

        val lrc = RhythmLyricsParser.toLRCFormat(wordByWordLines)
        assertTrue(lrc.contains("[00:10.00]This is a lyric line"))
        assertTrue(lrc.contains("[00:13.50]Second line of lyrics"))
    }

    @Test
    fun testTtmlWordTimingHasWordTiming() {
        val ttmlWord = """
            <?xml version="1.0" encoding="utf-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Word">
                <body>
                    <div>
                        <p begin="00:10.000" end="00:13.000">
                            <span begin="00:10.000" end="00:10.500">This </span>
                            <span begin="00:10.500" end="00:11.000">is </span>
                            <span begin="00:11.000" end="00:11.500">a </span>
                            <span begin="00:11.500" end="00:12.000">lyric </span>
                            <span begin="00:12.000" end="00:13.000">line</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()

        val rhythmParsed = RhythmLyricsParser.parseTtmlLyrics(ttmlWord)
        assertEquals(1, rhythmParsed.size)

        val wordByWordLines = RhythmLyricsParser.parseWordByWordLyrics(
            com.google.gson.Gson().toJson(rhythmParsed)
        )
        assertEquals(1, wordByWordLines.size)
        assertTrue("TTML with Word timing must have word timing", RhythmLyricsParser.hasWordTiming(wordByWordLines))
        assertTrue("Line must be word-timed", RhythmLyricsParser.isLineWordTimed(wordByWordLines[0]))
        assertTrue(wordByWordLines[0].words.size >= 5)
    }

    @Test
    fun testTtmlFallbackParserTimingLineIgnoresSpans() {
        val ttmlLineWithSpans = """
            <tt itunes:timing="Line">
                <body>
                    <p begin="00:05.000" end="00:08.000">
                        <span begin="00:05.000" end="00:06.000">Fallback </span>
                        <span begin="00:06.000" end="00:08.000">line 1</span>
                    </p>
                    <p begin="00:08.500" end="00:11.000">Fallback line 2</p>
                </body>
            </tt>
        """.trimIndent()

        val fallbackLines = RhythmLyricsParser.parseTtmlFallback(ttmlLineWithSpans)
        assertEquals(2, fallbackLines.size)
        val wordByWord = RhythmLyricsParser.parseWordByWordLyrics(
            com.google.gson.Gson().toJson(fallbackLines)
        )
        assertFalse(RhythmLyricsParser.hasWordTiming(wordByWord))
    }

    @Test
    fun testConvertSemanticLyricsToWordByWordRequiresRealWordTiming() {
        val lineOnlyLyrics = SemanticLyrics.SyncedLyrics(
            listOf(
                SemanticLyrics.LyricLine(
                    text = "Line without word timestamps",
                    start = 10000uL,
                    end = 13000uL,
                    endIsImplicit = false,
                    words = null,
                    speaker = null,
                    isTranslated = false
                )
            )
        )

        val converted = LrcUtils.convertSemanticLyricsToWordByWord(lineOnlyLyrics)
        assertNull("convertSemanticLyricsToWordByWord must return null when there is no word timing", converted)

        val wordTimedLyrics = SemanticLyrics.SyncedLyrics(
            listOf(
                SemanticLyrics.LyricLine(
                    text = "Word timed line",
                    start = 10000uL,
                    end = 13000uL,
                    endIsImplicit = false,
                    words = mutableListOf(
                        SemanticLyrics.Word(10000uL, 10500uL, 0..3, false),
                        SemanticLyrics.Word(10500uL, 11000uL, 5..9, false),
                        SemanticLyrics.Word(11000uL, 13000uL, 11..14, false)
                    ),
                    speaker = null,
                    isTranslated = false
                )
            )
        )
        val convertedWordTimed = LrcUtils.convertSemanticLyricsToWordByWord(wordTimedLyrics)
        assertNotNull("convertSemanticLyricsToWordByWord must succeed when real word timing exists", convertedWordTimed)
    }
}
