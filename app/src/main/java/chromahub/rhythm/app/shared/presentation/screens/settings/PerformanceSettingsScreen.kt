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
    val settings = remember { AppSettings.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val manual by settings.batterySaverEnabled.collectAsState()
    val active by settings.batterySaverActive.collectAsState()
    val automatic by settings.batterySaverAutoEnabled.collectAsState()
    val threshold by settings.batterySaverThreshold.collectAsState()
    val haptics by settings.batterySaverDisableHaptics.collectAsState()
    val offload by settings.batterySaverEnableOffload.collectAsState()
    val marquee by settings.batterySaverDisableMarquee.collectAsState()
    val lossless by settings.batterySaverDisableLosslessArtwork.collectAsState()
    val artwork by settings.batterySaverDisableAutoFetchArtwork.collectAsState()
    val lyrics by settings.batterySaverDisableLyricsFetch.collectAsState()
    val library by settings.batterySaverDisableLibraryWork.collectAsState()
    val canvas by settings.batterySaverDisableCanvas.collectAsState()
    var slider by remember(threshold) { mutableStateOf(threshold.toFloat()) }

    CollapsibleHeaderScreen(
        title = stringResource(R.string.performancesettingsscreen_performance),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(if (active) R.string.battery_saver_active else R.string.battery_saver_inactive),
                    style = MaterialTheme.typography.titleLarge)
                Material3SettingsGroup(items = listOf(
                    toMaterial3SettingsItem(context = context, hapticFeedback = haptic, item = SettingItem(
                        icon = MaterialSymbolIcon("battery_saver"),
                        title = stringResource(R.string.battery_saver_manual),
                        description = stringResource(R.string.battery_saver_manual_desc),
                        toggleState = manual, onToggleChange = settings::setBatterySaverEnabled)),
                    toMaterial3SettingsItem(context = context, hapticFeedback = haptic, item = SettingItem(
                        icon = MaterialSymbolIcon("battery_alert"),
                        title = stringResource(R.string.battery_saver_auto),
                        description = stringResource(R.string.battery_saver_auto_desc),
                        toggleState = automatic, onToggleChange = settings::setBatterySaverAutoEnabled))
                ))
                if (automatic) {
                    Text(stringResource(R.string.battery_saver_threshold, slider.toInt()), modifier = Modifier.padding(top = 16.dp))
                    Slider(value = slider, onValueChange = { slider = it }, valueRange = 1f..100f,
                        steps = 98, onValueChangeFinished = { settings.setBatterySaverThreshold(slider.toInt()) })
                }
            }
            item {
                Text(stringResource(R.string.battery_saver_features), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.battery_saver_features_desc), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp))
                val options = listOf(
                    Triple(R.string.performancesettingsscreen_disable_haptics, haptics, settings::setBatterySaverDisableHaptics),
                    Triple(R.string.performancesettingsscreen_enable_audio_offload, offload, settings::setBatterySaverEnableOffload),
                    Triple(R.string.performancesettingsscreen_disable_text_marquee, marquee, settings::setBatterySaverDisableMarquee),
                    Triple(R.string.performancesettingsscreen_disable_lossless_artwork, lossless, settings::setBatterySaverDisableLosslessArtwork),
                    Triple(R.string.performancesettingsscreen_disable_auto_fetch_artwork, artwork, settings::setBatterySaverDisableAutoFetchArtwork),
                    Triple(R.string.battery_saver_pause_lyrics, lyrics, settings::setBatterySaverDisableLyricsFetch),
                    Triple(R.string.battery_saver_pause_library, library, settings::setBatterySaverDisableLibraryWork),
                    Triple(R.string.battery_saver_pause_canvas, canvas, settings::setBatterySaverDisableCanvas)
                )
                Material3SettingsGroup(items = options.map { (title, checked, change) ->
                    toMaterial3SettingsItem(context = context, hapticFeedback = haptic, item = SettingItem(
                        icon = MaterialSymbolIcon("tune"), title = stringResource(title),
                        description = if (title == R.string.performancesettingsscreen_enable_audio_offload)
                            stringResource(R.string.battery_saver_offload_desc) else "",
                        toggleState = checked, onToggleChange = change))
                })
            }
        }
    }
}
