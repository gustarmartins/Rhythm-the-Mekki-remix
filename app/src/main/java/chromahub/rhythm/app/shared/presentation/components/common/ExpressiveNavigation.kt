/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// EXPRESSIVE NAVIGATION BAR
// ============================================================================

/**
 * Expressive Navigation Bar with pill-shaped indicators and bouncy animations
 * Designed for bottom navigation with 3-5 items
 */
@Composable
fun ExpressiveNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Expressive Navigation Bar Item with pill indicator and bouncy animation
 */
@Composable
fun RowScope.ExpressiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    selectedIcon: @Composable () -> Unit = icon,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            selected -> 1f
            else -> 0.95f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_item_scale"
    )
    
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 64.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator_width"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "icon_color"
    )
    
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "label_color"
    )
    
    Column(
        modifier = modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill indicator with icon
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(if (selected) 64.dp else 48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated pill background
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                )
            }
            
            // Icon
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    selectedIcon()
                } else {
                    icon()
                }
            }
        }
        
        // Label
        if (label != null && (alwaysShowLabel || selected)) {
            Spacer(Modifier.height(4.dp))
            Box {
                label()
            }
        }
    }
}

// ============================================================================
// EXPRESSIVE NAVIGATION RAIL
// ============================================================================

/**
 * Expressive Navigation Rail for tablets and large screens
 */
@Composable
fun ExpressiveNavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(windowInsets)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (header != null) {
                header()
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

/**
 * Expressive Navigation Rail Item
 */
@Composable
fun ExpressiveNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    selectedIcon: @Composable () -> Unit = icon,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            selected -> 1f
            else -> 0.95f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rail_item_scale"
    )
    
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pill indicator with icon
        Box(
            modifier = Modifier.height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated pill background
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                )
            }
            
            // Icon
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    selectedIcon()
                } else {
                    icon()
                }
            }
        }
        
        // Label
        if (label != null && (alwaysShowLabel || selected)) {
            Spacer(Modifier.height(4.dp))
            label()
        }
    }
}

// ============================================================================
// FLOATING NAVIGATION BAR (Expressive Style)
// ============================================================================

/**
 * Floating Navigation Bar - a pill-shaped floating navigation component
 * Great for immersive content like music players
 */
@Composable
fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = ExpressiveShapes.Full,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Floating Navigation Bar Item
 */
@Composable
fun FloatingNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    selectedIcon: @Composable () -> Unit = icon,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            selected -> 1f
            else -> 0.92f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_nav_item_scale"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "container_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "content_color"
    )
    
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        enabled = enabled,
        shape = ExpressiveShapes.Full,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected && label != null) 16.dp else 12.dp,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                if (selected) {
                    selectedIcon()
                } else {
                    icon()
                }
            }
            
            AnimatedVisibility(
                visible = selected && label != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================================
// NAVIGATION DATA CLASSES
// ============================================================================

/**
 * Data class for navigation item
 */
data class ExpressiveNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val contentDescription: String = label
)

/**
 * Helper composable to create navigation items from a list
 */
@Composable
fun RowScope.ExpressiveNavigationBarItems(
    items: List<ExpressiveNavItem>,
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    items.forEach { item ->
        ExpressiveNavigationBarItem(
            selected = selectedRoute == item.route,
            onClick = { onNavigate(item.route) },
            icon = {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription
                )
            },
            selectedIcon = {
                Icon(
                    imageVector = item.selectedIcon,
                    contentDescription = item.contentDescription
                )
            },
            label = {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedRoute == item.route) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}
