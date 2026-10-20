/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Native catalog/control adapters, independent of the embedded frontend and Android UI.
 * All mutations are serialized through this session. Server queues remain authoritative;
 * a caller must read them back before changing its visible queue or playback state.
 */
internal class MusicAssistantApi(private val transport: MusicAssistantTransport) {
    private val mutations = Mutex()

    suspend fun serverTime(): Double {
        val result = transport.command("time", JsonObject())
        if (!result.isJsonPrimitive || !result.asJsonPrimitive.isNumber) invalidResponse()
        return result.asDouble.takeIf { it.isFinite() && it > 0 } ?: invalidResponse()
    }

    suspend fun libraryTracks(offset: Int = 0, limit: Int = 200, search: String? = null): List<MusicAssistantTrack> {
        val args = page(offset, limit).apply { if (search != null) addProperty("search", search) }
        return transport.command("music/tracks/library_items", args).itemsOrFail().map(MusicAssistantTrack::fromJson)
    }

    suspend fun searchTracks(query: String, limit: Int = 50, libraryOnly: Boolean = false): List<MusicAssistantTrack> {
        return searchItems("track", query, limit, libraryOnly).map(MusicAssistantTrack::fromJson)
    }

    suspend fun searchItems(type: String, query: String, limit: Int = 50, libraryOnly: Boolean = false): List<JsonObject> {
        require(type in listOf("track", "album", "artist", "playlist"))
        require(query.isNotBlank())
        require(limit in 1..500)
        val result = transport.command("music/search", args(
            "search_query" to query, "media_types" to listOf(type),
            "limit" to limit, "library_only" to libraryOnly,
        )).objectOrFail()
        return (result.get("${type}s") ?: invalidResponse()).itemsOrFail()
    }

    suspend fun track(uri: String): MusicAssistantTrack = MusicAssistantTrack.fromJson(
        transport.command("music/item_by_uri", args("uri" to identifier(uri))).objectOrFail()
    )

    suspend fun albumTracks(itemId: String, provider: String, offset: Int = 0, limit: Int = 200): List<MusicAssistantTrack> =
        transport.command("music/albums/album_tracks", page(offset, limit).apply {
            addProperty("item_id", identifier(itemId))
            addProperty("provider_instance_id_or_domain", identifier(provider))
        }).itemsOrFail().map(MusicAssistantTrack::fromJson)

    suspend fun players(): List<MusicAssistantPlayer> = transport.command("players/all", JsonObject())
        .itemsOrFail().map(MusicAssistantPlayer::fromJson)

    /** Resolve a grouped player's actual queue before issuing queue commands. */
    suspend fun activeQueue(playerId: String): MusicAssistantQueue? = transport.command(
        "player_queues/get_active_queue", args("player_id" to identifier(playerId))
    ).takeUnless { it.isJsonNull }?.objectOrFail()?.let(MusicAssistantQueue::fromJson)

    suspend fun queueItems(queueId: String, offset: Int = 0, limit: Int = 200): List<MusicAssistantQueueItem> =
        transport.command("player_queues/items", page(offset, limit).apply {
            addProperty("queue_id", identifier(queueId))
        }).itemsOrFail().map(MusicAssistantQueueItem::fromJson)

    /** Explicit Play action only. Failure never falls back to another queue or player. */
    suspend fun play(queueId: String, mediaUris: List<String>) = playMedia(queueId, mediaUris, "replace")

    /**
     * Replace an explicit track list without briefly playing its first track.
     * MA's start_item only selects within containers (albums/playlists), not a batch of tracks.
     * Stage the ordered list while stopped, then select the exact returned occurrence.
     */
    suspend fun replaceQueueAndSelect(queueId: String, uris: List<String>, startIndex: Int,
                                      positionSeconds: Int = 0, shuffle: Boolean = false) {
        require(uris.isNotEmpty() && startIndex in uris.indices && positionSeconds >= 0)
        uris.forEach(::identifier)
        mutations.withLock {
            transport.command("player_queues/clear", args("queue_id" to identifier(queueId)))
            transport.command("player_queues/shuffle", args("queue_id" to queueId, "shuffle_enabled" to false))
            transport.command("player_queues/play_media", args("queue_id" to queueId, "media" to uris, "option" to "add"))
            val loaded = ArrayList<MusicAssistantQueueItem>()
            while (true) {
                val page = queueItems(queueId, loaded.size, 500)
                loaded.addAll(page)
                if (page.size < 500 || loaded.size > uris.size) break
            }
            check(loaded.size == uris.size && loaded.map { it.occurrenceId }.toSet().size == loaded.size) {
                "Server queue changed while selecting a track; refresh and retry"
            }
            check(orderMusicAssistantAdditions(uris, loaded).map { it.occurrenceId } == loaded.map { it.occurrenceId }) {
                "Server queue order changed while selecting a track; refresh and retry"
            }
            transport.command("player_queues/play_index", args("queue_id" to queueId,
                "index" to loaded[startIndex].occurrenceId, "seek_position" to positionSeconds))
            if (shuffle) transport.command("player_queues/shuffle", args("queue_id" to queueId, "shuffle_enabled" to true))
        }
    }

