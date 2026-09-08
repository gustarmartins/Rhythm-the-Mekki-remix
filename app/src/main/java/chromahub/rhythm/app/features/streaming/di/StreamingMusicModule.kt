/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.streaming.di

import android.content.Context
import chromahub.rhythm.app.features.streaming.data.repository.StreamingMusicRepositoryImpl
import chromahub.rhythm.app.features.streaming.domain.repository.StreamingMusicRepository

/**
 * Dependency injection module for streaming music feature.
 * Provides minimal stub instances for streaming functionality.
 * 
 * This uses manual DI for now. Can be converted to Hilt/Dagger later.
 */
object StreamingMusicModule {
    
    /**
     * Singleton instance of StreamingMusicRepository.
     */
    @Volatile
    private var streamingMusicRepository: StreamingMusicRepository? = null
    
    /**
     * Provides StreamingMusicRepository instance.
     * Currently returns a stub implementation.
     */
    fun provideStreamingMusicRepository(context: Context): StreamingMusicRepository {
        return streamingMusicRepository ?: synchronized(this) {
            streamingMusicRepository ?: StreamingMusicRepositoryImpl(context.applicationContext).also {
                streamingMusicRepository = it
            }
        }
    }
    
}
