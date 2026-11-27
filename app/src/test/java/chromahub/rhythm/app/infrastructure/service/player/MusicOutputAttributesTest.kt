/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.infrastructure.service.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class MusicOutputAttributesTest {
    @Test fun enabledPreservesTheOriginalMedia3MusicAttributes() {
        val previous = AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build()
        assertEquals(previous, musicOutputAttributes(true))
    }

    @Test fun disablingSystemSpatialAudioPreservesMediaUsageAndCapturePolicy() {
        val enabled = musicOutputAttributes(true)
        val disabled = musicOutputAttributes(false)
        assertEquals(C.SPATIALIZATION_BEHAVIOR_NEVER, disabled.spatializationBehavior)
        assertEquals(enabled.usage, disabled.usage)
        assertEquals(enabled.contentType, disabled.contentType)
        assertEquals(enabled.flags, disabled.flags)
        assertEquals(enabled.allowedCapturePolicy, disabled.allowedCapturePolicy)
    }
}
