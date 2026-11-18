/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.infrastructure.service.player

/** Do not suppress native repeat/advance for the whole track while waiting for a fade. */
internal fun shouldArmCrossfadeEndPause(remainingUntilTransitionMs: Long): Boolean =
    remainingUntilTransitionMs in 0L..2000L
