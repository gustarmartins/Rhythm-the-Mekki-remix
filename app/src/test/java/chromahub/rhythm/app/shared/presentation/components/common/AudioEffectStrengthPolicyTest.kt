/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.common

import org.junit.Assert.*
import org.junit.Test

class AudioEffectStrengthPolicyTest {
    @Test fun heldBassIncreaseReachesExactlySeventyFivePercentAndStops() {
        var value = 0
        repeat(200) { value = nextAudioEffectStrength(value, true, true, 750) }
        assertEquals(750, value)
        assertEquals(750, nextAudioEffectStrength(745, true, true, 750))
    }

    @Test fun holdingAboveBassCeilingNeverRaisesOrLowersExistingValue() {
        for (value in 750..1000) assertEquals(value, nextAudioEffectStrength(value, true, true, 750))
    }

    @Test fun individualTapsContinueInOnePercentStepsAllTheWayToMaximum() {
        var value = 750
        repeat(25) { step ->
            value = nextAudioEffectStrength(value, true, false, 750)
            assertEquals(760 + step * 10, value)
        }
        assertEquals(1000, nextAudioEffectStrength(value, true, false, 750))
    }

    @Test fun heldDecreaseWorksAboveTheCapAndContinuesToZero() {
        var value = 1000
        repeat(100) { step ->
            value = nextAudioEffectStrength(value, false, true, 750)
            assertEquals(990 - step * 10, value)
        }
        assertEquals(0, nextAudioEffectStrength(value, false, true, 750))
    }

    @Test fun spatialEffectKeepsItsFullHoldRange() {
        assertEquals(910, nextAudioEffectStrength(900, true, true))
        assertEquals(1000, nextAudioEffectStrength(995, true, true))
    }

    @Test fun fineValuesAndEndpointsNeverWrap() {
        assertEquals(0, nextAudioEffectStrength(5, false, true, 750))
        assertEquals(1000, nextAudioEffectStrength(995, true, false, 750))
        assertEquals(245, nextAudioEffectStrength(235, true, false, 750))
    }
}
