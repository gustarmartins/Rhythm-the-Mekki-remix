/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color // Added back to fix compilation
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.theme.ExpressiveMaterialShape
import chromahub.rhythm.app.shared.presentation.theme.rememberExpressiveShape

/**
 * Preview components for Glance widgets
 * * This file provides previews for all widget sizes and states to help visualize
 * the widget layouts during development. These previews show what the actual
 * Glance widgets will look like on the home screen.
 */

@Composable
private fun WidgetPreviewCard(
    title: String,
    size: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun MockupArt(size: Int, corner: Int = 20) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    )
}

@Composable
private fun MockupTextLines(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun MockupButton(isPlay: Boolean, modifier: Modifier = Modifier, size: Int = 48, corner: Int = 24) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(
                if (isPlay) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.tertiary
            )
    )
}

@Composable
private fun WidgetMockup(
    widthDp: Int,
    heightDp: Int,
    layoutName: String
) {
    val baseModifier = Modifier
        .width(widthDp.dp)
        .height(heightDp.dp)
        .clip(RoundedCornerShape(28.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)

    Box(contentAlignment = Alignment.Center) {
        when (layoutName) {
            "OneByOne" -> {
                Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
                    MockupButton(isPlay = true, modifier = Modifier.fillMaxSize(), size = 0, corner = 30)
                }
            }
            "Gabe" -> {
                Column(
                    modifier = baseModifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MockupArt(size = 48, corner = 24)
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)) {
                        MockupButton(isPlay = false, modifier = Modifier.fillMaxWidth().weight(1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        MockupButton(isPlay = true, modifier = Modifier.fillMaxWidth().weight(1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        MockupButton(isPlay = false, modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
            "Thin" -> {
                Row(modifier = baseModifier, verticalAlignment = Alignment.CenterVertically) {
                    MockupArt(size = heightDp - 32, corner = 16)
                    Spacer(modifier = Modifier.width(14.dp))
                    MockupTextLines(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    MockupButton(isPlay = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    MockupButton(isPlay = false)
                }
            }
            "Small" -> {
                Column(modifier = baseModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    MockupButton(isPlay = true, modifier = Modifier.fillMaxWidth().height(48.dp), size = 0)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                        Spacer(modifier = Modifier.width(10.dp))
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                    }
                }
            }
            "Medium" -> {
                Column(modifier = baseModifier) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MockupArt(size = 84, corner = 18)
                        Spacer(modifier = Modifier.width(14.dp))
                        MockupTextLines(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                        Spacer(modifier = Modifier.width(10.dp))
                        MockupButton(isPlay = true, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                        Spacer(modifier = Modifier.width(10.dp))
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                    }
                }
            }
            else -> {
                Column(modifier = baseModifier) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        MockupArt(size = if (heightDp >= 260) 110 else 72, corner = 20)
                        Spacer(modifier = Modifier.width(16.dp))
                        MockupTextLines(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth().height(if (heightDp >= 260) 64.dp else 60.dp)) {
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                        Spacer(modifier = Modifier.width(12.dp))
                        MockupButton(isPlay = true, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                        Spacer(modifier = Modifier.width(12.dp))
                        MockupButton(isPlay = false, modifier = Modifier.weight(1f).fillMaxHeight(), size = 0)
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = layoutName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun LyricsWidgetMockup(
    widthDp: Int,
    heightDp: Int,
    layoutName: String
) {
    val isCompact = heightDp < 120
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Song title
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Lyrics lines
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isCompact) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Active lyric line (highlighted)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(if (isCompact) 11.dp else 13.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                )
                if (!isCompact) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Controls row
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Next
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

// ==================== Rhythm Cookie Widget Mockup ====================

@Composable
private fun CookieWidgetMockup(
    widthDp: Int,
    heightDp: Int,
    isPlaying: Boolean = true
) {
    val cookieShape = rememberExpressiveShape(ExpressiveMaterialShape.COOKIE_12)
    val sunnyShape = rememberExpressiveShape(ExpressiveMaterialShape.SUNNY)
    val square = minOf(widthDp, heightDp)
    val scale = square / 100f

    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(square.dp)
                .clip(cookieShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Mock album art filling the cookie
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )

            // Play/Pause — sunny badge, top-right corner (center on the cookie rim)
            Box(
                modifier = Modifier
                    .size((36 * scale).dp)
                    .align(Alignment.TopEnd)
                    .padding((2 * scale).dp)
                    .clip(sunnyShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                PlayPauseGlyph(
                    isPlaying = isPlaying,
                    color = MaterialTheme.colorScheme.onPrimary,
                    glyphSize = (14 * scale).dp
                )
            }

            // Previous — circle badge, bottom-left corner
            Box(
                modifier = Modifier
                    .size((26 * scale).dp)
                    .align(Alignment.BottomStart)
                    .padding((6 * scale).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                SkipGlyph(
                    flipped = true,
                    color = MaterialTheme.colorScheme.onTertiary,
                    glyphSize = (11 * scale).dp
                )
            }

            // Next — circle badge, bottom-right corner
            Box(
                modifier = Modifier
                    .size((26 * scale).dp)
                    .align(Alignment.BottomEnd)
                    .padding((6 * scale).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                SkipGlyph(
                    flipped = false,
                    color = MaterialTheme.colorScheme.onTertiary,
                    glyphSize = (11 * scale).dp
                )
            }
        }
    }
}

@Composable
private fun PlayPauseGlyph(isPlaying: Boolean, color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        if (isPlaying) {
            // Two pause bars
            val barWidth = size.width * 0.28f
            drawRect(
                color = color,
                topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                size = Size(barWidth, size.height * 0.7f)
            )
            drawRect(
                color = color,
                topLeft = Offset(size.width * 0.57f, size.height * 0.15f),
                size = Size(barWidth, size.height * 0.7f)
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.18f)
                lineTo(size.width * 0.32f, size.height * 0.82f)
                lineTo(size.width * 0.82f, size.height * 0.5f)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
private fun SkipGlyph(flipped: Boolean, color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val cx = size.width / 2f
        val triW = size.width * 0.3f
        val path = Path().apply {
            if (flipped) {
                moveTo(cx + triW, size.height * 0.12f)
                lineTo(cx + triW, size.height * 0.88f)
                lineTo(cx - triW, size.height * 0.5f)
                close()
            } else {
                moveTo(cx - triW, size.height * 0.12f)
                lineTo(cx - triW, size.height * 0.88f)
                lineTo(cx + triW, size.height * 0.5f)
                close()
            }
        }
        drawPath(path, color)
    }
}

@Preview(name = "Glance Widget Previews - All Sizes", showBackground = true)
@Composable
fun GlanceWidgetPreviewsScreen() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.rhythmwidgetpreview_rhythm_glance_widget_previews),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 1x1 Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_11_compact_widget),
                    size = "110 × 110 dp"
                ) {
                    WidgetMockup(110, 110, "OneByOne")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 1x2 Narrow Vertical Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_12_vertical_widget),
                    size = "110 × 220 dp"
                ) {
                    WidgetMockup(110, 220, "Gabe")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 2x1 Horizontal Strip Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_21_horizontal_strip),
                    size = "250 × 80 dp"
                ) {
                    WidgetMockup(250, 80, "Thin")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 2x2 Small Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_22_small_widget),
                    size = "180 × 180 dp"
                ) {
                    WidgetMockup(180, 180, "Small")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 3x2 Medium Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_32_medium_widget),
                    size = "250 × 150 dp"
                ) {
                    WidgetMockup(250, 150, "Medium")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 4x2 Large Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_42_large_widget),
                    size = "300 × 180 dp"
                ) {
                    WidgetMockup(300, 180, "Large")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 3x3 Extra Large Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_33_extra_large_widget),
                    size = "300 × 220 dp"
                ) {
                    WidgetMockup(300, 220, "ExtraLarge")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 4x3 Extra Large Plus Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_43_extra_large_plus),
                    size = "350 × 260 dp"
                ) {
                    WidgetMockup(350, 260, "ExtraLargePlus")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 5x4 Huge Widget
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_str_54_huge_widget),
                    size = "400 × 300 dp"
                ) {
                    WidgetMockup(400, 300, "Huge")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // ── Lyrics Widget Previews ──
                Text(
                    text = stringResource(R.string.rhythmwidgetpreview_lyrics_widget),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Lyrics 3×2
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_lyrics_3x2),
                    size = "250 × 150 dp"
                ) {
                    LyricsWidgetMockup(250, 150, "LyricsWidget3x2")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Lyrics 4×3
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_lyrics_4x3),
                    size = "350 × 220 dp"
                ) {
                    LyricsWidgetMockup(350, 220, "LyricsWidget4x3")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Spacer(modifier = Modifier.height(32.dp))
                
                // ── Rhythm Cookie Widget Previews ──
                Text(
                    text = stringResource(R.string.rhythmwidgetpreview_cookie_widget),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Cookie 2×2
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_cookie_2x2),
                    size = "180 × 180 dp"
                ) {
                    CookieWidgetMockup(180, 180, isPlaying = true)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Cookie resized wide
                WidgetPreviewCard(
                    title = stringResource(R.string.rhythmwidgetpreview_cookie_resized_wide),
                    size = "180 × 90 dp"
                ) {
                    CookieWidgetMockup(180, 90, isPlaying = false)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Spacer(modifier = Modifier.height(32.dp))
                
                // Widget States Section
                Text(
                    text = stringResource(R.string.rhythmwidgetpreview_widget_states),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.rhythmwidgetpreview_playing), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        WidgetMockup(180, 180, "Small")
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.rhythmwidgetpreview_paused), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        WidgetMockup(180, 180, "Small")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Design Notes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.rhythmwidgetpreview_design_features),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        DesignFeatureItem("🎨 Material 3 Expressive Design")
                        DesignFeatureItem("📐 SizeMode.Exact for pixel-perfect layouts")
                        DesignFeatureItem("🎵 Dynamic play/pause button corners")
                        DesignFeatureItem("🖼️ Album art with LruCache optimization")
                        DesignFeatureItem("🎯 Adaptive layouts for all sizes")
                        DesignFeatureItem("✨ Rounded corners and expressive spacing")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DesignFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(name = "Small Widget Detail", showBackground = true)
@Composable
fun SmallWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(180, 180, "Small")
            }
        }
    }
}

@Preview(name = "Medium Widget Detail", showBackground = true)
@Composable
fun MediumWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(250, 150, "Medium")
            }
        }
    }
}

@Preview(name = "Large Widget Detail", showBackground = true)
@Composable
fun LargeWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WidgetMockup(300, 220, "ExtraLarge")
            }
        }
    }
}

@Preview(name = "Lyrics 3x2 Widget Detail", showBackground = true)
@Composable
fun LyricsWidget3x2DetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LyricsWidgetMockup(250, 150, "LyricsWidget3x2")
            }
        }
    }
}

@Preview(name = "Cookie Widget Detail", showBackground = true)
@Composable
fun CookieWidgetDetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CookieWidgetMockup(180, 180, isPlaying = true)
            }
        }
    }
}

@Preview(name = "Lyrics 4x3 Widget Detail", showBackground = true)
@Composable
fun LyricsWidget4x3DetailPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LyricsWidgetMockup(350, 220, "LyricsWidget4x3")
            }
        }
    }
}