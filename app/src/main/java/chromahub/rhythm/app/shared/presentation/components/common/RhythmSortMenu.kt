/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShape
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.R

/**
 * Data class representing a sorting option in the custom sort menu.
 */
data class RhythmSortOption(
    val key: String,
    val label: String,
    val icon: MaterialSymbolIcon? = null
)

@Composable
fun RhythmSortMenuContent(
    selectedKey: String,
    isAscending: Boolean,
    options: List<RhythmSortOption>,
    onKeySelected: (String) -> Unit,
    onDirectionToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val directionCardShape = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 30.dp,
        bottomStart = 30.dp,
        bottomEnd = 30.dp
    )
    val arrowContainerShape = rememberExpressiveShape("COOKIE_7", CircleShape)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Order Direction Toggle Card
        Surface(
            onClick = { onDirectionToggled(!isAscending) },
            shape = directionCardShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = arrowContainerShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAscending) RhythmIcons.ArrowUpward else RhythmIcons.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.sort_order_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        text = if (isAscending) context.getString(R.string.sort_order_ascending) else context.getString(R.string.sort_order_descending),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // 2. Sorting Option Cards grouped into a card stack
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedKey == option.key
                val itemShape = when {
                    options.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 6.dp, bottomEnd = 6.dp
                    )
                    index == options.size - 1 -> RoundedCornerShape(
                        topStart = 6.dp, topEnd = 6.dp,
                        bottomStart = 24.dp, bottomEnd = 24.dp
                    )
                    else -> RoundedCornerShape(6.dp)
                }

                Surface(
                    onClick = { onKeySelected(option.key) },
                    shape = itemShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    border = if (!isSelected) {
                        BorderStroke(0.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                        
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        RadioButton(
                            selected = isSelected,
                            onClick = null, // handled by row surface click
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}
