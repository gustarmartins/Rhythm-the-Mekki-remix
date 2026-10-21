package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.CanvasNetworkMode
import chromahub.rhythm.app.shared.data.model.LyricsSourcePreference
import org.junit.Assert.*
import org.junit.Test

class NewUserDefaultsTest {
    @Test fun cleanInstallHasOnlineLyricsAndOwnerPlayerDefaultsWithoutLibraryWriteConsent() {
        val values = NewUserDefaults.forEmptyPreferences(emptyMap<String, Any>(), mapOf("lrclib_api_enabled" to true))
        assertEquals(true, values["bluetooth_lyrics_enabled"])
        assertEquals(20, values["bluetooth_lyrics_max_chunk_chars"])
        assertEquals(0, values["bluetooth_lyrics_offset_ms"])
        assertEquals(0, values["bluetooth_lyrics_scroll_chars_per_second"])
        assertEquals(600, values["bluetooth_lyrics_metadata_update_interval_ms"])
        assertEquals(LyricsSourcePreference.API_FIRST.ordinal, values["lyrics_source_preference"])
        assertEquals(true, values["integrations_enabled"])
        assertEquals(true, values["show_lyrics"])
        assertEquals(true, values["broadcast_status_enabled"])
        assertEquals("EXPRESSIVE", values["player_theme_id"])
        assertEquals("MATERIAL", values["miniplayer_theme_id"])
        assertEquals(true, values["player_ambient_backdrop_enabled"])
        assertEquals(false, values["player_accent_background_enabled"])
        assertEquals(0.35f, values["player_ambient_backdrop_intensity"])
        assertEquals(true, values["player_ambient_infinite_zoom"])
        assertEquals(false, values["enable_album_editing"])
        assertEquals(true, values["lrclib_api_enabled"])
        assertEquals(false, values["artwork_allow_mobile_data"])
        assertEquals(false, values["artwork_setup_completed"])
        assertEquals(false, values["apple_canvas_enabled"])
        assertEquals(CanvasNetworkMode.WIFI_ONLY.ordinal, values["apple_canvas_network_mode"])
    }

    @Test fun oldInstallKeepsExplicitChoicesAndMissingLegacyDefaults() {
        val existing = mapOf("onboarding_completed" to true, "integrations_enabled" to false,
            "bluetooth_lyrics_max_chunk_chars" to 16, "bluetooth_lyrics_offset_ms" to 500,
            "apple_canvas_network_mode" to CanvasNetworkMode.BOTH.ordinal)
        assertTrue(NewUserDefaults.forEmptyPreferences(existing, mapOf("lrclib_api_enabled" to true)).isEmpty())
        assertTrue(NewUserDefaults.forEmptyPreferences(mapOf("onboarding_completed" to false), emptyMap()).isEmpty())
    }

    @Test fun defaultsAreSeededOnlyOnceAndDoNotEnableUnsupportedProviders() {
        val saved = NewUserDefaults.forEmptyPreferences(emptyMap<String, Any>(), mapOf("lrclib_api_enabled" to false))
        assertEquals(false, saved["lrclib_api_enabled"])
        assertTrue(NewUserDefaults.forEmptyPreferences(saved, mapOf("lrclib_api_enabled" to true)).isEmpty())
    }

    @Test fun staticDisplayDefaultsNeverRelyOnScrollingForLongLines() {
        val values = NewUserDefaults.forEmptyPreferences(emptyMap<String, Any>(), emptyMap())
        val tuning = BluetoothLyricsFormatter.Tuning(
            maxChunkChars = values["bluetooth_lyrics_max_chunk_chars"] as Int,
            scrollCharsPerSecond = values["bluetooth_lyrics_scroll_chars_per_second"] as Int)
        val chunks = BluetoothLyricsFormatter.chunksForLine("Read every word on a tiny stationary lyric display", 8000, tuning)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 20 })
        assertEquals("Read every word on a tiny stationary lyric display", chunks.joinToString(" "))
    }
}
