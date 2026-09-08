/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import chromahub.rhythm.app.R
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.data.repository.StatsTimeRange

/**
 * Rhythm Stats widget.
 *
 * Polished to read like Rhythm's own stats screen:
 * - 12-sided "cookie" background shape tinted with the widget background color
 * - Central "sunny" shape (primary) holding a music note + TOTAL LISTENING TIME hero value
 * - Bottom-right circular "gem" badge (tertiary) with a crown + longest streak (in days)
 * - All elements scale proportionally with the widget size (SizeMode.Exact)
 */
class RhythmStatsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appSettings = try {
            chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context)
        } catch (e: Exception) {
            null
        }

        // Time range for the hero listening time (0=all time, 1=today, 2=week, 3=month)
        val range = when (appSettings?.widgetStatsRange?.value ?: 0) {
            1 -> StatsTimeRange.TODAY
            2 -> StatsTimeRange.WEEK
            3 -> StatsTimeRange.MONTH
            else -> StatsTimeRange.ALL_TIME
        }
        // Gem content (0=longest streak, 1=current streak, 2=active days, 3=total sessions)
        val gemContent = appSettings?.widgetStatsGem?.value ?: 0

        val statsRepository = PlaybackStatsRepository.getInstance(context)
        val summary = try {
            statsRepository.loadSummary(range)
        } catch (e: Exception) {
            null
        }

        val totalDurationMs = summary?.totalDurationMs ?: 0L
        val gemValue = when (gemContent) {
            1 -> summary?.currentStreakDays ?: 0
            2 -> summary?.activeDays ?: 0
            3 -> summary?.totalSessions ?: 0
            else -> summary?.longestStreakDays ?: 0
        }

        provideContent {
            val uiMode = context.resources.configuration.uiMode
            val sunnyBitmap = remember(uiMode) { GlanceShapeBitmaps.create(context, 80, MaterialShapes.Sunny) }
            val backgroundBitmap = remember(uiMode) { GlanceShapeBitmaps.create(context, 120, MaterialShapes.Cookie12Sided) }
            val circleBitmap = remember(uiMode) { GlanceShapeBitmaps.create(context, 80, MaterialShapes.Circle) }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .cornerRadius(100.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    StatsWidgetContent(
                        totalDurationMs = totalDurationMs,
                        gemValue = gemValue,
                        gemContent = gemContent,
                        sunnyBitmap = sunnyBitmap,
                        backgroundBitmap = backgroundBitmap,
                        circleBitmap = circleBitmap
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SuppressLint("RestrictedApi")
    @Composable
    private fun StatsWidgetContent(
        totalDurationMs: Long,
        gemValue: Int,
        gemContent: Int,
        sunnyBitmap: android.graphics.Bitmap,
        backgroundBitmap: android.graphics.Bitmap,
        circleBitmap: android.graphics.Bitmap
    ) {
        val size = LocalSize.current
        val squareSize = minOf(size.width, size.height)
        val scaleFactor = squareSize.value / 100f

        val contentPadding = (7 * scaleFactor).dp
        val containerSize = (34 * scaleFactor).dp
        val noteSize = (15 * scaleFactor).dp

        val durationStr = formatDurationCompact(totalDurationMs)
        val mainFontSize = when {
            durationStr.length >= 8 -> (13 * scaleFactor).sp
            durationStr.length == 7 -> (15 * scaleFactor).sp
            durationStr.length == 6 -> (17 * scaleFactor).sp
            durationStr.length == 5 -> (20 * scaleFactor).sp
            else -> (22 * scaleFactor).sp
        }
        val unitFontSize = (9 * scaleFactor).sp

        val bestGemSize = (28 * scaleFactor).dp
        val bestGemStr = gemValue.toString()
        val bestPillFontSize = when {
            bestGemStr.length >= 4 -> (7 * scaleFactor).sp
            bestGemStr.length == 3 -> (8 * scaleFactor).sp
            else -> (10 * scaleFactor).sp
        }
        val bestIconSize = (10 * scaleFactor).dp
        // Icon shown in the gem badge (crown for streaks, stats icon for days/sessions)
        val gemIconRes = if (gemContent == 1) R.drawable.ic_fire else R.drawable.ic_crown

        val backgroundColor = GlanceTheme.colors.widgetBackground

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.size(squareSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(backgroundBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(backgroundColor)
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(GlanceModifier.defaultWeight())

                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                provider = ImageProvider(sunnyBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.size(containerSize),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            )
                            Image(
                                provider = ImageProvider(R.drawable.ic_stats),
                                contentDescription = null,
                                modifier = GlanceModifier.size(noteSize),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
                            )
                        }

                        Spacer(GlanceModifier.height((4 * scaleFactor).dp))

                        Text(
                            text = durationStr,
                            style = TextStyle(
                                fontSize = mainFontSize,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary
                            )
                        )

                        Spacer(GlanceModifier.height((1 * scaleFactor).dp))

                        // "listening" unit label - like the stats screen's total listening time
                        Text(
                            text = LocalContext.current.getString(R.string.rhythmstatswidget_listening),
                            style = TextStyle(
                                fontSize = unitFontSize,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.primaryContainer
                            )
                        )

                        Spacer(GlanceModifier.defaultWeight())
                    }
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .padding(bottom = contentPadding, end = (2 * scaleFactor).dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(circleBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(bestGemSize),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiary)
                        )
                        Column(
                            modifier = GlanceModifier.size(bestGemSize).padding(top = (2 * scaleFactor).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                provider = ImageProvider(gemIconRes),
                                contentDescription = null,
                                modifier = GlanceModifier.size(bestIconSize),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiary)
                            )
                            Text(
                                text = gemValue.toString(),
                                modifier = GlanceModifier.padding(top = (-2 * scaleFactor).dp),
                                style = TextStyle(
                                    fontSize = bestPillFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.onTertiary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Compact duration formatting for the widget, e.g. "12h 30m", "3h", "45m", "<1m".
     */
    private fun formatDurationCompact(ms: Long): String {
        val seconds = ms / 1000
        val totalMinutes = seconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "<1m"
        }
    }
}
