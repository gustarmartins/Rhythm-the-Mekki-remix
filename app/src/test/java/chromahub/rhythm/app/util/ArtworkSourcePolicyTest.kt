package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArtworkSourcePolicyTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun arbitraryPersonalImagesAreNeverCovers() {
        val folder = temp.newFolder("album")
        listOf("holiday.jpg", "private.png", "Screenshot_123.webp").forEach { folder.resolve(it).writeText("image") }
        assertNull(ArtworkSourcePolicy.findFolderCover(folder))
    }
    @Test fun conventionalCoverInDedicatedFolderWorks() {
        val folder = temp.newFolder("album")
        folder.resolve("random.jpg").writeText("image")
        val cover = folder.resolve("Cover.PNG").apply { writeText("image") }
        assertEquals(cover, ArtworkSourcePolicy.findFolderCover(folder))
    }
    @Test fun sharedFolderCannotSupplyAllSongsWithOneCover() {
        val folder = temp.newFolder("Music")
        folder.resolve("cover.jpg").writeText("image")
        assertNull(ArtworkSourcePolicy.findFolderCover(folder))
    }
    @Test fun metadataEditsInvalidateAutomaticCoverIdentity() {
        assertNotEquals(ArtworkSourcePolicy.identity("Track", "Artist", "Studio"),
            ArtworkSourcePolicy.identity("Track", "Artist", "Live"))
        assertNotEquals(ArtworkSourcePolicy.identity("Track", "Artist", "Studio"),
            ArtworkSourcePolicy.identity("Other Track", "Artist", "Studio"))
    }
    @Test fun mismatchedAndAmbiguousApiResultsAreRejected() {
        assertFalse(ArtworkSourcePolicy.matches("Nightcore Mix Compilation", "Various Artists", "Prism", "Katy Perry"))
        assertFalse(ArtworkSourcePolicy.matches("Prism", "Other Artist", "Prism", "Katy Perry"))
        assertFalse(ArtworkSourcePolicy.matches("Prism", "Katy Perry", "Prism Live", "Katy Perry"))
        assertFalse(ArtworkSourcePolicy.matches("Compilation", "Various Artists", "Compilation", "Various Artists"))
        assertTrue(ArtworkSourcePolicy.matches("PRISM", "Katy Perry", "Prism", "katy perry"))
    }
}
