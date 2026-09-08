/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.common

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expressive button group component with smooth animations and pill-shaped selection.
 * Creates a row of buttons where only one can be selected at a time.
 *
 * @param items List of button labels
 * @param selectedIndex Currently selected button index
 * @param onItemClick Callback when a button is clicked
 * @param modifier Modifier for the button group container
 * @param buttonHeight Height of each button
 * @param containerColor Background color of the container
 * @param selectedButtonColor Color of the selected button
 * @param unselectedButtonColor Color of unselected buttons
 * @param selectedContentColor Text/icon color for selected button
 * @param unselectedContentColor Text/icon color for unselected buttons
 * @param showCheckIcon Whether to show check icon on selected button
 */
@Composable
fun ExpressiveButtonGroup(
    items: List<String>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 48.dp,
    containerColor: Color = Color.Transparent,
    selectedButtonColor: Color = MaterialTheme.colorScheme.primary,
    unselectedButtonColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    showCheckIcon: Boolean = true,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor, CircleShape)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            val shape = if (isSelected) CircleShape else RoundedCornerShape(10.dp)
            
            val buttonColor by animateColorAsState(
                targetValue = if (isSelected) selectedButtonColor else unselectedButtonColor,
                animationSpec = tween(durationMillis = 250),
                label = "ButtonColor"
            )
            
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                animationSpec = tween(durationMillis = 250),
                label = "ContentColor"
            )
            
            val checkIconAlpha by animateFloatAsState(
                targetValue = if (isSelected && showCheckIcon) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "CheckIconAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight)
                    .clip(shape)
                    .background(buttonColor)
                    .clickable(enabled = enabled) { onItemClick(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showCheckIcon && isSelected && checkIconAlpha > 0f) {
                        Icon(
                            imageVector = RhythmIcons.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            tint = contentColor.copy(alpha = checkIconAlpha)
                        )
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Compact variant of ExpressiveButtonGroup with smaller height and tighter spacing.
 */
@Composable
fun ExpressiveButtonGroupCompact(
    items: List<String>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ExpressiveButtonGroup(
        items = items,
        selectedIndex = selectedIndex,
        onItemClick = onItemClick,
        modifier = modifier,
        buttonHeight = 40.dp,
        showCheckIcon = false
    )
}
