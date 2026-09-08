/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.player

import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SheetAdaptiveType
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader

import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel.SleepAction
import androidx.compose.material3.LinearWavyProgressIndicator
import chromahub.rhythm.app.shared.presentation.components.common.RhythmGroupedButton
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonWeighted
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SleepTimerOption(
    val minutes: Int,
    val label: String,
    val icon: MaterialSymbolIcon
)

private enum class SheetContentState { Presets, Active, InlinePicker }

@Composable
fun SleepTimerBottomSheetNew(
    onDismiss: () -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val isTimerActive by musicViewModel.sleepTimerActive.collectAsState()
    val remainingSeconds by musicViewModel.sleepTimerRemainingSeconds.collectAsState()
    val totalTimerSeconds by musicViewModel.sleepTimerTotalSeconds.collectAsState()
    val timerAction by musicViewModel.sleepTimerAction.collectAsState()
    val serviceConnected by musicViewModel.serviceConnected.collectAsState()

    var selectedAction by remember { mutableStateOf(SleepAction.valueOf(timerAction.takeIf { it.isNotBlank() } ?: "FADE_OUT")) }
    var statusMessage by remember { mutableStateOf("") }
    var sheetState by remember { mutableStateOf(SheetContentState.Presets) }
    // Track session original total — only updated when a brand new timer session starts (not on +/- adjustments)
    var originalTotalSeconds by remember { mutableLongStateOf(0L) }

    val timerOptions = listOf(
        SleepTimerOption(5, "5 min", MaterialSymbolIcon("coffee", filled = true)),
        SleepTimerOption(15, "15 min", MaterialSymbolIcon("local_cafe", filled = true)),
        SleepTimerOption(30, "30 min", MaterialSymbolIcon("wb_twilight", filled = true)),
        SleepTimerOption(45, "45 min", MaterialSymbolIcon("bedtime", filled = true)),
        SleepTimerOption(60, "1 hour", MaterialSymbolIcon("nightlight_round", filled = true)),
        SleepTimerOption(90, "1.5 hr", RhythmIcons.DarkMode)
    )

    val actionOptions = listOf(
        Triple(SleepAction.FADE_OUT, "Fade Out", RhythmIcons.VolumeDown),
        Triple(SleepAction.PAUSE, "Pause", RhythmIcons.Pause),
        Triple(SleepAction.STOP, "Stop", RhythmIcons.Stop)
    )

    fun startTimer(minutes: Int) {
        if (!isPlaying || currentSong == null) {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
            return
        }
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        // Fresh start — reset session baseline
        originalTotalSeconds = (minutes * 60).toLong()
        coroutineScope.launch {
            statusMessage = "Timer Started"
            delay(1500)
            if (statusMessage == "Timer Started") statusMessage = ""
        }
        musicViewModel.startSleepTimer(minutes, selectedAction)
    }

    fun stopTimer() {
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        musicViewModel.stopSleepTimer()
    }

    fun adjustTime(deltaMinutes: Int) {
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        val newRemainingSeconds = remainingSeconds + (deltaMinutes * 60)
        
        if (newRemainingSeconds <= 0L) {
            stopTimer()
        } else {
            val newMinutes = (newRemainingSeconds / 60) + if (newRemainingSeconds % 60 > 0) 1 else 0
            val msg = if (deltaMinutes > 0) "+$deltaMinutes Minutes" else "$deltaMinutes Minutes"
            coroutineScope.launch {
                statusMessage = msg
                delay(1500)
                if (statusMessage == msg) statusMessage = ""
            }
            // Adjust originalTotalSeconds to keep elapsed progress consistent
            // elapsed = originalTotalSeconds - remainingSeconds (unchanged), new total = elapsed + newRemainingSeconds
            val elapsed = originalTotalSeconds - remainingSeconds
            originalTotalSeconds = elapsed + newRemainingSeconds
            musicViewModel.startSleepTimer(newMinutes.toInt(), selectedAction)
        }
    }

    fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }

    // Intercept state changes to play the 'Stopped' animation before swapping content
    LaunchedEffect(isTimerActive) {
        if (isTimerActive && sheetState != SheetContentState.InlinePicker) {
            sheetState = SheetContentState.Active
        } else if (!isTimerActive && sheetState == SheetContentState.Active) {
            statusMessage = "Timer Stopped"
            delay(1500)
            sheetState = SheetContentState.Presets
            statusMessage = ""
        }
    }

    val bottomSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))

    RhythmAdaptiveModalSheet(
        adaptiveType = SheetAdaptiveType.WIDE_DIALOG,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    ) {
        val isErrorState = !isPlaying || !serviceConnected || currentSong == null

        StandardBottomSheetHeader(
            title = context.getString(R.string.sleep_timer),
            subtitle = when {
                isTimerActive -> "Active"
                isErrorState -> "No music playing"
                else -> "Set automatic playback control"
            },
            visible = true
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ── Scrollable Content Area ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = sheetState,
                    transitionSpec = {
                        val floatSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                        val offsetSpring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)

                        (fadeIn(animationSpec = floatSpring) + slideInVertically(
                            animationSpec = offsetSpring,
                            initialOffsetY = { if (targetState == SheetContentState.Active) -it / 6 else it / 6 }
                        )).togetherWith(
                            fadeOut(animationSpec = floatSpring) + slideOutVertically(
                                animationSpec = offsetSpring,
                                targetOffsetY = { if (targetState == SheetContentState.Active) it / 6 else -it / 6 }
                            )
                        )
                    },
                    label = "sleep_timer_content"
                ) { state ->
                    when (state) {
                        // ── Active Timer ───────────────────────────────────────
                        SheetContentState.Active -> {
                            val activeScrollState = rememberScrollState()
                            AdaptiveSheetScrollContainer(
                                scrollState = activeScrollState,
                                blendColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxSize()
                            ) { endPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(activeScrollState)
                                        .padding(end = endPadding),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Timer Status Card — accent (primaryContainer) background
                                        Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(208.dp)
                                        ) {
                                            AnimatedContent(
                                                targetState = statusMessage.isNotEmpty(),
                                                transitionSpec = {
                                                    val floatSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                                                    (fadeIn(animationSpec = floatSpring) + scaleIn(animationSpec = floatSpring, initialScale = 0.92f)) togetherWith
                                                    (fadeOut(animationSpec = floatSpring) + scaleOut(animationSpec = floatSpring, targetScale = 0.92f))
                                                },
                                                label = "timer_status_anim"
                                            ) { isShowingMessage ->
                                                if (isShowingMessage) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                                        contentAlignment = Alignment.BottomEnd
                                                    ) {
                                                        Text(
                                                            text = statusMessage,
                                                            style = MaterialTheme.typography.headlineLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 20.dp, vertical = 20.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        BoxWithConstraints(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .weight(1f),
                                                            contentAlignment = Alignment.BottomEnd
                                                        ) {
                                                            val timerText = formatTime(remainingSeconds)
                                                            // Auto-scale the countdown so long hour+ timers (e.g. "1:23:45") never clip on narrow screens
                                                            val baseSizeSp = MaterialTheme.typography.displayLarge.fontSize.value
                                                            val baseSizeDp = baseSizeSp * LocalDensity.current.fontScale
                                                            val digitCount = timerText.count { it.isDigit() }
                                                            val colonCount = timerText.count { it == ':' }
                                                            val estimatedWidthDp = (digitCount * 0.6f + colonCount * 0.38f) * baseSizeDp
                                                            val scale = if (estimatedWidthDp > 0f) (maxWidth.value / estimatedWidthDp).coerceAtMost(1f) else 1f
                                                            val fittedSizeSp = (baseSizeSp * scale).coerceAtLeast(32f)
                                                            Text(
                                                                text = timerText,
                                                                style = MaterialTheme.typography.displayLarge.copy(fontSize = fittedSizeSp.sp),
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                textAlign = TextAlign.End,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        val rawProgress = if (totalTimerSeconds > 0L) {
                                                            ((totalTimerSeconds - remainingSeconds).toFloat() / totalTimerSeconds).coerceIn(0f, 1f)
                                                        } else 0f
                                                        val animatedProgress by animateFloatAsState(
                                                            targetValue = rawProgress,
                                                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f),
                                                            label = "SleepTimerProgress"
                                                        )
                                                        LinearWavyProgressIndicator(
                                                            progress = { animatedProgress },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(10.dp),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Quick Adjust Controls Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RhythmGroupedButton(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            size = RhythmButtonSize.Large
                                        ) {
                                            RhythmButtonWeighted(
                                                onClick = { adjustTime(-5) },
                                                weight = 1f,
                                                isFirst = true,
                                                isLast = false,
                                                icon = MaterialSymbolIcon("remove", filled = true),
                                                text = "5 min",
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                            RhythmButtonWeighted(
                                                onClick = { adjustTime(5) },
                                                weight = 1f,
                                                isFirst = false,
                                                isLast = true,
                                                icon = MaterialSymbolIcon("add", filled = true),
                                                text = "5 min"
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    ActionSelectionCard(
                                        selectedAction = selectedAction,
                                        actionOptions = actionOptions,
                                        onActionSelected = { action ->
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                            selectedAction = action
                                            musicViewModel.appSettings.setSleepTimerAction(action.name)
                                        },
                                        context = context
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    // ── Inline Time Picker ─────────────────────────────────
                    SheetContentState.InlinePicker -> {
                        InlineTimePickerContent(
                            onCancel = {
                                sheetState = if (isTimerActive) SheetContentState.Active else SheetContentState.Presets
                            },
                            onTimeSelected = { hours, minutes ->
                                val totalMinutes = hours * 60 + minutes
                                if (totalMinutes > 0) {
                                    startTimer(totalMinutes)
                                }
                                sheetState = SheetContentState.Active
                            }
                        )
                    }

                    // ── Presets ────────────────────────────────────────────
                    SheetContentState.Presets -> {
                        val presetsScrollState = rememberScrollState()
                        AdaptiveSheetScrollContainer(
                            scrollState = presetsScrollState,
                            blendColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxSize()
                        ) { endPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(presetsScrollState)
                                    .padding(end = endPadding),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Quick Presets
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = MaterialSymbolIcon("timer", filled = true),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = context.getString(R.string.sleep_timer_quick),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                items(timerOptions, key = { "timer_${it.minutes}" }) { option ->
                                                    val isTimerAvailable = isPlaying && serviceConnected && currentSong != null
                                                    Card(
                                                        onClick = { if (isTimerAvailable) startTimer(option.minutes) },
                                                        modifier = Modifier.size(width = 80.dp, height = 84.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isTimerAvailable) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize().padding(10.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = option.icon,
                                                                contentDescription = null,
                                                                tint = if (isTimerAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = option.label,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (isTimerAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Custom Timer
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.AccessTime,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = context.getString(R.string.sleep_timer_custom),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = context.getString(R.string.bottomsheet_timer_custom_desc),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            val isCustomTimerAvailable = isPlaying && serviceConnected && currentSong != null
                                            FilledTonalButton(
                                                onClick = {
                                                    if (isCustomTimerAvailable) sheetState = SheetContentState.InlinePicker
                                                },
                                                enabled = isCustomTimerAvailable,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.Edit,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    context.getString(R.string.bottomsheet_timer_custom_title),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    ActionSelectionCard(
                                        selectedAction = selectedAction,
                                        actionOptions = actionOptions,
                                        onActionSelected = { action ->
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                            selectedAction = action
                                            musicViewModel.appSettings.setSleepTimerAction(action.name)
                                        },
                                        context = context
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            }

            // ── Fixed Footer ────────────────────────────────────────────────
            if (sheetState == SheetContentState.Active) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    RhythmGroupedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        size = RhythmButtonSize.Large
                    ) {
                        RhythmButtonWeighted(
                            onClick = { stopTimer() },
                            weight = 1f,
                            isFirst = true,
                            icon = RhythmIcons.Stop,
                            text = context.getString(R.string.bottomsheet_cancel),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        RhythmButtonWeighted(
                            onClick = { sheetState = SheetContentState.InlinePicker },
                            weight = 1f,
                            isLast = true,
                            icon = RhythmIcons.Edit,
                            text = context.getString(R.string.bottomsheet_timer_edit)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSelectionCard(
    selectedAction: SleepAction,
    actionOptions: List<Triple<SleepAction, String, MaterialSymbolIcon>>,
    onActionSelected: (SleepAction) -> Unit,
    context: android.content.Context
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = RhythmIcons.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.sleep_timer_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = context.getString(R.string.sleep_timer_action_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actionOptions.forEach { (action, label, icon) ->
                    val isSelected = selectedAction == action
                    Card(
                        onClick = { onActionSelected(action) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineTimePickerContent(
    onCancel: () -> Unit,
    onTimeSelected: (hours: Int, minutes: Int) -> Unit
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 30,
        is24Hour = true
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TimePicker(
                modifier = Modifier.padding(horizontal = 24.dp),
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    clockDialContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    periodSelectorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    periodSelectorContentColor = MaterialTheme.colorScheme.onSurface,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    timeSelectorContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            RhythmGroupedButton(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                size = RhythmButtonSize.Large
            ) {
                RhythmButtonWeighted(
                    onClick = onCancel,
                    weight = 1f,
                    isFirst = true,
                    icon = RhythmIcons.Close,
                    text = context.getString(R.string.bottomsheet_cancel),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                RhythmButtonWeighted(
                    onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) },
                    weight = 1f,
                    isLast = true,
                    icon = RhythmIcons.Check,
                    text = context.getString(R.string.bottomsheet_timer_set)
                )
            }
        }
    }
}