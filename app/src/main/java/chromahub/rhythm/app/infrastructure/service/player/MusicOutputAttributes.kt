/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi

/** Both local players use the same spatialization policy through a transition. */
@UnstableApi
internal fun musicOutputAttributes(allowSpatialAudio: Boolean): AudioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .setSpatializationBehavior(if (allowSpatialAudio) C.SPATIALIZATION_BEHAVIOR_AUTO else C.SPATIALIZATION_BEHAVIOR_NEVER)
    .build()
