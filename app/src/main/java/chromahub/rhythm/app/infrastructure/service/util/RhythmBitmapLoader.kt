/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import chromahub.rhythm.app.util.ImageUtils
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Arrays

/**
 * Custom [BitmapLoader] for MediaSession and notifications that bounds artwork dimensions,
 * ensures software bitmaps, and protects against recycled bitmap propagation.
 */
@OptIn(UnstableApi::class)
class RhythmBitmapLoader(
    private val context: Context? = null,
    private val maxDimension: Int = DEFAULT_MAX_DIMENSION
) : BitmapLoader {

    companion object {
        private const val TAG = "RhythmBitmapLoader"
        const val DEFAULT_MAX_DIMENSION = 512
    }

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cacheLock = Any()
    @Volatile
    private var lastUriKey: String? = null
    @Volatile
    private var lastDataHash: Int? = null
    @Volatile
    private var cachedBitmap: Bitmap? = null

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val hash = Arrays.hashCode(data)
        synchronized(cacheLock) {
            if (lastDataHash == hash) {
                val current = cachedBitmap
                if (current != null && !current.isRecycled) {
                    createDefensiveCopy(current)?.let { copy ->
                        return Futures.immediateFuture(copy)
                    }
                } else {
                    Log.w(TAG, "Cached artwork from byte data was recycled or invalid, invalidating cache")
                    clearCacheLocked()
                }
            }
        }

        val future = SettableFuture.create<Bitmap>()
        loaderScope.launch {
            try {
                val peekOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, peekOpts)
                if (peekOpts.outWidth <= 0 || peekOpts.outHeight <= 0) {
                    future.setException(IllegalArgumentException("Invalid image dimensions in byte array"))
                    return@launch
                }

                val sample = ImageUtils.calculateInSampleSize(peekOpts.outWidth, peekOpts.outHeight, maxDimension)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, opts)
                if (decoded != null && !decoded.isRecycled) {
                    val softwareBmp = if (decoded.config != Bitmap.Config.ARGB_8888) {
                        decoded.copy(Bitmap.Config.ARGB_8888, false) ?: decoded
                    } else {
                        decoded
                    }
                    synchronized(cacheLock) {
                        cachedBitmap = softwareBmp
                        lastDataHash = hash
                        lastUriKey = null
                    }
                    val copy = createDefensiveCopy(softwareBmp) ?: softwareBmp
                    future.set(copy)
                } else {
                    future.setException(IllegalStateException("Failed to decode byte array to bitmap"))
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to decode bitmap from data", t)
                future.setException(t)
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val key = uri.toString()
        synchronized(cacheLock) {
            if (lastUriKey == key) {
                val current = cachedBitmap
                if (current != null && !current.isRecycled) {
                    createDefensiveCopy(current)?.let { copy ->
                        return Futures.immediateFuture(copy)
                    }
                } else {
                    Log.w(TAG, "Cached artwork for URI $key was recycled or invalid, invalidating cache")
                    clearCacheLocked()
                }
            }
        }

        val appContext = context ?: return Futures.immediateFailedFuture(
            IllegalStateException("Context is required to load bitmap from URI")
        )

        val future = SettableFuture.create<Bitmap>()
        loaderScope.launch {
            try {
                val loaded = ImageUtils.loadArtworkBitmap(appContext, uri, maxDimension)
                if (loaded != null && !loaded.isRecycled) {
                    val softwareBmp = if (loaded.config != Bitmap.Config.ARGB_8888) {
                        loaded.copy(Bitmap.Config.ARGB_8888, false) ?: loaded
                    } else {
                        loaded
                    }
                    synchronized(cacheLock) {
                        cachedBitmap = softwareBmp
                        lastUriKey = key
                        lastDataHash = null
                    }
                    val copy = createDefensiveCopy(softwareBmp) ?: softwareBmp
                    future.set(copy)
                } else {
                    future.setException(IllegalStateException("Failed to load bitmap from uri: $uri"))
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load bitmap from uri: $uri", t)
                future.setException(t)
            }
        }
        return future
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/", ignoreCase = true)
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        return if (metadata.artworkData != null) {
            decodeBitmap(metadata.artworkData!!)
        } else if (metadata.artworkUri != null) {
            loadBitmap(metadata.artworkUri!!)
        } else {
            Futures.immediateFailedFuture(IllegalArgumentException("MediaMetadata contains neither artworkData nor artworkUri"))
        }
    }

    private fun createDefensiveCopy(bmp: Bitmap): Bitmap? {
        return try {
            if (!bmp.isRecycled) {
                bmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to make defensive copy of bitmap", t)
            null
        }
    }

    private fun clearCacheLocked() {
        cachedBitmap = null
        lastUriKey = null
        lastDataHash = null
    }
}
