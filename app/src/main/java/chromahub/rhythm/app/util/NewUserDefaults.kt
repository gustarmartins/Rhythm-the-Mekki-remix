package chromahub.rhythm.app.util

/** Seed only genuinely empty preferences. Existing explicit and implicit choices stay intact. */
internal object NewUserDefaults {
    fun forEmptyPreferences(existing: Map<String, *>, supportedApis: Map<String, Boolean>): Map<String, Any> {
        if (existing.isNotEmpty()) return emptyMap()
        return mapOf<String, Any>(
            "new_user_defaults_version" to 1,
            "bluetooth_lyrics_enabled" to true,
            "bluetooth_lyrics_max_chunk_chars" to 20,
            "bluetooth_lyrics_offset_ms" to 0,
            "bluetooth_lyrics_scroll_chars_per_second" to 0,
            "bluetooth_lyrics_metadata_update_interval_ms" to 600,
            "lyrics_source_preference" to 2, // LyricsSourcePreference.LOCAL_FIRST.
            "integrations_enabled" to true,
            "auto_fetch_artwork" to true,
            "artwork_allow_mobile_data" to false,
            "artwork_setup_completed" to false,
            "apple_canvas_enabled" to false,
            "apple_canvas_network_mode" to 0 // CanvasNetworkMode.WIFI_ONLY; persisted ordinal.
        ) + supportedApis
    }
}