    /** Append preserves caller order and duplicates. This is NOT Rhythm's FIFO Play Next adapter. */
    suspend fun append(queueId: String, mediaUris: List<String>) = playMedia(queueId, mediaUris, "add")

    private suspend fun playMedia(queueId: String, uris: List<String>, option: String) {
        require(uris.isNotEmpty() && uris.size <= 500)
        uris.forEach(::identifier)
        mutate("player_queues/play_media", args(
            "queue_id" to identifier(queueId), "media" to uris, "option" to option,
        ))
    }

    enum class PlayerAction(val wireName: String) { PLAY("play"), PAUSE("pause"), STOP("stop"), NEXT("next"), PREVIOUS("previous") }

    suspend fun playerAction(playerId: String, action: PlayerAction) = mutate(
        "players/cmd/${action.wireName}", args("player_id" to identifier(playerId))
    )

    suspend fun volume(playerId: String, volume: Int) {
        require(volume in 0..100)
        mutate("players/cmd/volume_set", args("player_id" to identifier(playerId), "volume_level" to volume))
    }

    suspend fun seek(queueId: String, positionSeconds: Int) {
        require(positionSeconds >= 0)
        mutate("player_queues/seek", args("queue_id" to identifier(queueId), "position" to positionSeconds))
    }

    suspend fun transfer(sourceQueueId: String, targetQueueId: String) {
        require(sourceQueueId != targetQueueId)
        // Omit auto_play: the server preserves the source's paused/playing state.
        mutate("player_queues/transfer", args(
            "source_queue_id" to identifier(sourceQueueId), "target_queue_id" to identifier(targetQueueId),
        ))
    }

    suspend fun groupMembers(targetPlayer: String, add: List<String>, remove: List<String>) {
        require(add.isNotEmpty() || remove.isNotEmpty())
        require(add.size <= 100 && remove.size <= 100)
        require(add.toSet().intersect(remove.toSet()).isEmpty())
        require(targetPlayer !in add && targetPlayer !in remove)
        (add + remove).forEach(::identifier)
        // The server determines compatibility; arbitrary devices cannot be promised synchronization.
        mutate("players/cmd/set_members", args(
            "target_player" to identifier(targetPlayer),
            "player_ids_to_add" to add.distinct(), "player_ids_to_remove" to remove.distinct(),
        ))
    }

    suspend fun favorite(uri: String, enabled: Boolean) {
        identifier(uri)
        mutations.withLock {
            // These commands work on Stable (2.10.6) and development servers. The newer
            // set_item API is not available on Stable, even at API schema version 65.
            if (enabled) {
                transport.command("music/favorites/add_item", args("item" to uri))
            } else {
                val item = transport.command("music/item_by_uri", args("uri" to uri)).objectOrFail()
                val libraryItem = if (item.requiredText("provider") == "library") item else {
                    transport.command("music/get_library_item", args(
                        "media_type" to item.requiredText("media_type"),
                        "item_id" to item.requiredText("item_id"),
                        "provider_instance_id_or_domain" to item.requiredText("provider"),
                    )).takeUnless { it.isJsonNull }?.objectOrFail() ?: return@withLock
                }
                if (libraryItem.requiredText("provider") != "library") invalidResponse()
                // Removal takes a library ID, not a provider ID or URI. It clears the
                // favorite; sending false to set_item would instead create a dislike.
                transport.command("music/favorites/remove_item", args(
                    "media_type" to libraryItem.requiredText("media_type"),
                    "library_item_id" to libraryItem.requiredText("item_id"),
                ))
            }
        }
    }

    private suspend fun mutate(command: String, args: JsonObject) {
        mutations.withLock { transport.command(command, args) }
    }

    private fun page(offset: Int, limit: Int): JsonObject {
        require(offset >= 0 && limit in 1..500)
        return args("offset" to offset, "limit" to limit)
    }

    private fun identifier(value: String): String = value.also { require(it.isNotBlank() && it.length <= 8192) }

    private fun args(vararg values: Pair<String, Any?>) = JsonObject().apply {
        values.forEach { (key, value) -> add(key, json(value)) }
    }

    private fun json(value: Any?): JsonElement = when (value) {
        null -> JsonNull.INSTANCE
        is String -> com.google.gson.JsonPrimitive(value)
        is Boolean -> com.google.gson.JsonPrimitive(value)
        is Number -> com.google.gson.JsonPrimitive(value)
        is List<*> -> JsonArray().apply { value.forEach { add(json(it)) } }
        else -> error("Unsupported command argument")
    }
}
