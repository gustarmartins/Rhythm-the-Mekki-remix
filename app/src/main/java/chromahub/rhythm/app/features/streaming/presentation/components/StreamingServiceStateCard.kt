/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.presentation.components

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveCard
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.theme.ExpressiveMaterialShape
import chromahub.rhythm.app.shared.presentation.theme.rememberExpressiveShape
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Shared streaming (Go-mode) service-state card.
 *
 * Mirrors the local `ModernEmptyState` visual language: cookie-shaped icon chip
 * in `primaryContainer`, squircle card, horizontal row layout with bold title
 * + subtitle, and an optional action button. Used by the merged Home/Library
 * screens to show disconnected / syncing / error states.
 */
@Composable
fun StreamingServiceStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: MaterialSymbolIcon = MaterialSymbolIcon("cloud_off", filled = true),
    iconContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onErrorContainer,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    showProgressIndicator: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val cookieShape = rememberExpressiveShape(ExpressiveMaterialShape.COOKIE_12)

    ExpressiveCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = ExpressiveShapes.SquircleLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = cookieShape,
                color = iconContainerColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (actionText != null && onAction != null) {
                ExpressiveFilledButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onAction()
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (showProgressIndicator) {
            androidx.compose.material3.LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 20.dp),
                trackColor = androidx.compose.ui.graphics.Color.Transparent
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
