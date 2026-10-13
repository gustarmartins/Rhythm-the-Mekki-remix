package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.BluetoothLyricsTextMode
import androidx.annotation.Keep

/** One device's complete lyric presentation. The synced-lyrics master remains app-wide. */
@Keep
data class BluetoothLyricsProfile(
    val offsetMs: Int = 0,
    val maxChunkChars: Int = BluetoothLyricsFormatter.DEFAULT_MAX_CHUNK_CHARS,
    val scrollCharsPerSecond: Int = BluetoothLyricsFormatter.DEFAULT_SCROLL_CHARS_PER_SECOND,
    val minChunkHoldMs: Int = BluetoothLyricsFormatter.DEFAULT_MIN_CHUNK_HOLD_MS,
    val metadataUpdateIntervalMs: Int = BluetoothLyricsFormatter.DEFAULT_METADATA_UPDATE_INTERVAL_MS,
    val textMode: BluetoothLyricsTextMode = BluetoothLyricsTextMode.ROMANIZATION
)

internal fun resolveBluetoothLyricsProfile(
    device: BluetoothDisplayDevice?,
    profiles: Map<String, BluetoothLyricsProfile>,
    defaults: BluetoothLyricsProfile,
    legacyOffsets: Map<String, Int>
): BluetoothLyricsProfile {
    // Phone/USB displays retain their own defaults, with no Bluetooth timing adjustment.
    if (device == null) return defaults.copy(offsetMs = 0)
    return bluetoothDisplayProfileValue(device, profiles,
        defaults.copy(offsetMs = bluetoothDisplayProfileValue(device, legacyOffsets, defaults.offsetMs)))
}
