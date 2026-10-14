/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

/** MA may shuffle an ADD batch. Match its new occurrences back to reserved caller order. */
internal fun orderMusicAssistantAdditions(
    requestedUris: List<String>,
    added: List<MusicAssistantQueueItem>,
): List<MusicAssistantQueueItem> {
    require(requestedUris.size == added.size) { "Queue changed concurrently; refresh before reordering" }
    require(added.map { it.occurrenceId }.toSet().size == added.size) { "Duplicate server occurrence IDs" }
    val remaining = added.toMutableList()
    return requestedUris.map { uri ->
        val index = remaining.indexOfFirst { it.track?.let { track -> track.uri == uri || uri in track.providerUris } == true }
        check(index >= 0) { "The server resolved a different item; refresh before reordering" }
        remaining.removeAt(index)
    }
}
