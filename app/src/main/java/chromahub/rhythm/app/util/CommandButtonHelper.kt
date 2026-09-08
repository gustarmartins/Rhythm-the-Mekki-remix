/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.os.Bundle
import android.util.Log
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.google.common.util.concurrent.ListenableFuture

/**
 * Helper for executing CommandButton actions
 * Simplifies triggering custom button actions in controllers
 */
class CommandButtonHelper {
    companion object {
        private const val TAG = "CommandButtonHelper"
        
        /**
         * Execute a CommandButton action
         * Handles both session commands and player commands
         * 
         * @param controller MediaController instance
         * @param button CommandButton to execute
         * @param parameters Optional parameters for the action
         */
        fun executeButtonAction(
            controller: MediaController,
            button: CommandButton,
            parameters: Bundle = Bundle.EMPTY
        ): ListenableFuture<androidx.media3.session.SessionResult>? {
            return try {
                // Execute session command if available
                button.sessionCommand?.let { sessionCommand ->
                    Log.d(TAG, "Executing session command: ${sessionCommand.customAction}")
                    controller.sendCustomCommand(sessionCommand, parameters)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing button action", e)
                null
            }
        }
        
        /**
         * Create a CommandButton with automatic action execution support
         * 
         * @param displayName Button display name
         * @param iconResId Icon resource ID
         * @param commandAction Action identifier
         * @param playerCommand Optional Player.Command for built-in actions
         */
        fun createCommandButtonWithAction(
            displayName: String,
            iconResId: Int? = null,
            icon: Int? = null, // CommandButton icon constant
            commandAction: String,
            playerCommand: Int? = null
        ): CommandButton {
            val builder = if (icon != null) {
                CommandButton.Builder(icon)
            } else {
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            }
            
            builder.setDisplayName(displayName)
            
            if (iconResId != null) {
                builder.setCustomIconResId(iconResId)
            }
            
            // Set session command for custom actions
            builder.setSessionCommand(SessionCommand(commandAction, Bundle.EMPTY))
            
            // Optionally set player command for built-in actions
            if (playerCommand != null) {
                builder.setPlayerCommand(playerCommand)
            }
            
            return builder.build()
        }
        
        /**
         * Execute multiple button actions in sequence
         * Useful for macro-like operations
         */
        fun executeButtonActions(
            controller: MediaController,
            buttons: List<Pair<CommandButton, Bundle>>
        ) {
            buttons.forEach { (button, params) ->
                executeButtonAction(controller, button, params)
            }
        }
    }
}
