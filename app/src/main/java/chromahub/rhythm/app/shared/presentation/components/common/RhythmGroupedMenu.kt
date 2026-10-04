/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.util.HapticUtils

private val GroupedMenuOuterRadius = 16.dp
private val GroupedMenuInnerRadius = 4.dp
private val GroupedMenuRowSpacing = 3.dp

/**
 * A row of [RhythmGroupedMenuContent]. [icon] accepts a [MaterialSymbolIcon] or an
 * [ImageVector]; [iconTint] should be the `on*` colour paired with [iconContainerColor].
 */
data class RhythmMenuItem(
    val title: String,
    val icon: Any,
    val iconContainerColor: Color,
    val iconTint: Color,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Grouped list of overflow-menu rows, shared by the song item and header 3-dot menus.
 */
@Composable
fun RhythmGroupedMenuContent(
    items: List<RhythmMenuItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(GroupedMenuRowSpacing)
    ) {
        items.forEachIndexed { index, item ->
            Surface(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.MEDIUM)
                    item.onClick()
                },
                shape = groupedMenuRowShape(index, items.size),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = item.iconContainerColor
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val icon = item.icon
                            when (icon) {
                                is MaterialSymbolIcon -> Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = item.iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                is ImageVector -> Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = item.iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isDestructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun groupedMenuRowShape(index: Int, totalItems: Int): RoundedCornerShape = when {
    totalItems <= 1 -> RoundedCornerShape(GroupedMenuOuterRadius)
    index == 0 -> RoundedCornerShape(
        topStart = GroupedMenuOuterRadius,
        topEnd = GroupedMenuOuterRadius,
        bottomStart = GroupedMenuInnerRadius,
        bottomEnd = GroupedMenuInnerRadius
    )
    index == totalItems - 1 -> RoundedCornerShape(
        topStart = GroupedMenuInnerRadius,
        topEnd = GroupedMenuInnerRadius,
        bottomStart = GroupedMenuOuterRadius,
        bottomEnd = GroupedMenuOuterRadius
    )
    else -> RoundedCornerShape(GroupedMenuInnerRadius)
}
