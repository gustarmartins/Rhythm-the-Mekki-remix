/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Renders Material 3 Expressive polygons (MaterialShapes) into cached Bitmaps so they can be
 * displayed inside Glance widgets via [androidx.glance.ImageProvider]. White shape bitmaps are
 * tinted through Glance's ColorFilter, optionally clipped to a source image (e.g. album art).
 */
internal object GlanceShapeBitmaps {

    // Bounded so that resizing widgets (which generate a bitmap per size) can't accumulate
    // large album-art copies in memory.
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun clearCache() {
        cache.evictAll()
    }

    fun create(
        context: Context,
        sizeDp: Int,
        shape: RoundedPolygon,
        alpha: Int = 255,
        sourceBitmap: Bitmap? = null
    ): Bitmap = create(context, sizeDp, sizeDp, shape, alpha, sourceBitmap)

    /**
     * Square or stretched variant of [create]. Distinct width/height lets widgets render the
     * 12-sided cookie stretched to their bounds (used by the music/lyrics widget backgrounds).
     */
    fun create(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        shape: RoundedPolygon,
        alpha: Int = 255,
        sourceBitmap: Bitmap? = null
    ): Bitmap {
        val uiMode = context.resources.configuration.uiMode
        val cacheKey = if (sourceBitmap != null) {
            "src_${sourceBitmap.hashCode()}_${widthDp}x${heightDp}_${shape.hashCode()}_$uiMode"
        } else {
            "shape_${widthDp}x${heightDp}_${shape.hashCode()}_$uiMode"
        }
        cache.get(cacheKey)?.let { if (!it.isRecycled) return it }

        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = shape.toPath()
        val matrix = Matrix()
        matrix.setScale(widthPx.toFloat(), heightPx.toFloat())
        path.transform(matrix)

        val paint = Paint().apply {
            color = Color.WHITE
            this.alpha = alpha
            isAntiAlias = true
            isFilterBitmap = true
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)

        sourceBitmap?.let { source ->
            // Coil may return hardware bitmaps (Bitmap.Config.HARDWARE), which cannot be drawn
            // onto a software Canvas. Convert to a software bitmap first.
            val swSource = if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }
            if (swSource != null && swSource.width > 0 && swSource.height > 0) {
                // Fill the cookie without stretching: center-crop the source so the
                // artwork keeps its aspect ratio and just fills the shape.
                val srcW = swSource.width
                val srcH = swSource.height
                val scale = maxOf(widthPx.toFloat() / srcW, heightPx.toFloat() / srcH)
                val scaledW = (srcW * scale).toInt()
                val scaledH = (srcH * scale).toInt()
                val left = (widthPx - scaledW) / 2
                val top = (heightPx - scaledH) / 2
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(swSource, null, Rect(left, top, left + scaledW, top + scaledH), paint)
            }
        }

        cache.put(cacheKey, bitmap)
        return bitmap
    }
}
