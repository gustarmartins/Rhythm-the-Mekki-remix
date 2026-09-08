/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Worker to update Glance widgets
 * This ensures widgets stay synchronized with playback state
 */
class RhythmWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            // Get the current widget state from SharedPreferences
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            
            val songId = prefs.getString(RhythmMusicWidget.KEY_SONG_ID, "") ?: ""
            val songTitle = prefs.getString(RhythmMusicWidget.KEY_SONG_TITLE, "Rhythm") ?: "Rhythm"
            val artistName = prefs.getString(RhythmMusicWidget.KEY_ARTIST_NAME, "") ?: ""
            val albumName = prefs.getString(RhythmMusicWidget.KEY_ALBUM_NAME, "") ?: ""
            val isPlaying = prefs.getBoolean(RhythmMusicWidget.KEY_IS_PLAYING, false)
            val artworkUri = prefs.getString(RhythmMusicWidget.KEY_ARTWORK_URI, null)
            val hasPrevious = prefs.getBoolean(RhythmMusicWidget.KEY_HAS_PREVIOUS, false)
            val hasNext = prefs.getBoolean(RhythmMusicWidget.KEY_HAS_NEXT, false)
            val isFavorite = prefs.getBoolean(RhythmMusicWidget.KEY_IS_FAVORITE, false)
            
            // Update all widget instances
            val glanceWidgetManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceWidgetManager.getGlanceIds(RhythmMusicWidget::class.java)
            
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    if (songId.isNotEmpty()) {
                        prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID)] = songId
                    } else {
                        prefs.remove(stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID))
                    }
                    prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] = songTitle
                    prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] = artistName
                    prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] = albumName
                    prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_PLAYING)] = isPlaying
                    prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_PREVIOUS)] = hasPrevious
                    prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_NEXT)] = hasNext
                    prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_FAVORITE)] = isFavorite
                    if (artworkUri != null) {
                        prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI)] = artworkUri
                    }
                }
            }
            
            // Trigger widget updates
            RhythmMusicWidget().updateAll(context)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RhythmWidgetWorker", "Error updating widget", e)
            Result.failure()
        }
    }
}
