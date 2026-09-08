/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.R
import androidx.core.net.toUri

/**
 * Rhythm Cookie widget — minimalist 2x2 music controls.
 *
 * - The album art fills the whole 12-sided "cookie" background shape
 * - The play/pause button sits in a "sunny" badge anchored to the TOP-RIGHT corner
 * - Previous/Next sit in circular badges anchored to the BOTTOM corners
 * - All elements scale proportionally with the widget size (SizeMode.Exact)
 */
class RhythmCookieWidget : GlanceAppWidget() {

    companion object {
        // Dedicated high-resolution album-art cache for the full-cookie background.
        // The shared RhythmMusicWidget cache stores small (~150px) thumbnails that
        // look blurry when upscaled to fill the whole widget.
        private const val GRID_ART_CACHE_MAX_BYTES = 16 * 1024 * 1024
        private val gridArtCache = object : LruCache<String, Bitmap>(GRID_ART_CACHE_MAX_BYTES) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        }

        /** Released on app onTrimMemory so background bitmap memory is freed. */
        fun clearArtCache() {
            gridArtCache.evictAll()
        }
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appSettings = try {
            chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context)
        } catch (e: Exception) {
            null
        }

        provideContent {
            val currentPrefs = currentState<Preferences>()
            val artworkUriString = currentPrefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI)]

            var bitmap by remember(artworkUriString) {
                mutableStateOf<Bitmap?>(null)
            }

            val glanceContext = LocalContext.current
            LaunchedEffect(artworkUriString) {
                if (artworkUriString.isNullOrBlank()) {
                    bitmap = null
                    return@LaunchedEffect
                }
                // Prefer the dedicated high-resolution cache; only fall back to loading.
                val cached = gridArtCache.get(artworkUriString)
                if (cached != null) {
                    bitmap = cached
                    return@LaunchedEffect
                }
                val loaded = withContext(Dispatchers.IO) {
                    try {                            val imageLoader = coil.Coil.imageLoader(glanceContext)
                        val request = ImageRequest.Builder(glanceContext)
                            .data(artworkUriString)
                            .size(Size(1024, 1024))
                            .build()
                        val result = imageLoader.execute(request)
                        (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    } catch (e: Exception) {
                        android.util.Log.e("RhythmCookieWidget", "Error fetching bitmap", e)
                        null
                    }
                }
                if (loaded != null) {
                    gridArtCache.put(artworkUriString, loaded)
                    bitmap = loaded
                }
            }

            val widgetData = getWidgetData(currentPrefs, appSettings).copy(preloadedBitmap = bitmap)
            GlanceTheme {
                GridWidgetContent(widgetData)
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun GridWidgetContent(data: WidgetData) {
        val glanceContext = LocalContext.current
        val uiMode = glanceContext.resources.configuration.uiMode

        val cookieBitmap = remember(uiMode) {
            GlanceShapeBitmaps.create(glanceContext, 120, MaterialShapes.Cookie12Sided)
        }
        val sunnyBitmap = remember(uiMode) {
            GlanceShapeBitmaps.create(glanceContext, 80, MaterialShapes.Sunny)
        }
        val circleBitmap = remember(uiMode) {
            GlanceShapeBitmaps.create(glanceContext, 80, MaterialShapes.Circle)
        }
        val size = LocalSize.current
        val squareSize = minOf(size.width, size.height)
        val scaleFactor = squareSize.value / 100f

        // Album art clipped into the main cookie background, generated at a resolution
        // matching the widget size so it stays crisp even on large widgets.
        val cookieArtSizeDp = (120 * scaleFactor).toInt().coerceAtLeast(120)
        val cookieArtBitmap = remember(data.preloadedBitmap, uiMode, cookieArtSizeDp) {
            data.preloadedBitmap?.let {
                GlanceShapeBitmaps.create(glanceContext, cookieArtSizeDp, MaterialShapes.Cookie12Sided, sourceBitmap = it)
            }
        }

        // Small corner offsets so the badge CENTERS sit right on the cookie border:
        // play/pause pushed furthest out, corner buttons only slightly out.
        val playCornerInset = (2 * scaleFactor).dp
        val skipCornerInset = (6 * scaleFactor).dp
        val playSize = (36 * scaleFactor).dp
        val skipSize = (26 * scaleFactor).dp
        val playIconSize = (16 * scaleFactor).dp
        val skipIconSize = (12 * scaleFactor).dp
        val logoSize = (44 * scaleFactor).dp

        // Corner action config (0=skip, 1=shuffle, 2=repeat, 3=favorite, 4=none)
        val bottomLeftAction = data.cookieBottomLeft
        val bottomRightAction = data.cookieBottomRight

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(100.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.size(squareSize),
                contentAlignment = Alignment.Center
            ) {
                // Background: album art fills the whole cookie shape, or a tinted cookie placeholder
                if (cookieArtBitmap != null) {
                    Image(
                        provider = ImageProvider(cookieArtBitmap),
                        contentDescription = LocalContext.current.getString(R.string.settings_shapes_album_art),
                        modifier = GlanceModifier.fillMaxSize()
                    )
                } else {
                    Image(
                        provider = ImageProvider(cookieBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground)
                    )
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // App logo placeholder (notification icon mark)
                        Image(
                            provider = ImageProvider(R.drawable.ic_notification),
                            contentDescription = LocalContext.current.getString(R.string.rhythmcookiewidget_rhythm_logo),
                            modifier = GlanceModifier.size(logoSize),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                        )
                    }
                }

                // Play/Pause — sunny badge anchored to the top-right corner
                // (same corner-badge positioning as the stats widget's crown gem)
                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .padding(top = playCornerInset, end = playCornerInset),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(playSize)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(sunnyBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.fillMaxSize(),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                        )
                        Image(
                            provider = ImageProvider(
                                if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (data.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(playIconSize),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                        )
                    }
                }

                // Bottom-left corner action badge
                if (bottomLeftAction != 4) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize()
                            .padding(bottom = skipCornerInset, start = skipCornerInset),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        CornerActionBadge(
                            action = bottomLeftAction,
                            side = CornerSide.LEFT,
                            size = skipSize,
                            iconSize = skipIconSize,
                            circleBitmap = circleBitmap,
                            isFavorite = data.isFavorite,
                            isShuffle = data.isShuffle,
                            repeatMode = data.repeatMode
                        )
                    }
                }

                // Bottom-right corner action badge
                if (bottomRightAction != 4) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize()
                            .padding(bottom = skipCornerInset, end = skipCornerInset),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        CornerActionBadge(
                            action = bottomRightAction,
                            side = CornerSide.RIGHT,
                            size = skipSize,
                            iconSize = skipIconSize,
                            circleBitmap = circleBitmap,
                            isFavorite = data.isFavorite,
                            isShuffle = data.isShuffle,
                            repeatMode = data.repeatMode
                        )
                    }
                }
            }
        }
    }

    private fun getWidgetData(prefs: Preferences, appSettings: chromahub.rhythm.app.shared.data.model.AppSettings?): WidgetData {
        return try {
            WidgetData(
                songTitle = prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] ?: "Rhythm",
                artistName = prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] ?: "",
                albumName = prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] ?: "",
                isPlaying = prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_PLAYING)] ?: false,
                artworkUri = prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI)]?.takeIf { it.isNotBlank() }?.let {
                    try {
                        (it).toUri()
                    } catch (e: Exception) {
                        null
                    }
                },
                hasPrevious = prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_PREVIOUS)] ?: false,
                hasNext = prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_NEXT)] ?: false,
                isFavorite = prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_FAVORITE)] ?: false,
                showAlbumArt = appSettings?.widgetShowAlbumArt?.value ?: true,
                showArtist = appSettings?.widgetShowArtist?.value ?: true,
                showAlbum = appSettings?.widgetShowAlbum?.value ?: false,
                showFavoriteButton = appSettings?.widgetShowFavoriteButton?.value ?: true,
                cornerRadius = appSettings?.widgetCornerRadius?.value ?: 28,
                widgetTheme = appSettings?.widgetTheme?.value ?: 0,
                isShuffle = prefs[booleanPreferencesKey("is_shuffle")] ?: false,
                repeatMode = prefs[intPreferencesKey("repeat_mode")] ?: 0,
                cookieBottomLeft = appSettings?.widgetCookieBottomLeft?.value ?: 3,
                cookieBottomRight = appSettings?.widgetCookieBottomRight?.value ?: 4
            )
        } catch (e: Exception) {
            WidgetData(
                songTitle = "Rhythm",
                artistName = "",
                albumName = "",
                isPlaying = false,
                artworkUri = null,
                hasPrevious = false,
                hasNext = false,
                isFavorite = false,
                widgetTheme = 0
            )
        }
    }
}

