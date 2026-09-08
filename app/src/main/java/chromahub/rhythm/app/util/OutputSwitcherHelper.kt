/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Helper class to show the Android media output switcher
 * On Android 11+: Uses MediaRouter or broadcasts to trigger system output switcher
 * On older versions: Opens Sound Settings
 */
object OutputSwitcherHelper {
    private const val TAG = "OutputSwitcherHelper"
    
    /**
     * Show the system media output switcher
     * 
     * @param context The context to use for showing the dialog
     * @return true if the dialog was shown, false otherwise
     */
    fun showOutputSwitcher(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Try to trigger the native output switcher
            tryShowNativeOutputSwitcher(context)
        } else {
            // Fallback to settings for older versions
            showFallbackOutputSwitcher(context)
        }
    }
    
    /**
     * Try various methods to show the native output switcher on Android 11+
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun tryShowNativeOutputSwitcher(context: Context): Boolean {
        
        // Method 1: Try broadcast to open output switcher directly
        if (tryShowViaBroadcast(context)) {
            return true
        }
        
        // Method 2: Try Settings Panel API (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && tryShowViaSettingsPanel(context)) {
            return true
        }
        
        // Fallback to Sound Settings
        return showFallbackOutputSwitcher(context)
    }
    
    /**
     * Method 1: Try to broadcast intent to open output switcher (device-specific)
     */
    private fun tryShowViaBroadcast(context: Context): Boolean {
        try {
            // Some devices support this broadcast to open the output switcher directly
            val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
            intent.setPackage("com.android.systemui")
            intent.putExtra("package_name", context.packageName)
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent broadcast to open media output dialog")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast: ${e.message}")
        }
        return false
    }
    
    /**
     * Method 3: Try Settings Panel API (Android 13+)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun tryShowViaSettingsPanel(context: Context): Boolean {
        try {
            // Use the official Settings.Panel API for media output
            val panelIntent = Intent(Settings.Panel.ACTION_VOLUME)
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(panelIntent)
            Log.d(TAG, "Showing volume panel via Settings Panel API")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show Settings Panel: ${e.message}")
        }
        return false
    }
    
    /**
     * Show fallback output switcher options (Sound settings)
     * 
     * @param context The context to use for showing the dialog
     * @return true if a fallback was shown, false otherwise
     */
    private fun showFallbackOutputSwitcher(context: Context): Boolean {
        // Try various intents that might work on different devices/Android versions
        val intents = listOf(
            Intent(Settings.ACTION_SOUND_SETTINGS), // Sound settings (primary fallback)
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS), // Bluetooth settings (secondary)
            Intent("android.settings.SOUND_SETTINGS") // Legacy sound settings
        )
        
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Log.d(TAG, "Showing fallback output switcher via intent: ${intent.action}")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error showing fallback intent ${intent.action}: ${e.message}")
            }
        }
        
        Log.e(TAG, "All fallback intents failed")
        return false
    }
} 
