/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

/** Taps retain 1% steps over the full range; only a held increase has a lower ceiling. */
internal fun nextAudioEffectStrength(
    value: Int,
    increase: Boolean,
    repeating: Boolean,
    increaseHoldLimit: Int = 1000
): Int {
    val current = value.coerceIn(0, 1000)
    if (!increase) return (current - 10).coerceAtLeast(0)
    val ceiling = if (repeating) increaseHoldLimit.coerceIn(0, 1000) else 1000
    // Holding + above its ceiling must leave the current setting alone, never pull it down.
    return if (current >= ceiling) current else (current + 10).coerceAtMost(ceiling)
}
