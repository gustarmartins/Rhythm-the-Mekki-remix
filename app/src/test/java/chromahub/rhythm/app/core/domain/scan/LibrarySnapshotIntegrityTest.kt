/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.core.domain.scan

import org.junit.Assert.*
import org.junit.Test

class LibrarySnapshotIntegrityTest {
    @Test fun unchangedProviderDoesNotHideTheObservedPartialRoomLibrary() {
        assertTrue(librarySnapshotNeedsRefresh("2258:123:456", "2258:123:456", 56, 56, false))
    }
    @Test fun interruptedScanRetriesEvenIfTheOldLibraryIsStillComplete() {
        assertTrue(librarySnapshotNeedsRefresh("250:123:456", "250:123:456", 250, 250, true))
    }
    @Test fun staleMemorySnapshotAlsoRequiresReload() {
        assertTrue(librarySnapshotNeedsRefresh("250:123:456", "250:123:456", 250, 50, false))
    }
    @Test fun unchangedCompleteLibraryDoesNotRescanIncludingIntentionalEmptyLibrary() {
        assertFalse(librarySnapshotNeedsRefresh("250:123:456", "250:123:456", 250, 250, false))
        assertFalse(librarySnapshotNeedsRefresh("0:0:0", "0:0:0", 0, 0, false))
    }
    @Test fun deletedProviderRowsAndMissingFingerprintTriggerRefresh() {
        assertTrue(librarySnapshotNeedsRefresh("250:123:456", "200:222:333", 250, 250, false))
        assertTrue(librarySnapshotNeedsRefresh(null, "250:123:456", 250, 250, false))
    }
}
