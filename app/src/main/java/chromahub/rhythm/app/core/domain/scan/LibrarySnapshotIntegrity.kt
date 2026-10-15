/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

/** Provider equality alone cannot prove that Room still holds the completed snapshot. */
internal fun librarySnapshotNeedsRefresh(
    previousFingerprint: String?,
    currentFingerprint: String,
    roomCount: Int,
    cachedCount: Int,
    interrupted: Boolean
): Boolean {
    val expectedCount = currentFingerprint.substringBefore(':').toIntOrNull() ?: return true
    return interrupted || previousFingerprint != currentFingerprint ||
        roomCount != expectedCount || cachedCount != expectedCount
}
