/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.domain.model

object StreamingServiceId {
    const val SUBSONIC = "SUBSONIC"
    const val JELLYFIN = "JELLYFIN"
    const val MUSIC_ASSISTANT = "MUSIC_ASSISTANT"
    val all = listOf(
        SUBSONIC,
        JELLYFIN,
        MUSIC_ASSISTANT
    )
}

object StreamingServiceRules {
    fun requiresServerUrl(serviceId: String): Boolean {
        return serviceId == StreamingServiceId.SUBSONIC || serviceId == StreamingServiceId.JELLYFIN || serviceId == StreamingServiceId.MUSIC_ASSISTANT
    }
}
