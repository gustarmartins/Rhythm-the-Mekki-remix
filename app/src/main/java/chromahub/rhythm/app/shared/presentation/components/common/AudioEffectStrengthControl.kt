/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R

/**
 * Deliberate, one-percentage-point adjustments for live audio effects.
 * The dial is read-only: taps, swipes and crossing its gap cannot change gain.
 * Buttons have no hold-to-repeat behavior and cannot enable a disabled effect.
 */
@Composable
internal fun AudioEffectStrengthControl(
    title: String,
    strength: Int,
    enabled: Boolean,
    onStrengthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val value = strength.coerceIn(0, 1000)
    val description = stringResource(R.string.audio_effect_strength_value, title, value / 10)
    val decreaseDescription = stringResource(R.string.audio_effect_decrease, title)
    val increaseDescription = stringResource(R.string.audio_effect_increase, title)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp).clearAndSetSemantics {
                contentDescription = description
            }
        ) {
            ArcProgressIndicator(
                value = value.toFloat(),
                modifier = Modifier.fillMaxSize(),
                enabled = enabled,
                valueRange = 0f..1000f,
                activeTrackColor = if (enabled) activeColor else MaterialTheme.colorScheme.outlineVariant,
                inactiveTrackColor = if (enabled) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                thumbColor = if (enabled) activeColor else MaterialTheme.colorScheme.outline
            )
            Text(
                text = stringResource(R.string.settings_value_percent, value / 10),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedIconButton(
                onClick = { onStrengthChange((value - 10).coerceAtLeast(0)) },
                enabled = enabled && value > 0,
                modifier = Modifier.size(48.dp).semantics { contentDescription = decreaseDescription }
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedIconButton(
                onClick = { onStrengthChange((value + 10).coerceAtMost(1000)) },
                enabled = enabled && value < 1000,
                modifier = Modifier.size(48.dp).semantics { contentDescription = increaseDescription }
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            text = stringResource(R.string.audio_effect_step_hint),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
    }
}
