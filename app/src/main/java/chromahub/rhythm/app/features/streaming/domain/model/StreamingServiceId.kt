/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.domain.model

object StreamingServiceId {
    const val SUBSONIC = "SUBSONIC"
    const val JELLYFIN = "JELLYFIN"
    val all = listOf(
        SUBSONIC,
        JELLYFIN
    )
}

object StreamingServiceRules {
    fun requiresServerUrl(serviceId: String): Boolean {
        return serviceId == StreamingServiceId.SUBSONIC || serviceId == StreamingServiceId.JELLYFIN
    }
}
