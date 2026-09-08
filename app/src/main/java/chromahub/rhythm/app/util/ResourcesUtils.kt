/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.content.res.Resources
import java.util.IllegalFormatException

/**
 * Returns a plural quantity string without ever crashing on format-placeholder
 * mismatches introduced by translations.
 *
 * `Resources.getQuantityString` forwards the string to `String.format` with the
 * supplied arguments, so a translation that carries a placeholder the app does
 * not provide an argument for (e.g. a leftover `%2$s` while the English source
 * only has `%1$d`) throws [IllegalFormatException] on non-English locales.
 *
 * When that happens, this helper falls back to the raw quantity text with any
 * `%N$c` format specifiers stripped, so the UI keeps working instead of crashing.
 */
fun Resources.safeGetQuantityString(
    id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    return try {
        getQuantityString(id, quantity, *formatArgs)
    } catch (_: IllegalFormatException) {
        getQuantityText(id, quantity)
            .toString()
            .replace(Regex("%(?:\\d+\\$)?[A-Za-z]"), "")
            .trim()
            .replace(Regex("\\s{2,}"), " ")
    }
}
