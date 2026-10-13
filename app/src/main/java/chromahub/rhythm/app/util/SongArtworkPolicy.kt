package chromahub.rhythm.app.util

import android.content.Context
import androidx.core.net.toUri
import chromahub.rhythm.app.infrastructure.provider.RhythmAlbumArtProvider
import chromahub.rhythm.app.shared.data.model.Song

/** Also sanitize persisted recent/playlist rows before the main library finishes loading. */
internal object SongArtworkPolicy {
    fun resolve(context: Context, song: Song, lossless: Boolean = false): Song {
        if (song.uri.scheme !in setOf("file", "content") || song.id.contains("::") ||
            song.id.startsWith("subsonic", ignoreCase = true) || song.id.startsWith("jellyfin", ignoreCase = true)) return song
        val prefs = context.getSharedPreferences("artwork_overrides", Context.MODE_PRIVATE)
        val explicit = prefs.getString("uri_${song.id}", null)
        val verified = prefs.getString("verified_uri_v2_${song.id}", null)
        val identity = ArtworkSourcePolicy.identity(song.title, song.artist, song.album)
        val art = when {
            prefs.getBoolean("removed_${song.id}", false) -> null
            explicit != null -> explicit.toUri()
            verified != null && prefs.getString("verified_identity_v2_${song.id}", null) == identity -> verified.toUri()
            else -> RhythmAlbumArtProvider.buildSongUri(song.id, song.path ?: song.uri.takeIf { it.scheme == "file" }?.path, song.albumId, lossless)
        }
        return song.copy(artworkUri = art)
    }
}
