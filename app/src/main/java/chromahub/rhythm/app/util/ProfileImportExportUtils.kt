/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import chromahub.rhythm.app.shared.data.model.UserAudioDevice
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProfileImportExportUtils {
    private val gson = Gson()
    
    /**
     * Export user devices to a JSON file
     */
    suspend fun exportProfiles(
        context: Context,
        devices: List<UserAudioDevice>,
        launcher: ActivityResultLauncher<Intent>
    ) {
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "rhythm_audio_devices_$timestamp.json"
            
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            
            withContext(Dispatchers.Main) {
                launcher.launch(intent)
            }
        }
    }
    
    /**
     * Write devices to URI
     */
    suspend fun writeDevicesToUri(
        context: Context,
        uri: Uri,
        devices: List<UserAudioDevice>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        val json = gson.toJson(devices)
                        writer.write(json)
                        writer.flush()
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Import devices from a JSON file
     */
    suspend fun importProfiles(
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    ) {
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            launcher.launch(intent)
        }
    }
    
    /**
     * Read devices from URI
     */
    suspend fun readDevicesFromUri(
        context: Context,
        uri: Uri
    ): List<UserAudioDevice>? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val json = reader.readText()
                        val devices = gson.fromJson(json, Array<UserAudioDevice>::class.java).toList()
                        
                        // Validate devices
                        if (devices.all { it.id.isNotBlank() && it.name.isNotBlank() }) {
                            devices
                        } else {
                            null
                        }
                    }
                }
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Try to import from other music apps (Poweramp, Neutron, etc.)
     * This is a placeholder for app-specific import logic
     */
    suspend fun importFromOtherApp(
        context: Context,
        appFormat: String,
        uri: Uri
    ): List<UserAudioDevice>? {
        return withContext(Dispatchers.IO) {
            try {
                when (appFormat) {
                    "poweramp" -> importFromPoweramp(context, uri)
                    "neutron" -> importFromNeutron(context, uri)
                    "jetaudio" -> importFromJetAudio(context, uri)
                    else -> readDevicesFromUri(context, uri) // Default JSON format
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    private fun importFromPoweramp(context: Context, uri: Uri): List<UserAudioDevice>? {
        // Poweramp uses a specific preset format
        // This is a placeholder implementation
        // TODO: Implement actual Poweramp preset parsing
        return null
    }
    
    private fun importFromNeutron(context: Context, uri: Uri): List<UserAudioDevice>? {
        // Neutron uses XML or JSON format
        // This is a placeholder implementation
        // TODO: Implement actual Neutron preset parsing
        return null
    }
    
    private fun importFromJetAudio(context: Context, uri: Uri): List<UserAudioDevice>? {
        // JetAudio uses a specific format
        // This is a placeholder implementation
        // TODO: Implement actual JetAudio preset parsing
        return null
    }
    
    /**
     * Merge imported devices with existing ones, avoiding duplicates
     */
    fun mergeDevices(
        existing: List<UserAudioDevice>,
        imported: List<UserAudioDevice>,
        replaceExisting: Boolean = false
    ): List<UserAudioDevice> {
        if (replaceExisting) {
            return imported
        }
        
        val existingIds = existing.map { it.id }.toSet()
        val newDevices = imported.filter { it.id !in existingIds }
        
        return existing + newDevices
    }
}