/**
 * Which corner a configurable badge sits in, so skip resolves to prev vs next.
 */
private enum class CornerSide { LEFT, RIGHT }

/**
 * Renders one configurable corner badge on the cookie widget.
 * Action values: 0=skip, 1=shuffle, 2=repeat, 3=favorite, 4=none.
 */
@Composable
private fun CornerActionBadge(
    action: Int,
    side: CornerSide,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    circleBitmap: android.graphics.Bitmap,
    isFavorite: Boolean,
    isShuffle: Boolean,
    repeatMode: Int
) {
    val circleModifier = GlanceModifier.size(size)
    val iconModifier = GlanceModifier.size(iconSize)

    when (action) {
        0 -> {
            // Skip (previous on left, next on right)
            val isPrevious = side == CornerSide.LEFT
            val provider = ImageProvider(
                if (isPrevious) R.drawable.ic_skip_previous else R.drawable.ic_skip_next
            )
            Box(
                modifier = circleModifier
                    .clickable(
                        if (isPrevious) actionRunCallback<SkipPreviousAction>()
                        else actionRunCallback<SkipNextAction>()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(circleBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiary)
                )
                Image(
                    provider = provider,
                    contentDescription = if (isPrevious) "Previous" else "Next",
                    modifier = iconModifier,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiary)
                )
            }
        }
        1 -> {
            // Shuffle
            Box(
                modifier = circleModifier
                    .clickable(actionRunCallback<ToggleShuffleAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(circleBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(
                        if (isShuffle) GlanceTheme.colors.primary else GlanceTheme.colors.tertiary
                    )
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_shuffle),
                    contentDescription = LocalContext.current.getString(R.string.widget_cookie_action_shuffle),
                    modifier = iconModifier,
                    colorFilter = ColorFilter.tint(
                        if (isShuffle) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onTertiary
                    )
                )
            }
        }
        2 -> {
            // Repeat
            val repeatIcon = when (repeatMode) {
                1 -> R.drawable.ic_repeat_one
                else -> R.drawable.ic_repeat
            }
            val isRepeatActive = repeatMode != 0
            Box(
                modifier = circleModifier
                    .clickable(actionRunCallback<ToggleRepeatAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(circleBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(
                        if (isRepeatActive) GlanceTheme.colors.primary else GlanceTheme.colors.tertiary
                    )
                )
                Image(
                    provider = ImageProvider(repeatIcon),
                    contentDescription = LocalContext.current.getString(R.string.widget_cookie_action_repeat),
                    modifier = iconModifier,
                    colorFilter = ColorFilter.tint(
                        if (isRepeatActive) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onTertiary
                    )
                )
            }
        }
        3 -> {
            // Favorite
            Box(
                modifier = circleModifier
                    .clickable(actionRunCallback<ToggleFavoriteAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(circleBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(
                        if (isFavorite) GlanceTheme.colors.primary else GlanceTheme.colors.tertiary
                    )
                )
                Image(
                    provider = ImageProvider(
                        if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                    ),
                    contentDescription = LocalContext.current.getString(R.string.widget_cookie_action_favorite),
                    modifier = iconModifier,
                    colorFilter = ColorFilter.tint(
                        if (isFavorite) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onTertiary
                    )
                )
            }
        }
    }
}
