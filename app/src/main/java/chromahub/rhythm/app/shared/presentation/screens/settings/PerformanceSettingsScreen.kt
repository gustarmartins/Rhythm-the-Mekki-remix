/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings



import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import androidx.compose.foundation.layout.PaddingValues
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import chromahub.rhythm.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.data.repository.StatsTimeRange
import chromahub.rhythm.app.util.GsonUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveScrollBar
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.UpdateBottomSheet
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeProvider
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.common.buildSplashBackdropShapes
import chromahub.rhythm.app.shared.presentation.components.common.SplashBackgroundOrbs
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppVersion
import chromahub.rhythm.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import chromahub.rhythm.app.utils.FontLoader
import chromahub.rhythm.app.ui.theme.parseCustomColorScheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import chromahub.rhythm.app.shared.presentation.components.common.M3FourColorCircularLoader
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.BulkPlaylistExportDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistImportDialog
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShape
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationProgressDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationResultDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.AppRestartDialog
import chromahub.rhythm.app.shared.presentation.components.player.PlayerChipOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem

import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingRow
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingCard
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingItem
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingGroup


@Composable
fun PerformanceSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // Collect settings states
    val batterySaverEnabled by appSettings.batterySaverEnabled.collectAsState()
    val batterySaverMode by appSettings.batterySaverMode.collectAsState()
    
    val batterySaverDisableHaptics by appSettings.batterySaverDisableHaptics.collectAsState()
    val batterySaverEnableOffload by appSettings.batterySaverEnableOffload.collectAsState()
    val batterySaverDisableMarquee by appSettings.batterySaverDisableMarquee.collectAsState()
    val batterySaverDisableLosslessArtwork by appSettings.batterySaverDisableLosslessArtwork.collectAsState()
    val batterySaverDisableAutoFetchArtwork by appSettings.batterySaverDisableAutoFetchArtwork.collectAsState()

    CollapsibleHeaderScreen(
        title = stringResource(R.string.performancesettingsscreen_performance),
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (batterySaverEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("speed"),
                            contentDescription = null,
                            tint = if (batterySaverEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(35.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(if (batterySaverEnabled) R.string.status_active else R.string.status_disabled),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (batterySaverEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TunerAnimatedSwitch(
                            checked = batterySaverEnabled,
                            onCheckedChange = { enabled ->
                                appSettings.setBatterySaverEnabled(enabled)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = batterySaverEnabled,
                        enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            ExpressiveButtonGroup(
                                items = listOf("Auto", "Manual"),
                                selectedIndex = if (batterySaverMode == "auto") 0 else 1,
                                onItemClick = { index ->
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    appSettings.setBatterySaverMode(if (index == 0) "auto" else "manual")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    ) { modifier ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "options_section") {
                AnimatedVisibility(
                    visible = batterySaverEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (batterySaverMode == "auto") "Auto Overrides (Enforced)" else "Manual Overrides",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        
                        if (batterySaverMode == "auto") {
                            Material3SettingsGroup(
                                items = listOf(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("touch_app"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_haptics),
                                            description = context.getString(R.string.performancesettingsscreen_haptics_detail),
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("bolt"),
                                            title = stringResource(R.string.performancesettingsscreen_enable_audio_offload),
                                            description = context.getString(R.string.performancesettingsscreen_offload_detail),
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("slideshow"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_text_marquee),
                                            description = context.getString(R.string.performancesettingsscreen_marquee_detail),
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = RhythmIcons.Image,
                                            title = stringResource(R.string.performancesettingsscreen_disable_lossless_artwork),
                                            description = context.getString(R.string.performancesettingsscreen_lossless_detail),
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("cloud_off"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_auto_fetch_artwork),
                                            description = context.getString(R.string.performancesettingsscreen_autofetch_detail),
                                            toggleState = true,
                                            onToggleChange = {},
                                            enabled = false
                                        )
                                    )
                                ),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        } else {
                            // Manual mode: switch controls
                            Material3SettingsGroup(
                                items = listOf(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("touch_app"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_haptics),
                                            description = context.getString(R.string.performancesettingsscreen_disable_haptics_desc),
                                            toggleState = batterySaverDisableHaptics,
                                            onToggleChange = { appSettings.setBatterySaverDisableHaptics(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("bolt"),
                                            title = stringResource(R.string.performancesettingsscreen_enable_audio_offload),
                                            description = context.getString(R.string.performancesettingsscreen_enable_audio_offload_desc),
                                            toggleState = batterySaverEnableOffload,
                                            onToggleChange = { appSettings.setBatterySaverEnableOffload(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("slideshow"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_text_marquee),
                                            description = context.getString(R.string.performancesettingsscreen_disable_text_marquee_desc),
                                            toggleState = batterySaverDisableMarquee,
                                            onToggleChange = { appSettings.setBatterySaverDisableMarquee(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = RhythmIcons.Image,
                                            title = stringResource(R.string.performancesettingsscreen_disable_lossless_artwork),
                                            description = context.getString(R.string.performancesettingsscreen_disable_lossless_artwork_desc),
                                            toggleState = batterySaverDisableLosslessArtwork,
                                            onToggleChange = { appSettings.setBatterySaverDisableLosslessArtwork(it) }
                                        )
                                    ),
                                    toMaterial3SettingsItem(
                                        context = context,
                                        hapticFeedback = haptic,
                                        item = SettingItem(
                                            icon = MaterialSymbolIcon("cloud_off"),
                                            title = stringResource(R.string.performancesettingsscreen_disable_auto_fetch_artwork),
                                            description = context.getString(R.string.performancesettingsscreen_disable_auto_fetch_artwork_desc),
                                            toggleState = batterySaverDisableAutoFetchArtwork,
                                            onToggleChange = { appSettings.setBatterySaverDisableAutoFetchArtwork(it) }
                                        )
                                    )
                                ),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    }
                }
            }
            
            // Padding item
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}