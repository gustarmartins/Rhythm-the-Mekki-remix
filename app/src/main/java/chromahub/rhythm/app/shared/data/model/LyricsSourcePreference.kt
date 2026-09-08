/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.data.model

/**
 * Preference for lyrics source priority order
 */
enum class LyricsSourcePreference(val displayName: String) {
    /**
     * Try online API first, then embedded lyrics, then local .lrc files
     */
    API_FIRST("API"),
    
    /**
     * Try embedded lyrics in metadata first, then API, then local .lrc files
     */
    EMBEDDED_FIRST("Embedded"),
    
    /**
     * Try local .lrc files first, then embedded lyrics, then API
     */
    LOCAL_FIRST("Local");
    
    companion object {
        fun fromOrdinal(ordinal: Int): LyricsSourcePreference {
            return values().getOrElse(ordinal) { API_FIRST }
        }
    }
}
