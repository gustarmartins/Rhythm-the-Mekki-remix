/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import chromahub.rhythm.app.shared.data.model.AppSettings

@Composable
fun AutoScrollingTextOnDemand(
    text: String,
    gradientEdgeColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    respectGlobalSetting: Boolean = true
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val isMarqueeActive by appSettings.isMarqueeActive.collectAsState()
    val effectivelyEnabled = enabled && (!respectGlobalSetting || isMarqueeActive)

    var overflow by remember(text, effectivelyEnabled) { mutableStateOf(false) }

    // Use a measurement text on first composition to detect overflow
    if (!overflow || !effectivelyEnabled) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            textAlign = textAlign,
            onTextLayout = { res: TextLayoutResult -> 
                overflow = res.hasVisualOverflow 
            },
            modifier = modifier
        )
    } else {
        AutoScrollingText(
            text = text,
            style = style,
            textAlign = textAlign,
            gradientEdgeColor = gradientEdgeColor,
            modifier = modifier
        )
    }
}

/**
 * Auto-scrolling text with gradient fade on edges.
 * Only applies marquee effect when text overflows the available space.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    gradientEdgeColor: Color,
    gradientWidth: Dp = 24.dp,
    initialDelayMillis: Int = 1500,
    velocity: Dp = 25.dp
) {
    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val textPlaceable = subcompose("text") {
            Text(text = text, style = style, maxLines = 1)
        }[0].measure(constraints.copy(maxWidth = Int.MAX_VALUE))

        val isOverflowing = textPlaceable.width > constraints.maxWidth

        val content = @Composable {
            if (isOverflowing) {
                val fadeAnimationDuration = 500

                var isScrolling by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    isScrolling = false // Ensure initial state
                    delay(initialDelayMillis.toLong())
                    isScrolling = true
                }

                val animatedLeftGradientStartColor by animateColorAsState(
                    targetValue = if (isScrolling) Color.Transparent else gradientEdgeColor,
                    animationSpec = tween(durationMillis = fadeAnimationDuration),
                    label = "LeftGradientStartColor"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val gradientWidthPx = gradientWidth.toPx()

                            // Left fade-in: Animates its color from opaque to transparent
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(animatedLeftGradientStartColor, gradientEdgeColor),
                                    startX = 0f,
                                    endX = gradientWidthPx
                                ),
                                blendMode = BlendMode.DstIn
                            )
                            // Right fade-out: Always visible for overflow
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(gradientEdgeColor, Color.Transparent),
                                    startX = size.width - gradientWidthPx,
                                    endX = size.width
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Text(
                        text = text,
                        style = style,
                        textAlign = textAlign,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            spacing = MarqueeSpacing(gradientWidth + 6.dp),
                            velocity = velocity,
                            initialDelayMillis = initialDelayMillis
                        )
                    )
                }
            } else {
                Text(
                    text = text,
                    style = style,
                    textAlign = textAlign,
                    maxLines = 1,
                )
            }
        }

        val contentPlaceable = subcompose("content", content)[0].measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

/**
 * A simple marquee text component that automatically scrolls text when it overflows.
 * This is a convenience wrapper around AutoScrollingTextOnDemand with sensible defaults.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    gradientEdgeColor: Color = Color.Transparent,
    enabled: Boolean = true
) {
    AutoScrollingTextOnDemand(
        text = text,
        style = style,
        gradientEdgeColor = gradientEdgeColor,
        modifier = modifier,
        enabled = enabled
    )
}

fun Modifier.rhythmMarquee(
    iterations: Int = Int.MAX_VALUE,
    spacing: MarqueeSpacing = MarqueeSpacing(30.dp),
    velocity: Dp = 30.dp,
    initialDelayMillis: Int = 1200
): Modifier = composed {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val isMarqueeActive by appSettings.isMarqueeActive.collectAsState()
    if (isMarqueeActive) {
        this.basicMarquee(
            iterations = iterations,
            spacing = spacing,
            velocity = velocity,
            initialDelayMillis = initialDelayMillis
        )
    } else {
        this
    }
}
