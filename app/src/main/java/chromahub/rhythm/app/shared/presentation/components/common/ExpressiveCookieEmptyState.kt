/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons

/**
 * Shared cookie-cluster empty state.
 *
 * Displays a floating cluster of three expressive "cookie"-shaped icon chips (a large
 * main chip flanked by two smaller decorative chips) above a bold headline and an
 * optional supporting line. This is the canonical empty-state visual used by the
 * search screens (no-results), the queue / "add songs" bottom sheet, and Rhythm Stats,
 * so every empty state stays pixel-identical.
 *
 * The outer [modifier] is applied to the wrapping column, so callers can control
 * padding, alignment and animation without affecting the inner layout.
 */
@Composable
fun ExpressiveCookieEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    mainIcon: MaterialSymbolIcon = RhythmIcons.MusicNote,
    accentIcon: MaterialSymbolIcon = mainIcon,
    cornerIcon: MaterialSymbolIcon = mainIcon,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val cookieShape = rememberExpressiveShape("COOKIE_12")
    val smallCookieShape = rememberExpressiveShape("COOKIE_6")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = smallCookieShape,
                color = containerColor.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = 10.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = accentIcon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(
                shape = smallCookieShape,
                color = containerColor.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = (-6).dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = cornerIcon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Surface(
                shape = cookieShape,
                color = containerColor,
                shadowElevation = 6.dp,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = mainIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
