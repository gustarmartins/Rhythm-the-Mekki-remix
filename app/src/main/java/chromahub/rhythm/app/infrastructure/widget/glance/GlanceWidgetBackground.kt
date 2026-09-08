/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider

/**
 * Shared "cookie" widget background used by the music and lyrics widgets so their background
 * matches the stats widget: the Material 3 12-sided cookie shape stretched to the widget bounds
 * and tinted with the given color.
 */
@Composable
internal fun CookieWidgetBackground(
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val uiMode = context.resources.configuration.uiMode
    val size = LocalSize.current
    val widthDp = size.width.value.toInt().coerceAtLeast(1)
    val heightDp = size.height.value.toInt().coerceAtLeast(1)
    val bitmap = remember(widthDp, heightDp, uiMode) {
        GlanceShapeBitmaps.create(context, widthDp, heightDp, MaterialShapes.Cookie12Sided)
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        colorFilter = ColorFilter.tint(color)
    )
}
