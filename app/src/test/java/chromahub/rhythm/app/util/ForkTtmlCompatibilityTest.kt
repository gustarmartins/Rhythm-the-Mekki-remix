package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.BluetoothLyricsTextMode
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForkTtmlCompatibilityTest {
    @Test
    fun upstreamTtmlTimingSurvivesForkProviderAndBluetoothPipeline() {
        val payload = JsonObject().apply {
            addProperty("ttmlContent", """
                <tt xmlns="http://www.w3.org/ns/ttml">
                  <body><div>
                    <p begin="00:10.000" end="00:13.000">A little light along the road</p>
                    <p begin="00:14.000" end="00:17.000">Another line</p>
                  </div></body>
                </tt>
            """.trimIndent())
        }
        val lyrics = LyricallyApiParser.parseLyricsResponse(payload, "Better Lyrics")
        assertNotNull(lyrics)
        val parsed = LyricsParser.parseLyrics(lyrics!!.syncedLyrics!!)
        assertEquals(2, parsed.size)
        assertEquals(10_000L, parsed[0].timestamp)
        val chunks = BluetoothLyricsFormatter.chunksForLine(
            "A little light along the road", 3_000L,
            BluetoothLyricsFormatter.Tuning(19, 0, 350)
        )
        assertTrue(chunks.all { it.length <= 19 })
        assertEquals("A little light along the road", chunks.joinToString(" "))
    }

    @Test
    fun ttmlWithoutSupplementDoesNotReplaceCarRomajiWithJapanese() {
        val payload = JsonObject().apply {
            addProperty("ttmlContent", """
                <tt xmlns="http://www.w3.org/ns/ttml"><body><div>
                  <p begin="00:10.000" end="00:13.000">光</p>
                  <p begin="00:14.000" end="00:17.000">夢</p>
                </div></body></tt>
            """.trimIndent())
        }
        val lyrics = LyricallyApiParser.parseLyricsResponse(payload, "Better Lyrics")
        assertNotNull(lyrics)
        assertTrue(!lyrics!!.hasUsableTimedRomanization())
        assertEquals(listOf("", ""), BluetoothLyricsFormatter.selectTexts(
            BluetoothLyricsTextMode.ROMANIZATION,
            listOf("光", "夢"), emptyList(), emptyList()
        ))
    }
}
