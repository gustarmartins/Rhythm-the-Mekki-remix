/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Deliberate, one-percentage-point adjustments for live audio effects.
 * The dial is read-only: taps, swipes and crossing its gap cannot change gain.
 * Holding repeats after the long-press delay; a held increase can have a lower ceiling.
 * Release, cancellation, disposal and disabling the effect all stop repetition.
 */
@Composable
internal fun AudioEffectStrengthControl(
    title: String,
    strength: Int,
    enabled: Boolean,
    onStrengthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    increaseHoldLimit: Int = 1000
) {
    val value = strength.coerceIn(0, 1000)
    val description = stringResource(R.string.audio_effect_strength_value, title, value / 10)
    val decreaseDescription = stringResource(R.string.audio_effect_decrease, title)
    val increaseDescription = stringResource(R.string.audio_effect_increase, title)

    fun adjust(increase: Boolean, repeating: Boolean): Boolean {
        val next = nextAudioEffectStrength(value, increase, repeating, increaseHoldLimit)
        if (next == value) return false
        onStrengthChange(next) // The caller provides one light haptic tick per actual step.
        return true
    }

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
            RepeatingEffectButton(
                label = "−",
                description = decreaseDescription,
                onClick = { adjust(increase = false, repeating = false) },
                onRepeat = { adjust(increase = false, repeating = true) },
                enabled = enabled && value > 0,
            )
            RepeatingEffectButton(
                label = "+",
                description = increaseDescription,
                onClick = { adjust(increase = true, repeating = false) },
                onRepeat = { adjust(increase = true, repeating = true) },
                enabled = enabled && value < 1000,
            )
        }
        Text(
            text = stringResource(R.string.audio_effect_step_hint),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
    }
}

@Composable
private fun RepeatingEffectButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onRepeat: () -> Boolean
) {
    val interactions = remember { MutableInteractionSource() }
    val click by rememberUpdatedState(onClick)
    val repeatStep by rememberUpdatedState(onRepeat)
    OutlinedIconButton(
        onClick = { click() }, // Keyboard and accessibility keep the ordinary one-step action.
        enabled = enabled,
        interactionSource = interactions,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                coroutineScope {
                    awaitEachGesture {
                        // Own the touch before the button's click detector, so a hold can never
                        // produce an extra click on release (especially above the bass ceiling).
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        down.consume()
                        val press = PressInteraction.Press(down.position)
                        interactions.tryEmit(press)
                        var held = false
                        var released = false
                        val repeating = launch {
                            delay(viewConfiguration.longPressTimeoutMillis)
                            held = true
                            while (isActive && repeatStep()) delay(60L)
                        }
                        try {
                            val up = waitForUpOrCancellation()
                            released = up != null
                            repeating.cancel()
                            if (up != null) {
                                up.consume()
                                if (!held) click()
                            }
                        } finally {
                            repeating.cancel()
                            interactions.tryEmit(if (released) PressInteraction.Release(press)
                                else PressInteraction.Cancel(press))
                        }
                    }
                }
            }
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
