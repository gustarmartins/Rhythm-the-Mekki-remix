package chromahub.rhythm.app.util

/** Public Better Lyrics cache keys include album and whole-second duration. */
internal data class BetterLyricsQuery(val album: String?, val durationSeconds: Long?) {
    companion object {
        fun from(album: String?, durationMs: Long?) = BetterLyricsQuery(
            album?.trim()?.takeIf { it.isNotEmpty() },
            durationMs?.takeIf { it >= 1000 }?.div(1000)
        )
    }
}
