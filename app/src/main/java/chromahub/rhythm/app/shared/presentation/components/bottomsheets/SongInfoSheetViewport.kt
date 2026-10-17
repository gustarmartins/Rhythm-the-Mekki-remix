/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.util.windowScreenHeightDp

/** Loading metadata must not move the sheet's Expanded anchor under an active finger. */
@Composable
internal fun SongInfoSheetViewport(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .height((windowScreenHeightDp() * 0.85f).dp)
            .padding(bottom = 16.dp),
        content = content
    )
}
