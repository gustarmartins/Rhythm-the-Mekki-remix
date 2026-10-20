/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

/** Selecting observes the destination; only an explicit transfer reads or moves the source. */
internal suspend fun selectMusicAssistantOutput(
    destinationId: String,
    transfer: Boolean,
    sourceQueue: suspend () -> String,
    destinationQueue: suspend () -> String,
    transferQueue: suspend (String, String) -> Unit,
) {
    require(destinationId.isNotBlank())
    val target = destinationQueue()
    if (transfer) {
        val source = sourceQueue()
        if (source != target) transferQueue(source, target)
    }
}
