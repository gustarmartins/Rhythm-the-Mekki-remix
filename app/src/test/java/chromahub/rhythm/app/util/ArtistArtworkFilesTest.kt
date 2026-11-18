/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class ArtistArtworkFilesTest {
    @Test fun unicodeAndPunctuationCollisionsReceiveDifferentPrivateFiles() {
        for ((a,b) in listOf("米" to "空", "AC/DC" to "AC:DC", "Björk" to "Bjork"))
            assertNotEquals(ArtistArtworkFiles.fileName(a), ArtistArtworkFiles.fileName(b))
    }
    @Test fun caseWhitespaceAndCanonicalUnicodeAreStable() {
        assertEquals(ArtistArtworkFiles.fileName("  BJÖRK "), ArtistArtworkFiles.fileName("Björk"))
        assertEquals(ArtistArtworkFiles.fileName("ＡＢＣ"), ArtistArtworkFiles.fileName("abc"))
    }
    @Test fun ambiguousLegacyOwnersCannotBothReceiveTheSameOldImage() {
        val owners = ArtistArtworkFiles.legacyOwners(listOf("A/B", "A:B"))
        assertFalse(ArtistArtworkFiles.canMigrate("A/B", owners[ArtistArtworkFiles.legacyName("A/B")].orEmpty()))
        assertTrue(ArtistArtworkFiles.canMigrate("Annie Lennox", setOf(ArtistArtworkFiles.key("Annie Lennox"))))
        assertFalse(ArtistArtworkFiles.canMigrate("米", setOf(ArtistArtworkFiles.key("米"))))
    }
    @Test fun filenamesCannotEscapeTheArtistCacheDirectory() {
        val name = ArtistArtworkFiles.fileName("../../a/b:")
        assertFalse(name.contains('/')); assertFalse(name.contains("..")); assertTrue(name.endsWith(".jpg"))
    }
}
