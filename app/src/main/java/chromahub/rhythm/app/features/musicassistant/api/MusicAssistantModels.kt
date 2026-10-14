/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import chromahub.rhythm.app.features.streaming.data.provider.ProviderSong
import com.google.gson.JsonObject

/** Preserve provider-qualified identities; local filesystem paths must never become phone paths. */
internal data class MusicAssistantTrack(
    val uri: String,
    val itemId: String,
    val provider: String,
    val title: String,
    val artists: List<String>,
    val album: String?,
    val albumUri: String?,
    val durationMs: Long,
    val available: Boolean,
    val favorite: Boolean,
    val lyrics: String?,
    val syncedLyrics: String?,
    val codec: String?,
    val sampleRate: Int?,
    val channels: Int?,
    val providerUris: Set<String> = emptySet(),
) {
    /** Shared Rhythm catalog model; audio and artwork are resolved by their own authenticated adapters. */
    fun toProviderSong() = ProviderSong(
        providerId = uri, title = title, artist = artists.joinToString(", "),
        album = album.orEmpty(), albumId = albumUri, durationMs = durationMs,
        isFavorite = favorite, codec = codec, sampleRate = sampleRate, channels = channels,
    )

    companion object {
        fun fromJson(value: JsonObject): MusicAssistantTrack {
            val metadata = value.get("metadata")?.takeIf { it.isJsonObject }?.asJsonObject
            val album = value.get("album")?.takeIf { it.isJsonObject }?.asJsonObject
            val mappings = value.get("provider_mappings")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { it.takeIf { it.isJsonObject }?.asJsonObject }.orEmpty()
            val format = mappings.firstOrNull { it.boolean("available") != false }
                ?.get("audio_format")?.takeIf { it.isJsonObject }?.asJsonObject
            val duration = value.number("duration")?.coerceAtLeast(0.0) ?: 0.0
            return MusicAssistantTrack(
                uri = value.requiredText("uri"), itemId = value.requiredText("item_id"),
                provider = value.requiredText("provider"), title = value.requiredText("name"),
                artists = value.get("artists")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.takeIf { it.isJsonObject }?.asJsonObject?.text("name") }.orEmpty(),
                album = album?.text("name"), albumUri = album?.text("uri"),
                durationMs = (duration * 1000).coerceAtMost(Long.MAX_VALUE.toDouble()).toLong(),
                available = value.boolean("available") ?: (mappings.isEmpty() || mappings.any { it.boolean("available") != false }),
                favorite = value.boolean("favorite") ?: false,
                lyrics = metadata?.text("lyrics"), syncedLyrics = metadata?.text("lrc_lyrics"),
                codec = format?.text("content_type"), sampleRate = format?.number("sample_rate")?.toInt(),
                channels = format?.number("channels")?.toInt(),
                providerUris = mappings.flatMap { mapping ->
                    val id = mapping.text("item_id") ?: return@flatMap emptyList()
                    listOfNotNull(mapping.text("provider_instance"), mapping.text("provider_domain"))
                        .map { provider -> "$provider://track/$id" }
                }.toSet(),
            )
        }
    }
}

/** Queue identity is an occurrence, so two plays of the same track remain distinct. */
internal data class MusicAssistantQueueItem(
    val occurrenceId: String,
    val name: String,
    val track: MusicAssistantTrack?,
) {
    companion object {
        fun fromJson(value: JsonObject) = MusicAssistantQueueItem(
            occurrenceId = value.requiredText("queue_item_id"), name = value.text("name").orEmpty(),
            track = value.get("media_item")?.takeIf { it.isJsonObject }?.asJsonObject
                ?.takeIf { it.text("media_type") == "track" }?.let(MusicAssistantTrack::fromJson),
        )
    }
}

internal data class MusicAssistantQueue(
    val id: String,
    val state: String,
    val currentOccurrenceId: String?,
    val currentIndex: Int?,
    val itemCount: Int,
    val elapsedSeconds: Double,
    val shuffle: Boolean,
    val repeat: String,
    /** Stop-backed pause and a non-playing transfer keep their position here, not elapsed_time. */
    val resumePositionSeconds: Double,
) {
    companion object {
        fun fromJson(value: JsonObject) = MusicAssistantQueue(
            id = value.requiredText("queue_id"), state = value.text("state") ?: "idle",
            currentOccurrenceId = value.get("current_item")?.takeIf { it.isJsonObject }
                ?.asJsonObject?.text("queue_item_id"),
            currentIndex = value.number("current_index")?.toInt(),
            itemCount = value.number("items")?.toInt()?.coerceAtLeast(0) ?: 0,
            elapsedSeconds = value.number("elapsed_time")?.coerceAtLeast(0.0) ?: 0.0,
            shuffle = value.boolean("shuffle_enabled") ?: false,
            repeat = value.text("repeat_mode") ?: "off",
            resumePositionSeconds = value.number("resume_pos")?.coerceAtLeast(0.0) ?: 0.0,
        )
    }
}

internal data class MusicAssistantPlayer(
    val id: String,
    val name: String,
    val available: Boolean,
    val state: String,
    val volume: Int?,
    val syncedTo: String?,
    val groupMembers: List<String>,
    val supportedFeatures: Set<String>,
) {
    companion object {
        fun fromJson(value: JsonObject) = MusicAssistantPlayer(
            id = value.requiredText("player_id"), name = value.text("display_name") ?: value.text("name").orEmpty(),
            available = value.boolean("available") ?: false, state = value.text("state") ?: "idle",
            volume = value.number("volume_level")?.toInt()?.coerceIn(0, 100),
            syncedTo = value.text("synced_to"), groupMembers =
                if (value.has("group_members")) value.strings("group_members") else value.strings("group_childs"),
            supportedFeatures = value.strings("supported_features").toSet(),
        )
    }
}

private fun JsonObject.strings(key: String): List<String> = get(key)?.takeIf { it.isJsonArray }
    ?.asJsonArray?.mapNotNull { it.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString }.orEmpty()
