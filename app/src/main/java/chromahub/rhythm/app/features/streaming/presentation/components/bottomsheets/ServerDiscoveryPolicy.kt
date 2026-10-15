/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.presentation.components.bottomsheets

import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId

internal object ServerDiscoveryPolicy {
    fun serviceTypes(service: String): List<String> = listOf(when (service) {
        StreamingServiceId.MUSIC_ASSISTANT -> "_mass._tcp"
        StreamingServiceId.JELLYFIN -> "_jellyfin._tcp"
        StreamingServiceId.SUBSONIC -> "_subsonic._tcp"
        else -> "_http._tcp"
    }, "_http._tcp").distinct()

    // NSD usually supplies the port only after resolution. Match generic HTTP by
    // advertised name, and accept dedicated service types even with opaque names.
    fun matches(service: String, type: String, name: String): Boolean {
        if (type != "_http._tcp") return type == serviceTypes(service).first()
        val lower = name.lowercase()
        return when (service) {
            StreamingServiceId.MUSIC_ASSISTANT -> "music assistant" in lower || "music-assistant" in lower || lower == "mass"
            StreamingServiceId.JELLYFIN -> listOf("jellyfin", "emby", "media", "server", "arch").any { it in lower }
            StreamingServiceId.SUBSONIC -> listOf("subsonic", "navidrome", "airsonic", "music", "server").any { it in lower }
            else -> false
        }
    }
}
