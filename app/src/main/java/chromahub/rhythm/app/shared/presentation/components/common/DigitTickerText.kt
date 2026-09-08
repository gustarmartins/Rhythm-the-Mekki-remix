/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle

/**
 * Renders [text] character-by-character with an odometer-style rollover.
 *
 * When a character changes to another digit it rolls vertically (up when the
 * value increases, down when it decreases); unchanged characters stay put and
 * non-digit changes simply fade. Mirrors the Rhythm Guard dashboard digit
 * ticker so short numeric counters animate only the digits that actually change.
 */
@Composable
fun AnimatedDigitTickerText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    prefix: String = ""
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState.isDigit() && initialState.isDigit()) {
                        if (targetState > initialState) {
                            (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                        } else {
                            (slideInVertically { -it / 2 } + fadeIn()).togetherWith(slideOutVertically { it / 2 } + fadeOut())
                        }
                    } else {
                        fadeIn().togetherWith(fadeOut())
                    }
                },
                label = "DigitTicker_${prefix}_$index",
                contentAlignment = Alignment.BottomStart
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Bottom,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    fontWeight = fontWeight,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}
