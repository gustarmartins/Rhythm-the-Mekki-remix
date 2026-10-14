package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.LyricsSourcePreference
import kotlinx.coroutines.CancellationException

internal enum class LyricsOrigin(val label: String) {
    LOCAL("Local .lrc"), EMBEDDED("Embedded"), API("Online API")
}

/** A missing, empty, failed or error-message source must not prevent trying the next one. */
internal suspend fun firstAvailableLyrics(
    preference: LyricsSourcePreference,
    fetch: suspend (LyricsOrigin) -> LyricsData?,
    onFailure: (LyricsOrigin, Exception) -> Unit = { _, _ -> }
): Pair<LyricsOrigin, LyricsData>? {
    val order = when (preference) {
        LyricsSourcePreference.LOCAL_FIRST -> listOf(LyricsOrigin.LOCAL, LyricsOrigin.EMBEDDED, LyricsOrigin.API)
        LyricsSourcePreference.EMBEDDED_FIRST -> listOf(LyricsOrigin.EMBEDDED, LyricsOrigin.API, LyricsOrigin.LOCAL)
        LyricsSourcePreference.API_FIRST -> listOf(LyricsOrigin.API, LyricsOrigin.EMBEDDED, LyricsOrigin.LOCAL)
    }
    for (origin in order) {
        val result = try { fetch(origin) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { onFailure(origin, e); null }
        if (result?.hasLyrics() == true && !result.isErrorMessage()) return origin to result
    }
    return null
}
