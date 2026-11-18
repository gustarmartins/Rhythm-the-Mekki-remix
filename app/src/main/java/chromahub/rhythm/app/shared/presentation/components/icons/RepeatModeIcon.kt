/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.icons

import androidx.media3.common.Player

/** Media3 owns the integer values: One is 1, All is 2. */
fun repeatModeIcon(repeatMode: Int): MaterialSymbolIcon = when (repeatMode) {
    Player.REPEAT_MODE_ONE -> RhythmIcons.Player.RepeatOne
    else -> RhythmIcons.Player.Repeat
}
