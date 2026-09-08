/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.data.model

sealed class ScanPhase(val displayName: String) {
    data object Idle : ScanPhase("Idle")
    data object Songs : ScanPhase("Songs")
    data object Incremental : ScanPhase("Incremental")
    data object SavingDb : ScanPhase("Saving Database")
    data object Complete : ScanPhase("Complete")
    data object Error : ScanPhase("Error")
    data object PermissionDenied : ScanPhase("Permission Denied")
}
