/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import chromahub.rhythm.app.shared.data.model.AppSettings

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.getInstance(application)
    
    // Theme state - initialize from AppSettings
    private val _useSystemTheme = MutableStateFlow(appSettings.useSystemTheme.value)
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()
    
    private val _darkMode = MutableStateFlow(appSettings.darkMode.value)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()
    
    // Dynamic color state (for Monet theme)
    private val _useDynamicColors = MutableStateFlow(appSettings.useDynamicColors.value)
    val useDynamicColors: StateFlow<Boolean> = _useDynamicColors.asStateFlow()
    
    init {
        // Observe AppSettings changes
        viewModelScope.launch {
            appSettings.useSystemTheme.collect { useSystem ->
                _useSystemTheme.value = useSystem
            }
        }
        
        viewModelScope.launch {
            appSettings.darkMode.collect { isDark ->
                _darkMode.value = isDark
            }
        }
        
        viewModelScope.launch {
            appSettings.useDynamicColors.collect { useDynamic ->
                _useDynamicColors.value = useDynamic
            }
        }
    }
    
    // Function to update system theme usage
    fun setUseSystemTheme(useSystem: Boolean) {
        appSettings.setUseSystemTheme(useSystem)
        _useSystemTheme.value = useSystem
    }
    
    // Function to update dark mode
    fun setDarkMode(isDark: Boolean) {
        appSettings.setDarkMode(isDark)
        _darkMode.value = isDark
    }
    
    // Function to update dynamic colors
    fun setUseDynamicColors(useDynamic: Boolean) {
        appSettings.setUseDynamicColors(useDynamic)
        _useDynamicColors.value = useDynamic
    }
} 
