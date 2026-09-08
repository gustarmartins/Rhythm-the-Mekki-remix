/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import chromahub.rhythm.app.R
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.infrastructure.widget.glance.RhythmMusicWidget

class RhythmTileService : TileService() {

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == RhythmMusicWidget.KEY_IS_PLAYING || key == RhythmMusicWidget.KEY_SONG_TITLE) {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        updateTileState()
    }

    override fun onStopListening() {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onStopListening()
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val isPlaying = prefs.getBoolean(RhythmMusicWidget.KEY_IS_PLAYING, false)
        val title = prefs.getString(RhythmMusicWidget.KEY_SONG_TITLE, "").orEmpty()
        
        val hasActiveSong = title.isNotBlank() && !title.equals(getString(R.string.app_name), ignoreCase = true)
        
        if (hasActiveSong) {
            val intent = Intent(this, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_PLAY_PAUSE
            }
            try {
                ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                openApp()
            }
            
            // Toggle state immediately for responsiveness
            qsTile?.let { tile ->
                val nextState = if (isPlaying) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                tile.state = nextState
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (isPlaying) "Paused: $title" else "Playing: $title"
                } else {
                    tile.label = if (isPlaying) "Paused: $title" else "Playing: $title"
                }
                val nextIconRes = if (isPlaying) chromahub.rhythm.app.R.drawable.ic_play_shortcut else chromahub.rhythm.app.R.drawable.ic_pause_shortcut
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, nextIconRes)
                tile.updateTile()
            }
        } else {
            openApp()
        }
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            } catch (e2: Exception) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val isPlaying = prefs.getBoolean(RhythmMusicWidget.KEY_IS_PLAYING, false)
        val title = prefs.getString(RhythmMusicWidget.KEY_SONG_TITLE, "").orEmpty()
        val hasActiveSong = title.isNotBlank() && !title.equals(getString(R.string.app_name), ignoreCase = true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = getString(R.string.app_name)
            if (hasActiveSong) {
                tile.subtitle = if (isPlaying) getString(R.string.tile_playing_format, title) else getString(R.string.tile_paused_format, title)
                tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                val iconRes = if (isPlaying) chromahub.rhythm.app.R.drawable.ic_pause_shortcut else chromahub.rhythm.app.R.drawable.ic_play_shortcut
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, iconRes)
            } else {
                tile.subtitle = getString(R.string.tile_play_music)
                tile.state = Tile.STATE_INACTIVE
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, chromahub.rhythm.app.R.drawable.ic_notification)
            }
        } else {
            if (hasActiveSong) {
                tile.label = if (isPlaying) getString(R.string.tile_playing_format, title) else getString(R.string.tile_paused_format, title)
                tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                val iconRes = if (isPlaying) chromahub.rhythm.app.R.drawable.ic_pause_shortcut else chromahub.rhythm.app.R.drawable.ic_play_shortcut
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, iconRes)
            } else {
                tile.label = getString(R.string.app_name)
                tile.state = Tile.STATE_INACTIVE
                tile.icon = android.graphics.drawable.Icon.createWithResource(this, chromahub.rhythm.app.R.drawable.ic_notification)
            }
        }
        tile.updateTile()
    }
}
