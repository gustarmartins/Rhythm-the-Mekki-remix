/*
 * Copyright (C) 2025 nift4 (Gramophone)
 * Modified for Rhythm by Anjishnu Nandi (cromaguy)
 *
 * SPDX-FileCopyrightText: 2025 nift4 <https://github.com/FoedusProgramme/Gramophone>
 * SPDX-FileCopyrightText: 2025-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.infrastructure.widget.glance

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class RhythmLyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RhythmLyricsWidget()
}
