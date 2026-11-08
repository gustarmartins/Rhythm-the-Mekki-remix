/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

/** Re-pin FIFO requests after a server shuffle, checking its authoritative order after every edit. */
internal suspend fun restoreServerPlayNext(
    pending: List<String>, current: String,
    read: suspend () -> Pair<String?, List<String>>,
    move: suspend (String, Int) -> Unit
) {
    for ((offset, token) in pending.withIndex()) {
        val (playing, order) = read()
        check(playing == current) { "Playback advanced during shuffle; refresh the queue" }
        val anchor = order.indexOf(current)
        val from = order.indexOf(token)
        check(anchor >= 0 && from > anchor) { "Requested occurrence is no longer upcoming" }
        val to = anchor + 1 + offset
        if (from != to) move(token, to - from)
    }
    val (playing, order) = read()
    check(playing == current && order.drop(order.indexOf(current) + 1).take(pending.size) == pending) {
        "Server did not retain Play Next order; refresh the queue"
    }
}
