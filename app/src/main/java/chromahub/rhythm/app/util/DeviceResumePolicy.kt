package chromahub.rhythm.app.util

/** Observe selected outputs, not the set of paired or connected Bluetooth devices. */
internal class DeviceResumePolicy(initialRoute: String?) {
    private var previousRoute = initialRoute

    fun routeChanged(route: String?): Boolean {
        val changed = route != previousRoute
        previousRoute = route
        return changed && route != null
    }
}

internal data class RestoredPlayback<T>(val items: List<T>, val index: Int, val positionMs: Long)

/** Preserve duplicate occurrences and remap the saved index when earlier files disappeared. */
internal fun <T> restorePlaybackOccurrences(
    ids: List<String>, index: Int, positionMs: Long, available: Map<String, T>
): RestoredPlayback<T>? {
    val kept = ids.mapIndexedNotNull { i, id -> available[id]?.let { i to it } }
    if (kept.isEmpty()) return null
    val selected = kept.indexOfFirst { it.first >= index }.takeIf { it >= 0 } ?: kept.lastIndex
    return RestoredPlayback(kept.map { it.second }, selected,
        if (kept[selected].first == index) positionMs.coerceAtLeast(0) else 0)
}
