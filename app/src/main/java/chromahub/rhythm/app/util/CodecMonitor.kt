/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Format

/**
 * Monitor codec and format information for audio playback
 * Provides insights into codec usage and track details
 */
class CodecMonitor(private val player: ExoPlayer) {
    
    companion object {
        private const val TAG = "CodecMonitor"
    }
    
    private val listener = object : Player.Listener {
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            logTrackInfo(tracks)
        }
        
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            Log.d(TAG, "Audio session ID changed: $audioSessionId")
        }
    }
    
    /**
     * Start monitoring codec and format information
     * Useful for debugging playback issues
     */
    fun startMonitoring() {
        player.addListener(listener)
        Log.d(TAG, "Codec monitoring enabled")
        
        // Log initial track info
        logTrackInfo(player.currentTracks)
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        player.removeListener(listener)
        Log.d(TAG, "Codec monitoring disabled")
    }
    
    private fun logTrackInfo(tracks: androidx.media3.common.Tracks) {
        val audioGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
        
        audioGroups.forEach { group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                Log.d(TAG, buildString {
                    append("Audio Track: ")
                    append("codec=${format.sampleMimeType}, ")
                    append("sampleRate=${format.sampleRate}Hz, ")
                    append("channels=${format.channelCount}, ")
                    append("bitrate=${format.bitrate}, ")
                    append("encoding=${format.pcmEncoding}")
                })
            }
        }
    }
    
    /**
     * Get current audio format information
     */
    fun getCurrentAudioFormat(): Format? {
        return player.currentTracks.groups
            .firstOrNull { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.getTrackFormat(0)
    }
}
