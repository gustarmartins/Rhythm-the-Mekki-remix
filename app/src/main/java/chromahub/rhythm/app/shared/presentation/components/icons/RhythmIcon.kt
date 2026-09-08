/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.icons

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import chromahub.rhythm.app.R
import java.util.concurrent.ConcurrentHashMap

private const val MaterialSymbolGlyphScale = 0.875f

/**
 * Thread-safe cache and fallback provider for Material Symbols font family.
 *
 * Speeds up font resolution and avoids heavy TTF re-parsing during recompositions.
 * Bypasses Compose's dynamic/asynchronous resolution entirely when standard loading fails
 * or on older SDK versions by wrapping the statically preloaded native Android Typeface.
 */
private object RhythmIconFontCache {
    private val cache = ConcurrentHashMap<String, FontFamily>()
    private var fallbackFontFamily: FontFamily? = null
    private val lock = Any()

    fun getFallbackFontFamily(context: Context): FontFamily {
        fallbackFontFamily?.let { return it }
        synchronized(lock) {
            fallbackFontFamily?.let { return it }
            val tf = try {
                ResourcesCompat.getFont(context, R.font.material_symbols_outlined)
            } catch (e: Throwable) {
                null
            }
            val ff = if (tf != null) {
                FontFamily(tf)
            } else {
                FontFamily(Font(R.font.material_symbols_outlined))
            }
            fallbackFontFamily = ff
            return ff
        }
    }

    fun getOrCreate(
        context: Context,
        filled: Boolean,
        weight: Int,
        grade: Float,
        opticalSize: Float
    ): FontFamily {
        val manufacturer = Build.MANUFACTURER.lowercase(java.util.Locale.US)
        val isXiaomiDevice = manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco")

        if (isXiaomiDevice) {
            return getFallbackFontFamily(context)
        }

        val key = "$filled-$weight-$grade-$opticalSize"
        return cache.getOrPut(key) {
            try {
                FontFamily(
                    Font(
                        resId = R.font.material_symbols_outlined,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight(weight),
                            FontVariation.Setting("FILL", if (filled) 1f else 0f),
                            FontVariation.Setting("GRAD", grade),
                            FontVariation.Setting("opsz", opticalSize)
                        )
                    )
                )
            } catch (e: Throwable) {
                getFallbackFontFamily(context)
            }
        }
    }
}

/**
 * Representation of a Material Symbol icon in the Rhythm app.
 *
 * Material Symbols use variable font technology, supporting four dynamic axes:
 * - **FILL**: 0 (outlined) to 1 (filled)
 * - **wght** (Weight): 100 to 700
 * - **GRAD** (Grade): -25 to 200
 * - **opsz** (Optical Size): 20 to 48
 *
 * All icons are rendered using the bundled Material Symbols Outlined variable TTF font.
 */
data class MaterialSymbolIcon(
    /** The ligature name of the icon (e.g. "play_arrow", "skip_next") */
    val name: String,
    /** Whether the icon should render in filled style (FILL axis = 1) */
    val filled: Boolean = false,
    /** Default weight for this icon (overridable at render time) */
    val defaultWeight: Int = 400
) {
    /** Returns a copy of this icon with filled style */
    fun filled(): MaterialSymbolIcon = copy(filled = true)

    /** Returns a copy of this icon with outlined style */
    fun outlined(): MaterialSymbolIcon = copy(filled = false)
}

/**
 * Creates a [FontFamily] for Material Symbols with the given variation settings.
 * Results are cached via [remember] and a static global cache map to avoid re-creating/re-parsing font objects.
 */
@Composable
fun rememberSymbolsFontFamily(
    filled: Boolean = false,
    weight: Int = 400,
    grade: Float = 0f,
    opticalSize: Float = 24f
): FontFamily {
    val context = LocalContext.current
    return remember(filled, weight, grade, opticalSize) {
        RhythmIconFontCache.getOrCreate(context, filled, weight, grade, opticalSize)
    }
}

/**
 * Renders a [MaterialSymbolIcon] using the Material Symbols variable font.
 *
 * This composable replaces `androidx.compose.material3.Icon` for Material Symbol icons,
 * rendering them as text glyphs via OpenType ligatures with full variable axis control.
 *
 * @param icon The [MaterialSymbolIcon] to render.
 * @param contentDescription Accessibility description for the icon.
 * @param modifier Standard [Modifier] applied to the icon container.
 * @param tint Color applied to the icon glyph.
 * @param size The display size of the icon (also sets the optical size axis).
 * @param weight Font weight for the icon (100–700).
 * @param grade Font grade for fine-tuning stroke weight (-25 to 200).
 */
@Composable
fun Icon(
    icon: MaterialSymbolIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    weight: Int = icon.defaultWeight,
    grade: Float = 0f
) {
    val fontFamily = rememberSymbolsFontFamily(
        filled = icon.filled,
        weight = weight,
        grade = grade,
        opticalSize = size.value * MaterialSymbolGlyphScale
    )

    BoxWithConstraints(
        modifier = modifier.semantics(mergeDescendants = true) {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        val slotSize = if (maxWidth != Dp.Infinity && maxHeight != Dp.Infinity) {
            minOf(maxWidth, maxHeight)
        } else {
            size
        }
        val glyphSize = slotSize * MaterialSymbolGlyphScale

        androidx.compose.material3.Text(
            text = icon.name,
            fontFamily = fontFamily,
            fontSize = glyphSize.value.sp,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(
                lineHeight = glyphSize.value.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

// ──────────────────────────────────────────────────────
// Compatibility overloads
// ──────────────────────────────────────────────────────
// These allow call sites to use our custom Icon alongside standard signatures.
// Files can import `chromahub...icons.Icon` and use it for all icon variants.

/**
 * Overload accepting [MaterialSymbolIcon] via the `imageVector` parameter name.
 * This preserves call-site compatibility with existing code like:
 * ```
 * Icon(imageVector = RhythmIcons.Play, contentDescription = "Play")
 * ```
 */
@Composable
fun Icon(
    imageVector: MaterialSymbolIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Icon(
        icon = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/**
 * Re-export: standard [ImageVector] icon rendering (delegates to Material 3).
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/**
 * Re-export: [Painter]-based icon rendering (delegates to Material 3).
 */
@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/**
 * Re-export: [ImageBitmap]-based icon rendering (delegates to Material 3).
 */
@Composable
fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    androidx.compose.material3.Icon(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
