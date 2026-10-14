/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.content.Context
import android.net.Uri
import chromahub.rhythm.app.features.musicassistant.api.*
import chromahub.rhythm.app.features.streaming.data.provider.*
import chromahub.rhythm.app.shared.data.model.LyricsData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Native Rhythm catalog; provider URIs remain opaque, never interpreted as Android file paths. */
internal class MusicAssistantProviderClient(context: Context) {
    val account = MusicAssistantAccount(context)
    fun isConnected() = runCatching { account.server.isNotBlank() && account.token.isNotBlank() }.getOrDefault(false)
    fun getUsername() = account.username
    fun getServerUrl() = account.server
    fun transport(): MusicAssistantHttpTransport {
        val session = account.snapshot()
        return MusicAssistantHttpTransport(session.server, { session.token })
    }
    fun api() = MusicAssistantApi(transport())
    suspend fun login(serverUrl: String, username: String, password: String, saveCredentials: Boolean = true) = result {
        check(android.os.Build.VERSION.SDK_INT >= 28) { "Music Assistant playback requires Android 9 or newer" }
        val server = requireNotNull(MusicAssistantServer.normalize(serverUrl)) { "Invalid server address" }
        val token = MusicAssistantHttpTransport(server, { "" }).login(username, password)
        // A foreground media service must be able to restore this account after process recreation.
        withContext(Dispatchers.Main.immediate) { MusicAssistantPlayer.active.value?.disconnectAccount() }
        account.save(server, username, token)
        ProviderConnectionResult(username, server)
    }
    suspend fun logout() {
        withContext(Dispatchers.Main.immediate) { MusicAssistantPlayer.active.value?.disconnectAccount() }
        account.clear()
    }
    suspend fun ping() = result { api().libraryTracks(limit = 1); Unit }
    suspend fun command(name: String, vararg args: Pair<String, Any?>) = transport().command(name, Gson().toJsonTree(args.toMap()).asJsonObject)
    suspend fun item(uri: String) = command("music/item_by_uri", "uri" to uri).objectOrFail()
    private fun image(item: JsonObject): String? = item.get("metadata")?.takeIf { it.isJsonObject }?.asJsonObject?.get("images")?.takeIf { it.isJsonArray }?.asJsonArray
        ?.firstNotNullOfOrNull { it.asJsonObject.text("proxy_id") }?.let {
            account.server.toHttpUrl().newBuilder().addPathSegment("imageproxy").addPathSegment(it).addQueryParameter("size", "512").build().toString()
        }
    private fun song(item: JsonObject) = MusicAssistantTrack.fromJson(item).toProviderSong().copy(
        artworkUrl = image(item), trackNumber = item.number("track_number")?.toInt(), year = item.get("album")?.takeIf { it.isJsonObject }?.asJsonObject?.number("year")?.toInt(),
    )
    private fun artists(item: JsonObject) = item.get("artists")?.takeIf { it.isJsonArray }?.asJsonArray?.joinToString(", ") { it.asJsonObject.text("name").orEmpty() }.orEmpty()
    private fun album(item: JsonObject) = ProviderAlbum(item.requiredText("uri"), item.requiredText("name"), artists(item), image(item), year = item.number("year")?.toInt())
    private fun artist(item: JsonObject) = ProviderArtist(item.requiredText("uri"), item.requiredText("name"), image(item))
    private fun playlist(item: JsonObject) = ProviderPlaylist(item.requiredText("uri"), item.requiredText("name"), artworkUrl = image(item), owner = if (item.boolean("is_editable") == true) account.username else null)
    private suspend fun library(type: String, limit: Int, search: String? = null, order: String = "name") = command(
        "music/$type/library_items", "limit" to limit.coerceIn(1, 500), "offset" to 0, "search" to search, "order_by" to order
    ).itemsOrFail()
    private suspend fun search(type: String, query: String, limit: Int) = command("music/search", "search_query" to query,
        "media_types" to listOf(type), "limit" to limit.coerceIn(1, 500)).objectOrFail().get("${type}s").itemsOrFail()
    private suspend fun children(command: String, uri: String): List<JsonObject> {
        val parent = item(uri)
        return command(command, "item_id" to parent.requiredText("item_id"), "provider_instance_id_or_domain" to parent.requiredText("provider")).itemsOrFail()
    }
    suspend fun fetchLibrarySongs(limit: Int, onProgress: ((Int, Int, Int) -> Unit)? = null) = result {
        val found = ArrayList<ProviderSong>()
        val maximum = if (limit > 0) limit else Int.MAX_VALUE
        while (found.size < maximum) {
            val count = minOf(200, maximum - found.size)
            val page = command("music/tracks/library_items", "offset" to found.size, "limit" to count, "order_by" to "item_id").itemsOrFail()
            found.addAll(page.map(::song)); onProgress?.invoke(found.size, found.size, found.size)
            if (page.size < count) break
        }
        found.toList()
    }
    suspend fun searchSongs(query: String, limit: Int = 50) = result { search("track", query, limit).map(::song) }
    suspend fun searchAlbums(query: String, limit: Int = 50) = result { search("album", query, limit).map(::album) }
    suspend fun searchArtists(query: String, limit: Int = 50) = result { search("artist", query, limit).map(::artist) }
    suspend fun searchPlaylists(query: String) = result { search("playlist", query, 100).map(::playlist) }
    suspend fun getArtists() = result { library("artists", 500).map(::artist) }
    suspend fun getPlaylists() = result { library("playlists", 500).map(::playlist) }
    suspend fun getPlaylistSongs(uri: String) = result { children("music/playlists/playlist_tracks", uri).map(::song) }
    suspend fun getAlbumSongs(uri: String) = result { children("music/albums/album_tracks", uri).map(::song) }
    suspend fun getAlbumById(uri: String) = result { album(item(uri)) }
    suspend fun getAlbumList(type: String, limit: Int) = result { library("albums", limit, order = if (type == "random") "random" else "timestamp_added_desc").map(::album) }
    suspend fun getRandomSongs(limit: Int) = result { library("tracks", limit, order = "random").map(::song) }
    private suspend fun artistUri(name: String) = if (name.contains("://")) name else search("artist", name, 1).firstOrNull()?.requiredText("uri") ?: error("Artist not found")
    suspend fun getArtistTopTracks(name: String, limit: Int = 50) = result { children("music/artists/top_tracks", artistUri(name)).take(limit).map(::song) }
    suspend fun getArtistAlbums(name: String) = result { children("music/artists/artist_albums", artistUri(name)).map(::album) }
    suspend fun getRelatedArtists(uri: String, limit: Int) = result { children("music/artists/similar_artists", uri).take(limit).map(::artist) }
    suspend fun getRelatedTracks(uri: String, limit: Int) = result { children("music/tracks/similar_tracks", uri).take(limit).map(::song) }
    suspend fun markFavorite(uri: String, enabled: Boolean) = result { api().favorite(uri, enabled) }
    suspend fun getLyrics(uri: String, artist: String?, title: String?) = result {
        api().track(uri).let { LyricsData(it.lyrics, it.syncedLyrics, source = "Music Assistant") }
    }
    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList(), description: String? = null, isPublic: Boolean = true) = result {
        val created = command("music/playlists/create_playlist", "name" to name).objectOrFail()
        if (songIds.isNotEmpty()) addSongsToPlaylist(created.requiredText("uri"), songIds).getOrThrow()
        playlist(created)
    }
    suspend fun addSongsToPlaylist(uri: String, songIds: List<String>) = result {
        command("music/playlists/add_playlist_tracks", "db_playlist_id" to libraryPlaylistId(uri), "uris" to songIds); Unit
    }
    suspend fun updatePlaylist(uri: String, name: String) = result {
        command("music/playlists/update", "item_id" to libraryPlaylistId(uri), "update" to mapOf("name" to name)); Unit
    }
    suspend fun deletePlaylist(uri: String) = result { command("music/playlists/remove", "item_id" to libraryPlaylistId(uri)); Unit }
    suspend fun removeSongsFromPlaylist(uri: String, songIds: List<String>) = result {
        val tracks = children("music/playlists/playlist_tracks", uri)
        val wanted = songIds.toMutableList()
        val positions = tracks.mapIndexedNotNull { index, track ->
            if (wanted.remove(track.requiredText("uri"))) track.number("position")?.toInt() ?: (index + 1) else null
        }
        require(wanted.isEmpty()) { "Playlist changed; refresh before removing songs" }
        command("music/playlists/remove_playlist_tracks", "db_playlist_id" to libraryPlaylistId(uri), "positions_to_remove" to positions); Unit
    }
    private suspend fun libraryPlaylistId(uri: String): String {
        val item = item(uri)
        require(item.requiredText("provider") == "library") { "Add the playlist to your server library before editing it" }
        return item.requiredText("item_id")
    }
    fun buildStreamUrl(uri: String, bitrate: Int? = null) = "musicassistant://track/" + Uri.encode(uri)
    fun buildDownloadUrl(uri: String): String? = null // MA exposes queue audio, not authenticated offline downloads.
    fun buildImageUrl(uri: String): String? = null
    suspend fun <T> result(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
    }
}
