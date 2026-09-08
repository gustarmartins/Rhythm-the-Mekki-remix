/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package chromahub.rhythm.app.features.local.presentation.components.settings

import chromahub.rhythm.app.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SheetAdaptiveType
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import chromahub.rhythm.app.shared.presentation.components.common.DragDropLazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.RhythmGroupedButton
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonWeighted
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonSize
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer

private fun groupedBottomSheetItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    if (totalCount <= 1) return RoundedCornerShape(24.dp)

    return when (index) {
        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }
}

@Composable
fun LibraryTabOrderBottomSheet(
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    val tabOrder by appSettings.libraryTabOrder.collectAsState()
    val hiddenTabs by appSettings.hiddenLibraryTabs.collectAsState()
    var reorderableList by remember { mutableStateOf(tabOrder.toList()) }
    var hiddenTabsSet by remember { mutableStateOf(hiddenTabs) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    val scope = rememberCoroutineScope()
    
    // Helper function to get display name and icon for tab
    fun getTabInfo(tabId: String): Pair<String, MaterialSymbolIcon> {
        return when (tabId) {
            "SONGS" -> Pair(context.getString(R.string.settings_tab_songs), RhythmIcons.HeadphonesFilled)
            "LIKED" -> Pair(context.getString(R.string.settings_tab_liked), RhythmIcons.FavoriteFilled)
            "PLAYLISTS" -> Pair(context.getString(R.string.settings_tab_playlists), RhythmIcons.PlaylistFilled)
            "ALBUMS" -> Pair(context.getString(R.string.settings_tab_albums), RhythmIcons.Music.Album)
            "ARTISTS" -> Pair(context.getString(R.string.settings_tab_artists), RhythmIcons.Artist)
            "ALBUM_ARTISTS" -> Pair(context.getString(R.string.settings_tab_album_artists), MaterialSymbolIcon("person_pin"))
            "EXPLORER" -> Pair(context.getString(R.string.settings_tab_explorer), RhythmIcons.Folder)
            else -> Pair(tabId, RhythmIcons.Music.Song)
        }
    }
    
    val lazyListState = rememberLazyListState()

    RhythmAdaptiveModalSheet(
        adaptiveType = SheetAdaptiveType.AUTO_DIALOG,
        lazyListState = lazyListState,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    ) {
        StandardBottomSheetHeader(
            title = context.getString(R.string.library_tab_order_title),
            subtitle = context.getString(R.string.library_tab_order_desc),
            visible = true
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Reorderable list using DragDropLazyColumn inside AdaptiveSheetScrollContainer
            AdaptiveSheetScrollContainer(
                lazyListState = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { endPadding ->
                DragDropLazyColumn(
                    items = reorderableList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp + endPadding),
                    lazyListState = lazyListState,
                    onMove = { fromIndex, toIndex ->
                        val newList = reorderableList.toMutableList()
                        val item = newList.removeAt(fromIndex)
                        newList.add(toIndex, item)
                        reorderableList = newList
                    },
                    itemKey = { it }
                ) { tabId, isDragging, index ->
                    val (tabName, tabIcon) = getTabInfo(tabId)
                    val totalTabs = reorderableList.size
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDragging) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = groupedBottomSheetItemShape(index, totalTabs)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Position indicator
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                // Tab icon
                                Icon(
                                    imageVector = tabIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                // Tab name
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Visibility toggle and drag handle
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isHidden = hiddenTabsSet.contains(tabId)
                                val visibleTabsCount = reorderableList.count { !hiddenTabsSet.contains(it) }
                                
                                IconButton(
                                    onClick = {
                                        // Prevent hiding the last visible tab
                                        if (!isHidden && visibleTabsCount <= 1) {
                                            Toast.makeText(context, R.string.library_tab_one_visible, Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                        hiddenTabsSet = if (isHidden) {
                                            hiddenTabsSet - tabId
                                        } else {
                                            hiddenTabsSet + tabId
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHidden) RhythmIcons.VisibilityOff else RhythmIcons.Visibility,
                                        contentDescription = if (isHidden) "Show tab" else "Hide tab",
                                        tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Drag Handle Icon
                                Icon(
                                    imageVector = RhythmIcons.DragHandle,
                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Footer at the bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                RhythmGroupedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    size = RhythmButtonSize.Large
                ) {
                    // Reset button
                    RhythmButtonWeighted(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            appSettings.resetLibraryTabOrder()
                            appSettings.setHiddenLibraryTabs(emptySet())
                            reorderableList = listOf("SONGS", "LIKED", "PLAYLISTS", "ALBUMS", "ARTISTS", "EXPLORER")
                            hiddenTabsSet = emptySet()
                            Toast.makeText(context, R.string.library_tab_order_reset, Toast.LENGTH_SHORT).show()
                        },
                        weight = 1f,
                        isFirst = true,
                        icon = MaterialSymbolIcon("restart_alt"),
                        text = context.getString(R.string.bottomsheet_reset)
                    )

                    // Save button
                    RhythmButtonWeighted(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            appSettings.setLibraryTabOrder(reorderableList)
                            appSettings.setHiddenLibraryTabs(hiddenTabsSet)
                            Toast.makeText(context, R.string.library_tab_order_saved, Toast.LENGTH_SHORT).show()
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        },
                        weight = 1f,
                        isLast = true,
                        icon = RhythmIcons.Check,
                        text = context.getString(R.string.bottomsheet_save)
                    )
                }
            }
        }
    }
}
