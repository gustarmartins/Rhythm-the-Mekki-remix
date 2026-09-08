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

class RomanizationParsingTest {

    @Test
    fun testRomanizationWithJapaneseCommaIsCorrectlyDetected() {
        val lrcContent = """
            [00:00.58]泣き声、遠く 息を合わせて、もう一度
            [00:04.43]そんな、僕らの未来を強く願う歌
            [00:00.58]na ki go e、to o ku i ki wo a wa se te、mo u i chi do
            [00:04.43]so n na、bo ku ra no mi ra i wo tsu yo ku ne ga u u ta
        """.trimIndent()

        val parsedLines = LyricsParser.parseLyrics(lrcContent)
        assertEquals(2, parsedLines.size)

        val firstLine = parsedLines[0]
        assertEquals("泣き声、遠く 息を合わせて、もう一度", firstLine.text)
        assertNotNull("Romanization should be present", firstLine.romanization)
        assertEquals("na ki go e、to o ku i ki wo a wa se te、mo u i chi do", firstLine.romanization)
        assertNull("Translation should be null", firstLine.translation)

        val secondLine = parsedLines[1]
        assertEquals("そんな、僕らの未来を強く願う歌", secondLine.text)
        assertNotNull("Romanization should be present", secondLine.romanization)
        assertEquals("so n na、bo ku ra no mi ra i wo tsu yo ku ne ga u u ta", secondLine.romanization)
        assertNull("Translation should be null", secondLine.translation)
    }

    @Test
    fun testScriptDetectionHelpers() {
        assertTrue(LyricsParser.hasNonLatinScript("泣き声、遠く 息を合わせて、もう一度"))
        assertTrue(LyricsParser.hasNonLatinScript("한국어 가사"))
        assertTrue(LyricsParser.hasNonLatinScript("Русский текст"))
        assertFalse(LyricsParser.hasNonLatinScript("na ki go e、to o ku i ki wo a wa se te、mo u i chi do"))
        assertFalse(LyricsParser.hasNonLatinScript("Hello World! 123, .?!"))

        assertTrue(LyricsParser.isLatinBased("na ki go e、to o ku i ki wo a wa se te、mo u i chi do"))
        assertTrue(LyricsParser.isLatinBased("Tōkyō de aō"))
        assertTrue(LyricsParser.isLatinBased("English lyrics with punctuation: , . ! ?"))
        assertFalse(LyricsParser.isLatinBased("泣き声、遠く"))
        assertFalse(LyricsParser.isLatinBased("한국어"))
    }
}
