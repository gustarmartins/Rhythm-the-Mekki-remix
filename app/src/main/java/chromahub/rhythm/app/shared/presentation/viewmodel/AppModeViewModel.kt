/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.core.domain.model.StreamingConfig
import chromahub.rhythm.app.core.domain.model.StreamingQuality
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the application mode (Local vs Streaming).
 * Handles mode switching and persists user preferences.
 * 
 * NOTE: Uses AppSettings (primary settings system) instead of duplicate UserPreferencesRepository
 * for consistency and to avoid sync issues between two settings systems.
 */
class AppModeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appSettings = AppSettings.getInstance(application)
    
    /**
     * Current application mode (Local or Streaming).
     */
    val currentMode: StateFlow<AppMode> = appSettings.appMode
        .map { modeString ->
            try {
                AppMode.valueOf(modeString)
            } catch (e: IllegalArgumentException) {
                AppMode.LOCAL
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppMode.LOCAL
        )
    
    /**
     * Current streaming configuration.
     */
    val streamingConfig: StateFlow<StreamingConfig> = appSettings.streamingService
        .map { service ->
            StreamingConfig(
                activeService = try {
                    SourceType.valueOf(service.uppercase())
                } catch (e: IllegalArgumentException) {
                    SourceType.SUBSONIC
                },
                isAuthenticated = true, // Assume authenticated if service is set
                streamingQuality = try {
                    StreamingQuality.valueOf(appSettings.streamingQuality.value.uppercase())
                } catch (e: IllegalArgumentException) {
                    StreamingQuality.HIGH
                },
                allowCellularStreaming = appSettings.allowCellularStreaming.value,
                offlineMode = appSettings.offlineMode.value
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StreamingConfig()
        )
    
    private val _isModeSwitching = MutableStateFlow(false)
    
    /**
     * Whether mode is currently being switched (for loading UI).
     */
    val isModeSwitching: StateFlow<Boolean> = _isModeSwitching.asStateFlow()
    
    /**
     * Switch to a new application mode.
     */
    fun switchMode(newMode: AppMode) {
        viewModelScope.launch {
            _isModeSwitching.value = true
            
            try {
                appSettings.setAppMode(newMode.name)
                
                // Add a small delay for smooth transition
                kotlinx.coroutines.delay(200)
            } finally {
                _isModeSwitching.value = false
            }
        }
    }
    
    /**
     * Toggle between Local and Streaming modes.
     */
    fun toggleMode() {
        val newMode = when (currentMode.value) {
            AppMode.LOCAL -> AppMode.STREAMING
            AppMode.STREAMING -> AppMode.LOCAL
        }
        switchMode(newMode)
    }
    
    /**
     * Set the active streaming service.
     */
    fun setActiveStreamingService(service: SourceType) {
        viewModelScope.launch {
            appSettings.setStreamingService(service.name)
        }
    }
    
    /**
     * Set streaming quality preference.
     */
    fun setStreamingQuality(quality: StreamingQuality) {
        viewModelScope.launch {
            appSettings.setStreamingQuality(quality.name)
        }
    }
    
    /**
     * Check if streaming mode is available (authenticated with at least one service).
     */
    fun isStreamingAvailable(): Boolean {
        return streamingConfig.value.isAuthenticated
    }
}
