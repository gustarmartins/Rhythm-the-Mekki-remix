/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import androidx.compose.foundation.lazy.rememberLazyListState
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SheetAdaptiveType

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.common.RhythmCardGroup
import chromahub.rhythm.app.shared.presentation.components.common.RhythmGroupedButton
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonWeighted
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonSize
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonType

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import androidx.compose.ui.res.stringResource
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PlaybackSpeedAndPitchBottomSheet(
    currentSpeed: Float,
    currentPitch: Float,
    syncEnabled: Boolean = false,
    onSyncChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (speed: Float, pitch: Float) -> Unit,
    onSetDefaultSpeed: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    val minVal = 0.25f
    val maxVal = 3.0f

    var selectedSpeed by remember { mutableFloatStateOf(currentSpeed.coerceIn(minVal, maxVal)) }
    var selectedPitch by remember { mutableFloatStateOf(currentPitch.coerceIn(minVal, maxVal)) }

    fun formatClean(value: Float): String {
        val formatted = String.format(Locale.US, "%.3f", value)
        return formatted.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
    }

    fun formatValueWithX(value: Float): String = "${formatClean(value)}x"

    fun adjustSpeed(delta: Float) {
        val newSpeed = (selectedSpeed + delta).coerceIn(minVal, maxVal)
        val rounded = (Math.round(newSpeed * 1000.0) / 1000.0).toFloat()
        selectedSpeed = rounded
        if (syncEnabled) selectedPitch = rounded
    }

    fun adjustPitch(delta: Float) {
        val newPitch = (selectedPitch + delta).coerceIn(minVal, maxVal)
        val rounded = (Math.round(newPitch * 1000.0) / 1000.0).toFloat()
        selectedPitch = rounded
        if (syncEnabled) selectedSpeed = rounded
    }

    fun dismissAndSave(speed: Float, pitch: Float) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onSave(speed, pitch)
            onDismiss()
        }
    }

    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)
    val pitchPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)

    val soloShape = RoundedCornerShape(24.dp)
    val topShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val bottomShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    RhythmAdaptiveModalSheet(
        adaptiveType = SheetAdaptiveType.WIDE_DIALOG,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) {
        val speedListState = rememberLazyListState()

        Column(modifier = Modifier.fillMaxWidth()) {
            StandardBottomSheetHeader(
                title = stringResource(R.string.player_speed_and_pitch),
                visible = true
            )

            AdaptiveSheetScrollContainer(
                lazyListState = speedListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) { endPadding ->
                LazyColumn(
                    state = speedListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp + endPadding, top = 8.dp, bottom = 16.dp)
                ) {
                    item {
                        val hasDefault = onSetDefaultSpeed != null
                    RhythmCardGroup(
                        shape = if (hasDefault) topShape else soloShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                    onSyncChange(!syncEnabled)
                                    if (!syncEnabled) selectedPitch = selectedSpeed
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = MaterialSymbolIcon("sync_alt", filled = true),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.player_sync_speed_pitch),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.mirror_changes_across_both),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TunerAnimatedSwitch(
                                checked = syncEnabled,
                                onCheckedChange = { enabled ->
                                    onSyncChange(enabled)
                                    if (enabled) selectedPitch = selectedSpeed
                                }
                            )
                        }
                    }

                    if (hasDefault) {
                        Spacer(modifier = Modifier.height(4.dp))
                        RhythmCardGroup(
                            shape = bottomShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        onSetDefaultSpeed(selectedSpeed)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = MaterialSymbolIcon("star", filled = true),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.set_as_default_speed),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.current_speed_value, formatValueWithX(selectedSpeed)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // Speed section heading + card
                item {
                    SectionHeadingRow(
                        icon = MaterialSymbolIcon("speed", filled = true),
                        tint = MaterialTheme.colorScheme.primary,
                        label = context.getString(R.string.player_speed_label),
                        value = formatValueWithX(selectedSpeed)
                    )
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }

                item {
                    RhythmCardGroup(shape = soloShape, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RhythmGroupedButton(
                                    modifier = Modifier.width(80.dp),
                                    isFillMaxWidth = false,
                                    size = RhythmButtonSize.Medium
                                ) {
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustSpeed(-0.05f)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f, isFirst = true, isLast = false,
                                        type = RhythmButtonType.Tonal,
                                        icon = MaterialSymbolIcon("remove", filled = true)
                                    )
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustSpeed(+0.05f)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f, isFirst = false, isLast = true,
                                        type = RhythmButtonType.Tonal,
                                        icon = MaterialSymbolIcon("add", filled = true)
                                    )
                                }
                                Slider(
                                    value = selectedSpeed,
                                    onValueChange = { v ->
                                        val r = (Math.round(v * 1000.0) / 1000.0).toFloat()
                                        selectedSpeed = r
                                        if (syncEnabled) selectedPitch = r
                                    },
                                    valueRange = minVal..maxVal,
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    onValueChangeFinished = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                    }
                                )
                            }

                            RhythmGroupedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                size = RhythmButtonSize.Small
                            ) {
                                listOf(-0.01f, -0.001f, +0.001f, +0.01f).forEachIndexed { idx, step ->
                                    val label = if (step > 0) "+${formatClean(step)}" else formatClean(step)
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustSpeed(step)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f,
                                        isFirst = idx == 0,
                                        isLast = idx == 3,
                                        type = RhythmButtonType.Tonal,
                                        text = label
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(speedPresets) { preset ->
                                    val isSelected = selectedSpeed == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSpeed = preset
                                            if (syncEnabled) selectedPitch = preset
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        },
                                        label = {
                                            Text(
                                                text = formatValueWithX(preset),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // Pitch section heading + card
                item {
                    SectionHeadingRow(
                        icon = MaterialSymbolIcon("graphic_eq", filled = true),
                        tint = MaterialTheme.colorScheme.secondary,
                        label = context.getString(R.string.player_pitch_label),
                        value = formatValueWithX(selectedPitch)
                    )
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }

                item {
                    RhythmCardGroup(shape = soloShape, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RhythmGroupedButton(
                                    modifier = Modifier.width(80.dp),
                                    isFillMaxWidth = false,
                                    size = RhythmButtonSize.Medium
                                ) {
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustPitch(-0.05f)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f, isFirst = true, isLast = false,
                                        type = RhythmButtonType.Tonal,
                                        icon = MaterialSymbolIcon("remove", filled = true),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustPitch(+0.05f)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f, isFirst = false, isLast = true,
                                        type = RhythmButtonType.Tonal,
                                        icon = MaterialSymbolIcon("add", filled = true),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Slider(
                                    value = selectedPitch,
                                    onValueChange = { v ->
                                        val r = (Math.round(v * 1000.0) / 1000.0).toFloat()
                                        selectedPitch = r
                                        if (syncEnabled) selectedSpeed = r
                                    },
                                    valueRange = minVal..maxVal,
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.secondary,
                                        activeTrackColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    onValueChangeFinished = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                    }
                                )
                            }

                            RhythmGroupedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                size = RhythmButtonSize.Small
                            ) {
                                listOf(-0.01f, -0.001f, +0.001f, +0.01f).forEachIndexed { idx, step ->
                                    val label = if (step > 0) "+${formatClean(step)}" else formatClean(step)
                                    RhythmButtonWeighted(
                                        onClick = {
                                            adjustPitch(step)
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        },
                                        weight = 1f,
                                        isFirst = idx == 0,
                                        isLast = idx == 3,
                                        type = RhythmButtonType.Tonal,
                                        text = label,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(pitchPresets) { preset ->
                                    val isSelected = selectedPitch == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPitch = preset
                                            if (syncEnabled) selectedSpeed = preset
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        },
                                        label = {
                                            Text(
                                                text = formatValueWithX(preset),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

            // Footer buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                RhythmGroupedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    size = RhythmButtonSize.Large
                ) {
                    RhythmButtonWeighted(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            selectedSpeed = 1.0f
                            selectedPitch = 1.0f
                        },
                        weight = 1f,
                        isFirst = true,
                        icon = MaterialSymbolIcon("restart_alt"),
                        text = context.getString(R.string.bottomsheet_reset)
                    )
                    RhythmButtonWeighted(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            dismissAndSave(selectedSpeed, selectedPitch)
                        },
                        weight = 1f,
                        isLast = true,
                        icon = RhythmIcons.Check,
                        text = context.getString(R.string.ui_apply)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeadingRow(
    icon: MaterialSymbolIcon,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
