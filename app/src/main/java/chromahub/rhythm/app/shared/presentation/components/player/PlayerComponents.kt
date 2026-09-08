/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.player

import java.util.concurrent.TimeUnit
import java.util.Locale

/**
 * Format duration from milliseconds to mm:ss or h:mm:ss format
 * @param durationMs Duration in milliseconds
 * @param useHoursFormat If true, shows hours when duration is >= 60 minutes (e.g., 1:32:26 instead of 92:26)
 */
fun formatDuration(durationMs: Long, useHoursFormat: Boolean = false): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
            TimeUnit.MINUTES.toSeconds(totalMinutes)
    
    return if (useHoursFormat && totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", totalMinutes, seconds)
    }
}
