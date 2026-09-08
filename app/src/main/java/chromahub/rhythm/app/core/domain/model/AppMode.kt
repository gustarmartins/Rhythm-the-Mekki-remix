/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.core.domain.model

/**
 * Represents the current application mode.
 * Users can switch between local music playback and streaming services.
 */
enum class AppMode {
    /**
     * Local mode - plays music stored on the device.
     * Uses MediaStore to scan and access local audio files.
     */
    LOCAL,
    
    /**
     * Streaming mode - plays music from streaming services.
     * Requires internet connection and service authentication.
     */
    STREAMING
}

/**
 * Configuration for the streaming mode.
 */
data class StreamingConfig(
    /**
     * The currently selected streaming service.
     */
    val activeService: SourceType = SourceType.SUBSONIC,
    
    /**
     * Whether the user is authenticated with the active service.
     */
    val isAuthenticated: Boolean = false,
    
    /**
     * Preferred streaming quality.
     */
    val streamingQuality: StreamingQuality = StreamingQuality.HIGH,
    
    /**
     * Whether to allow streaming over cellular data.
     */
    val allowCellularStreaming: Boolean = true,
    
    /**
     * Whether to download tracks for offline playback.
     */
    val offlineMode: Boolean = false
)

/**
 * Streaming quality options.
 */
enum class StreamingQuality {
    LOW,      // ~96 kbps
    NORMAL,   // ~160 kbps
    HIGH,     // ~320 kbps
    LOSSLESS  // FLAC/ALAC where available
}
