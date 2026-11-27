/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.infrastructure.service.player.replaygain

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

@UnstableApi
class PcmGainTest {
    private fun scale(bytes: ByteArray, encoding: Int, frames: Int, initial: Float, final: Float = initial, channels: Int = 1): ByteArray {
        val src = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val dest = ByteBuffer.allocate(bytes.size).order(ByteOrder.nativeOrder())
        scalePcm(src, dest, encoding, channels, frames, initial, final)
        assertFalse(src.hasRemaining())
        assertEquals(bytes.size, dest.position())
        assertEquals(ByteOrder.nativeOrder(), src.order())
        assertEquals(ByteOrder.nativeOrder(), dest.order())
        return dest.array()
    }
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun eightBitSilenceStaysCenteredAtEveryGain() {
        assertArrayEquals(bytes(64, 127, 128, 129, 191), scale(bytes(0, 126, 128, 130, 255), C.ENCODING_PCM_8BIT, 5, .5f))
        assertArrayEquals(bytes(0, 64, 128, 192, 255), scale(bytes(0, 96, 128, 160, 255), C.ENCODING_PCM_8BIT, 5, 2f))
    }
    @Test fun sixteenBitOverRangeSaturatesInBothByteOrders() {
        assertArrayEquals(bytes(0xff, 0x7f, 0, 0x80, 0x68, 0x24),
            scale(bytes(0, 0x70, 0, 0x90, 0x34, 0x12), C.ENCODING_PCM_16BIT, 3, 2f))
        assertArrayEquals(bytes(0x7f, 0xff, 0x80, 0, 0x24, 0x68),
            scale(bytes(0x70, 0, 0x90, 0, 0x12, 0x34), C.ENCODING_PCM_16BIT_BIG_ENDIAN, 3, 2f))
    }
    @Test fun twentyFourBitOverRangeSaturatesInBothByteOrders() {
        assertArrayEquals(bytes(0xff, 0xff, 0x7f, 0, 0, 0x80, 6, 4, 2),
            scale(bytes(0, 0, 0x70, 0, 0, 0x90, 3, 2, 1), C.ENCODING_PCM_24BIT, 3, 2f))
        assertArrayEquals(bytes(0x7f, 0xff, 0xff, 0x80, 0, 0, 2, 4, 6),
            scale(bytes(0x70, 0, 0, 0x90, 0, 0, 1, 2, 3), C.ENCODING_PCM_24BIT_BIG_ENDIAN, 3, 2f))
    }
    @Test fun thirtyTwoBitEncodingKeepsLowBitsWhenAttenuated() {
        assertArrayEquals(bytes(1, 0, 0, 0x20),
            scale(bytes(2, 0, 0, 0x40), C.ENCODING_PCM_32BIT, 1, .5f))
        assertArrayEquals(bytes(0x20, 0, 0, 1),
            scale(bytes(0x40, 0, 0, 2), C.ENCODING_PCM_32BIT_BIG_ENDIAN, 1, .5f))
    }
    @Test fun gainChangesRampPerFrameWithEqualGainInBothChannels() {
        val sample = bytes(0, 0x40, 0, 0x40)
        assertArrayEquals(bytes(0, 0x40, 0, 0x40, 0xff, 0x7f, 0xff, 0x7f),
            scale(sample + sample, C.ENCODING_PCM_16BIT, 2, 0f, 2f, channels = 2))
    }
    @Test fun floatOutputRetainsHeadroomAndSign() {
        val pcm = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder()).putFloat(.75f).putFloat(-.875f)
        val scaled = ByteBuffer.wrap(scale(pcm.array(), C.ENCODING_PCM_FLOAT, 2, 2f)).order(ByteOrder.nativeOrder())
        assertEquals(1.5f, scaled.float, 0f)
        assertEquals(-1.75f, scaled.float, 0f)
    }
}
