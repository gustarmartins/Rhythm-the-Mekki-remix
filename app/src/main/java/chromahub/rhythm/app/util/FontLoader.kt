/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.utils

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream

object FontLoader {
    private const val FONTS_DIR = "custom_fonts"
    private const val FONT_FILE_NAME = "custom_font.ttf"
    
    /**
     * Copies the selected font file to internal storage
     * @param context Application context
     * @param uri URI of the selected font file
     * @return Path to the saved font file, or null if failed
     */
    fun copyFontToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            // Create fonts directory if it doesn't exist
            val fontsDir = File(context.filesDir, FONTS_DIR)
            if (!fontsDir.exists()) {
                fontsDir.mkdirs()
            }
            
            // Create destination file
            val destinationFile = File(fontsDir, FONT_FILE_NAME)
            
            // Copy the font file
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Return the absolute path
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Loads a custom font from the given file path
     * @param context Application context
     * @param fontPath Absolute path to the font file
     * @return FontFamily if successful, null if failed
     */
    fun loadCustomFont(context: Context, fontPath: String?): FontFamily? {
        if (fontPath.isNullOrBlank()) return null
        
        return try {
            val fontFile = File(fontPath)
            if (!fontFile.exists()) {
                return null
            }
            
            // Read the font file into a temporary file that Compose can access
            // We need to copy it to cache dir with a resource-like name
            val cacheFile = File(context.cacheDir, "custom_font_${fontFile.name}")
            if (!cacheFile.exists() || fontFile.lastModified() > cacheFile.lastModified()) {
                fontFile.copyTo(cacheFile, overwrite = true)
            }
            
            // Load font using the file descriptor approach
            // Note: This is a workaround since Compose Font requires resource IDs
            // We'll use the androidx.compose.ui.text.font.Font constructor that accepts File
            FontFamily(
                Font(fontFile)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Deletes the custom font from internal storage
     * @param context Application context
     * @return true if deleted successfully
     */
    fun deleteCustomFont(context: Context): Boolean {
        return try {
            val fontsDir = File(context.filesDir, FONTS_DIR)
            val fontFile = File(fontsDir, FONT_FILE_NAME)
            if (fontFile.exists()) {
                fontFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Gets the file name of the custom font (without path)
     * @param fontPath Absolute path to the font file
     * @return File name or null
     */
    fun getFontFileName(fontPath: String?): String? {
        if (fontPath.isNullOrBlank()) return null
        return try {
            File(fontPath).name
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validates if a font file exists and is readable
     * @param fontPath Absolute path to the font file
     * @return true if valid
     */
    fun validateFontFile(fontPath: String?): Boolean {
        if (fontPath.isNullOrBlank()) return false
        return try {
            val file = File(fontPath)
            file.exists() && file.canRead() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}
