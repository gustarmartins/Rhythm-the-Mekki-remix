package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class BetterLyricsQueryTest {
    @Test fun sendsAlbumAndWholeSecondsForThePublicCache() {
        assertEquals(BetterLyricsQuery("OCTANE", 154), BetterLyricsQuery.from(" OCTANE ", 154_800))
    }
    @Test fun missingMetadataDoesNotInventAnAlbumOrDuration() {
        assertEquals(BetterLyricsQuery(null, null), BetterLyricsQuery.from("  ", 0))
        assertEquals(BetterLyricsQuery(null, null), BetterLyricsQuery.from(null, null))
        assertNull(BetterLyricsQuery.from("Album", -9223372036854775807L).durationSeconds)
    }
}
