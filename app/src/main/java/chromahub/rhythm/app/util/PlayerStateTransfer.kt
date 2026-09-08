/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.MediaItem

/**
 * Helper for transferring playback state between players
 * Useful for casting, multi-device playback, or switching audio outputs
 * Manual implementation since PlayerTransferState requires newer API
 */
class PlayerStateTransfer {
    
    data class SavedPlayerState(
        val mediaItems: List<MediaItem>,
        val currentMediaItemIndex: Int,
        val currentPosition: Long,
        val playWhenReady: Boolean,
        val playbackSpeed: Float,
        val playbackPitch: Float,
        val shuffleModeEnabled: Boolean,
        val repeatMode: Int
    )
    
    companion object {
        private const val TAG = "PlayerStateTransfer"
        
        /**
         * Save the current player state for transfer
         * @param player The player to save state from
         * @return SavedPlayerState containing all necessary state info
         */
        fun savePlayerState(player: Player): SavedPlayerState {
            Log.d(TAG, "Saving player state")
            
            val mediaItems = mutableListOf<MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                mediaItems.add(player.getMediaItemAt(i))
            }
            
            return SavedPlayerState(
                mediaItems = mediaItems,
                currentMediaItemIndex = player.currentMediaItemIndex,
                currentPosition = player.currentPosition,
                playWhenReady = player.playWhenReady,
                playbackSpeed = player.playbackParameters.speed,
                playbackPitch = player.playbackParameters.pitch,
                shuffleModeEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode
            )
        }
        
        /**
         * Restore player state from a saved transfer state
         * @param player The player to restore state to
         * @param savedState The saved state
         */
        fun restorePlayerState(player: Player, savedState: SavedPlayerState) {
            Log.d(TAG, "Restoring player state")
            
            // Set media items
            player.setMediaItems(savedState.mediaItems, savedState.currentMediaItemIndex, savedState.currentPosition)
            
            // Restore playback settings
            player.shuffleModeEnabled = savedState.shuffleModeEnabled
            player.repeatMode = savedState.repeatMode
            player.playbackParameters = PlaybackParameters(savedState.playbackSpeed, savedState.playbackPitch)
            player.playWhenReady = savedState.playWhenReady
            
            // Prepare the player
            player.prepare()
        }
        
        /**
         * Transfer playback from one player to another seamlessly
         * Example: Switching from local to Cast player
         * @param fromPlayer Source player
         * @param toPlayer Destination player
         */
        fun transferPlayback(fromPlayer: Player, toPlayer: Player) {
            Log.d(TAG, "Transferring playback from ${fromPlayer.javaClass.simpleName} to ${toPlayer.javaClass.simpleName}")
            
            try {
                // Save state from source player
                val savedState = savePlayerState(fromPlayer)
                
                // Pause source player
                fromPlayer.pause()
                
                // Restore state to destination player
                restorePlayerState(toPlayer, savedState)
                
                Log.d(TAG, "Playback transfer completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error transferring playback", e)
            }
        }
    }
}
