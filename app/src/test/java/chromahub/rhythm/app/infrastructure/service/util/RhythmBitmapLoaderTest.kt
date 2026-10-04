/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service.util

import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import chromahub.rhythm.app.util.ImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ExecutionException

@OptIn(UnstableApi::class)
class RhythmBitmapLoaderTest {

    @Test
    fun testDefaultMaxDimension() {
        assertEquals(512, RhythmBitmapLoader.DEFAULT_MAX_DIMENSION)
    }

    @Test
    fun testCalculateInSampleSizeDownscaling() {
        assertEquals(1, ImageUtils.calculateInSampleSize(512, 512, 512))
        assertEquals(1, ImageUtils.calculateInSampleSize(300, 300, 512))
        assertEquals(2, ImageUtils.calculateInSampleSize(1024, 1024, 512))
        assertEquals(4, ImageUtils.calculateInSampleSize(2048, 2048, 512))
        assertEquals(8, ImageUtils.calculateInSampleSize(4096, 4096, 512))
        assertEquals(4, ImageUtils.calculateInSampleSize(2048, 800, 512))
        assertEquals(4, ImageUtils.calculateInSampleSize(800, 2048, 512))
        assertEquals(1, ImageUtils.calculateInSampleSize(0, 0, 512))
        assertEquals(1, ImageUtils.calculateInSampleSize(-100, 500, 512))
        assertEquals(1, ImageUtils.calculateInSampleSize(1000, 1000, 0))
    }

    @Test
    fun testLoadBitmapFromMetadataWithoutArtworkReturnsFailedFuture() {
        val metadata = MediaMetadata.Builder().setTitle("Test Song").build()
        val loader = RhythmBitmapLoader()
        val future = loader.loadBitmapFromMetadata(metadata)
        assertTrue(future.isDone)
        var failed = false
        try {
            future.get()
        } catch (e: ExecutionException) {
            failed = true
            assertTrue(e.cause is IllegalArgumentException)
        }
        assertTrue(failed)
    }

    @Test
    fun testLoadBitmapFromMetadataEmptyFailsGracefully() {
        val loader = RhythmBitmapLoader(context = null)
        val metadata = MediaMetadata.Builder().build()
        val future = loader.loadBitmapFromMetadata(metadata)
        assertTrue(future.isDone)
    }

    @Test
    fun testSupportsMimeType() {
        val loader = RhythmBitmapLoader()
        assertTrue(loader.supportsMimeType("image/jpeg"))
        assertTrue(loader.supportsMimeType("image/png"))
        assertTrue(loader.supportsMimeType("image/webp"))
        assertTrue(loader.supportsMimeType("IMAGE/JPEG"))
        assertFalse(loader.supportsMimeType("audio/mpeg"))
        assertFalse(loader.supportsMimeType("video/mp4"))
    }
}
