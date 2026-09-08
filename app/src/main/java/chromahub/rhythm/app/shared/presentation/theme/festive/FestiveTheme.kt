/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Calendar

/**
 * Festive theme types supported by the engine
 */
enum class FestiveThemeType {
    NONE,
    CHRISTMAS,
    NEW_YEAR,
    HALLOWEEN,
    VALENTINES,
    CUSTOM
}

/**
 * Configuration for festive decorations
 */
data class FestiveConfig(
    val type: FestiveThemeType = FestiveThemeType.NONE,
    val intensity: Float = 0.5f, // 0.0 to 1.0
    val enabled: Boolean = false,
    val autoDetect: Boolean = true, // Automatically detect holidays
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
)

/**
 * Festive Theme Engine - Central manager for festive decorations
 */
object FestiveThemeEngine {
    
    /**
     * Automatically detect current festive period based on date
     */
    fun detectFestiveTheme(): FestiveThemeType {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) // 0-11
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        return when {
            // Christmas period: December 15 - December 31
            month == Calendar.DECEMBER && day in 15..31 -> FestiveThemeType.CHRISTMAS
            
            // New Year period: December 31 - January 3
            (month == Calendar.DECEMBER && day == 31) || 
            (month == Calendar.JANUARY && day in 1..3) -> FestiveThemeType.NEW_YEAR
            
            // Halloween: October 25 - October 31
            month == Calendar.OCTOBER && day in 25..31 -> FestiveThemeType.HALLOWEEN
            
            // Valentine's Day: February 10 - February 14
            month == Calendar.FEBRUARY && day in 10..14 -> FestiveThemeType.VALENTINES
            
            else -> FestiveThemeType.NONE
        }
    }
    
    /**
     * Get active festive theme based on configuration
     */
    fun getActiveFestiveTheme(config: FestiveConfig): FestiveThemeType {
        if (!config.enabled) return FestiveThemeType.NONE
        
        return if (config.autoDetect) {
            detectFestiveTheme()
        } else {
            config.type
        }
    }
    
    /**
     * Check if custom date range is active
     */
    fun isCustomRangeActive(startDate: Long?, endDate: Long?): Boolean {
        if (startDate == null || endDate == null) return false
        
        val now = System.currentTimeMillis()
        return now in startDate..endDate
    }
}

/**
 * Remember festive configuration with state
 */
@Composable
fun rememberFestiveConfig(
    type: FestiveThemeType = FestiveThemeType.NONE,
    intensity: Float = 0.5f,
    enabled: Boolean = false,
    autoDetect: Boolean = true
): FestiveConfig {
    return remember(type, intensity, enabled, autoDetect) {
        FestiveConfig(
            type = type,
            intensity = intensity,
            enabled = enabled,
            autoDetect = autoDetect
        )
    }
}
