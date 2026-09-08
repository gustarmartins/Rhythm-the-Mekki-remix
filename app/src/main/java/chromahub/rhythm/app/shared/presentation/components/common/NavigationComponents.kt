/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource
import chromahub.rhythm.app.util.windowScreenWidthDp
import chromahub.rhythm.app.util.windowScreenHeightDp

/**
 * Alphabet bar for quick navigation through sorted lists
 * 
 * @param letters List of letters to display (e.g., A-Z, or artist initials)
 * @param selectedLetter Currently selected/highlighted letter
 * @param onLetterSelected Callback when a letter is tapped or dragged to
 * @param modifier Modifier for the alphabet bar
 * @param haptics HapticFeedback for tactile response
 */
@Composable
fun AlphabetBar(
    letters: List<String>,
    selectedLetter: String?,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    haptics: HapticFeedback? = null
) {
    val context = LocalContext.current
    var draggedLetter by remember { mutableStateOf<String?>(null) }
    val activeLetter = draggedLetter ?: selectedLetter
    
    // Responsive sizing based on screen width
    val screenWidthDp = windowScreenWidthDp()
    val isCompactWidth = screenWidthDp < 400
    val isTablet = screenWidthDp >= 600
    
    val barWidth = when {
        isCompactWidth -> 36.dp
        isTablet -> 48.dp
        else -> 40.dp
    }
    val itemSize = when {
        isCompactWidth -> 28.dp
        isTablet -> 36.dp
        else -> 32.dp
    }
    val fontSize = when {
        isCompactWidth -> 9.sp
        isTablet -> 12.sp
        else -> 11.sp
    }
    
    Surface(
        modifier = modifier
            .width(barWidth)
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(barWidth / 2),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = (offset.y / size.height * letters.size).toInt()
                                .coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            draggedLetter = letter
                            onLetterSelected(letter)
                            haptics?.let { HapticUtils.performHapticFeedback(context, it, HapticType.LIGHT) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val index = (change.position.y / size.height * letters.size).toInt()
                                .coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (letter != draggedLetter) {
                                draggedLetter = letter
                                onLetterSelected(letter)
                                haptics?.let { HapticUtils.performHapticFeedback(context, it, HapticType.LIGHT) }
                            }
                        },
                        onDragEnd = {
                            draggedLetter = null
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            letters.forEach { letter ->
                AlphabetBarItem(
                    letter = letter,
                    isSelected = letter == activeLetter,
                    itemSize = itemSize,
                    fontSize = fontSize,
                    onClick = {
                        onLetterSelected(letter)
                        haptics?.let { HapticUtils.performHapticFeedback(context, it, HapticType.HEAVY) }
                    }
                )
            }
        }
    }
}

/**
 * Individual letter item in the alphabet bar
 */
@Composable
private fun AlphabetBarItem(
    letter: String,
    isSelected: Boolean,
    itemSize: Dp = 32.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "letter_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "letter_alpha"
    )
    
    Box(
        modifier = Modifier
            .size(itemSize)
            .scale(scale)
            .alpha(alpha)
            .pointerInput(letter) {
                detectDragGestures(
                    onDragStart = { onClick() },
                    onDrag = { _, _ -> }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Background circle for selected letter
        if (isSelected) {
            Surface(
                modifier = Modifier.size(itemSize * 0.85f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {}
        }
        
        Text(
            text = letter,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Scroll to top floating action button
 * 
 * @param visible Whether the button should be shown
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the FAB
 * @param haptics HapticFeedback for tactile response
 */
@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: HapticFeedback? = null
) {
    val context = LocalContext.current
    // Responsive sizing based on screen size
    val screenWidthDp = windowScreenWidthDp()
    val isCompactWidth = screenWidthDp < 400
    val isTablet = screenWidthDp >= 600
    
    val fabSize = when {
        isCompactWidth -> 48.dp
        isTablet -> 60.dp
        else -> 56.dp
    }
    val iconSize = when {
        isCompactWidth -> 20.dp
        isTablet -> 28.dp
        else -> 24.dp
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        modifier = modifier
    ) {
        val elevation by animateDpAsState(
            targetValue = if (visible) 6.dp else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "fab_elevation"
        )
        
        FloatingActionButton(
            onClick = {
                haptics?.let { HapticUtils.performHapticFeedback(context, it, HapticType.HEAVY) }
                onClick()
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(fabSize)
        ) {
            Icon(
                imageVector = RhythmIcons.ArrowUpward,
                contentDescription = stringResource(R.string.navigationcomponents_scroll_to_top),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
