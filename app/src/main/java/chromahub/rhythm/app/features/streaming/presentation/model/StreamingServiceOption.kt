/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.presentation.model

import androidx.annotation.StringRes
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId

data class StreamingServiceOption(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int
)

object StreamingServiceOptions {
    const val SUBSONIC = StreamingServiceId.SUBSONIC
    const val JELLYFIN = StreamingServiceId.JELLYFIN
    const val MUSIC_ASSISTANT = StreamingServiceId.MUSIC_ASSISTANT
    val defaults: List<StreamingServiceOption> = listOf(
        StreamingServiceOption(
            id = SUBSONIC,
            nameRes = R.string.streaming_service_subsonic,
            descriptionRes = R.string.streaming_service_subsonic_desc
        ),
        StreamingServiceOption(
            id = JELLYFIN,
            nameRes = R.string.streaming_service_jellyfin,
            descriptionRes = R.string.streaming_service_jellyfin_desc
        ),
        StreamingServiceOption(MUSIC_ASSISTANT, R.string.music_assistant_title, R.string.music_assistant_native_description),
    )
}
