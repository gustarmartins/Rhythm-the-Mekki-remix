package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.LyricsSourcePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LyricsSourceFallbackTest {
    private val embedded = LyricsData("Embedded words", "[00:01.00]Embedded words")
    private val online = LyricsData("Online words", "[00:01.00]Online words")

    @Test fun missingLocalFileUsesEmbeddedWithoutQueryingAnApi() = runBlocking {
        val visited = mutableListOf<LyricsOrigin>()
        val found = firstAvailableLyrics(LyricsSourcePreference.LOCAL_FIRST, fetch = {
            visited.add(it)
            if (it == LyricsOrigin.EMBEDDED) embedded else null
        })
        assertEquals(embedded, found?.second)
        assertEquals(listOf(LyricsOrigin.LOCAL, LyricsOrigin.EMBEDDED), visited)
    }
    @Test fun noLocalOrEmbeddedLyricsFallsBackToOnline() = runBlocking {
        val visited = mutableListOf<LyricsOrigin>()
        val found = firstAvailableLyrics(LyricsSourcePreference.LOCAL_FIRST, fetch = {
            visited.add(it)
            if (it == LyricsOrigin.API) online else null
        })
        assertEquals(LyricsOrigin.API to online, found)
        assertEquals(listOf(LyricsOrigin.LOCAL, LyricsOrigin.EMBEDDED, LyricsOrigin.API), visited)
    }
    @Test fun unreadableLocalAndErrorMessageEmbeddedDoNotBlockOnline() = runBlocking {
        val found = firstAvailableLyrics(LyricsSourcePreference.LOCAL_FIRST, fetch = {
            when (it) {
                LyricsOrigin.LOCAL -> throw java.io.IOException("Missing file")
                LyricsOrigin.EMBEDDED -> LyricsData("No lyrics found for this song", null)
                LyricsOrigin.API -> online
            }
        })
        assertEquals(LyricsOrigin.API to online, found)
    }
    @Test fun unavailableApiKeepsApiFirstUsersEmbeddedFallback() = runBlocking {
        assertEquals(LyricsOrigin.EMBEDDED to embedded,
            firstAvailableLyrics(LyricsSourcePreference.API_FIRST, fetch = {
                when (it) {
                    LyricsOrigin.API -> throw java.io.IOException("Offline")
                    LyricsOrigin.EMBEDDED -> embedded
                    LyricsOrigin.LOCAL -> null
                }
            }))
    }
    @Test fun cancelledSongDoesNotContinueFetchingFromOtherSources() = runBlocking {
        val visited = mutableListOf<LyricsOrigin>()
        try {
            firstAvailableLyrics(LyricsSourcePreference.LOCAL_FIRST, fetch = {
                visited.add(it)
                throw CancellationException("Song changed")
            })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(listOf(LyricsOrigin.LOCAL), visited)
        }
    }
}
