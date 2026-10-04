/* SPDX-FileCopyrightText: 2026 Gustavo Martins
 * SPDX-License-Identifier: GPL-3.0-or-later */

package chromahub.rhythm.app.util

import java.util.Locale

/** Stable profile identity; names remain labels and support existing saved presets. */
data class BluetoothDisplayDevice(val key: String, val name: String) {
    companion object {
        fun from(address: String?, name: String): BluetoothDisplayDevice {
            val label = name.trim().ifEmpty { "Bluetooth" }
            val normalizedAddress = address?.trim()?.takeIf {
                it.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")) &&
                    it != "00:00:00:00:00:00" && it != "02:00:00:00:00:00"
            }
            val key = normalizedAddress?.let { "address:" + it.uppercase(Locale.ROOT) }
                ?: "name:" + label.lowercase(Locale.ROOT)
            return BluetoothDisplayDevice(key, label)
        }
    }
}

internal fun <T> bluetoothDisplayProfileValue(
    device: BluetoothDisplayDevice?, profiles: Map<String, T>, fallback: T
): T = device?.let { profiles[it.key] ?: profiles[it.name] } ?: fallback

internal fun bluetoothDisplayCompatibilityEnabled(
    device: BluetoothDisplayDevice?, profiles: Map<String, Boolean>, legacyDefault: Boolean
): Boolean = device != null && bluetoothDisplayProfileValue(device, profiles, legacyDefault)
