/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.data.repository

import android.content.Context
import android.content.SharedPreferences
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.core.domain.model.StreamingConfig
import chromahub.rhythm.app.core.domain.model.StreamingQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * Repository for managing user preferences related to app mode and settings.
 */
class UserPreferencesRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    private val _appMode = MutableStateFlow(loadAppMode())
    val appMode: Flow<AppMode> = _appMode.asStateFlow()
    
    private val _streamingConfig = MutableStateFlow(loadStreamingConfig())
    val streamingConfig: Flow<StreamingConfig> = _streamingConfig.asStateFlow()
    
    /**
     * Get the current app mode synchronously.
     */
    fun getCurrentAppMode(): AppMode = _appMode.value
    
    /**
     * Set the app mode.
     */
    fun setAppMode(mode: AppMode) {
        prefs.edit { putString(KEY_APP_MODE, mode.name) }
        _appMode.value = mode
    }
    
    /**
     * Update the streaming configuration.
     */
    fun updateStreamingConfig(config: StreamingConfig) {
        prefs.edit {
            putString(KEY_ACTIVE_SERVICE, config.activeService.name)
            putBoolean(KEY_IS_AUTHENTICATED, config.isAuthenticated)
            putString(KEY_STREAMING_QUALITY, config.streamingQuality.name)
            putBoolean(KEY_ALLOW_CELLULAR, config.allowCellularStreaming)
            putBoolean(KEY_OFFLINE_MODE, config.offlineMode)
        }
        _streamingConfig.value = config
    }
    
    /**
     * Set the active streaming service.
     */
    fun setActiveService(service: SourceType) {
        val current = _streamingConfig.value
        updateStreamingConfig(current.copy(activeService = service))
    }
    
    /**
     * Set authentication status for current service.
     */
    fun setAuthenticated(authenticated: Boolean) {
        val current = _streamingConfig.value
        updateStreamingConfig(current.copy(isAuthenticated = authenticated))
    }
    
    /**
     * Set streaming quality preference.
     */
    fun setStreamingQuality(quality: StreamingQuality) {
        val current = _streamingConfig.value
        updateStreamingConfig(current.copy(streamingQuality = quality))
    }
    
    private fun loadAppMode(): AppMode {
        val modeName = prefs.getString(KEY_APP_MODE, AppMode.LOCAL.name)
        return try {
            AppMode.valueOf(modeName ?: AppMode.LOCAL.name)
        } catch (e: IllegalArgumentException) {
            AppMode.LOCAL
        }
    }
    
    private fun loadStreamingConfig(): StreamingConfig {
        val serviceName = prefs.getString(KEY_ACTIVE_SERVICE, SourceType.SUBSONIC.name)
        val activeService = try {
            SourceType.valueOf(serviceName ?: SourceType.SUBSONIC.name)
        } catch (e: IllegalArgumentException) {
            SourceType.SUBSONIC
        }
        
        val qualityName = prefs.getString(KEY_STREAMING_QUALITY, StreamingQuality.HIGH.name)
        val quality = try {
            StreamingQuality.valueOf(qualityName ?: StreamingQuality.HIGH.name)
        } catch (e: IllegalArgumentException) {
            StreamingQuality.HIGH
        }
        
        return StreamingConfig(
            activeService = activeService,
            isAuthenticated = prefs.getBoolean(KEY_IS_AUTHENTICATED, false),
            streamingQuality = quality,
            allowCellularStreaming = prefs.getBoolean(KEY_ALLOW_CELLULAR, true),
            offlineMode = prefs.getBoolean(KEY_OFFLINE_MODE, false)
        )
    }
    
    companion object {
        private const val PREFS_NAME = "rhythm_user_preferences"
        private const val KEY_APP_MODE = "app_mode"
        private const val KEY_ACTIVE_SERVICE = "active_streaming_service"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_STREAMING_QUALITY = "streaming_quality"
        private const val KEY_ALLOW_CELLULAR = "allow_cellular_streaming"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null
        
        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
