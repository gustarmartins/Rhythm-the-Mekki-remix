/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

import android.content.SharedPreferences
import java.io.IOException

/** Lives outside cacheDir: a killed scan must be retried even after Android clears caches. */
internal class LibraryScanJournal(private val prefs: SharedPreferences) {
    val interrupted: Boolean get() = prefs.getBoolean("scan_in_progress", false)
    val fullScanPending: Boolean get() = prefs.getBoolean("full_scan_pending", false)
    val fingerprint: String? get() = prefs.getString("scoped_fingerprint", null)

    fun requestFullScan() {
        persist(prefs.edit().putBoolean("full_scan_pending", true))
    }

    fun begin(forceRefresh: Boolean) {
        persist(prefs.edit().putBoolean("scan_in_progress", true)
            .putBoolean("full_scan_pending", forceRefresh || fullScanPending))
    }

    /** Called only after the complete Room transaction commits. A crash before this retries safely. */
    fun complete(fingerprint: String, rawCount: Int) {
        persist(prefs.edit().putString("scoped_fingerprint", fingerprint)
            .putInt("last_scan_mediastore_count", rawCount)
            .putBoolean("scan_in_progress", false).putBoolean("full_scan_pending", false))
    }

    private fun persist(editor: SharedPreferences.Editor) {
        if (!editor.commit()) throw IOException("Could not persist library scan checkpoint")
    }
}
