package chromahub.rhythm.app.infrastructure.service

/** A playlist edit in the service's coordinate space, independent of the one-item car view. */
internal data class LegacyPlayNextQueue<T>(
    val items: List<T>,
    val history: List<Int>,
    val upcoming: List<Int>
)

internal fun <T> insertLegacyPlayNext(
    items: List<T>,
    currentIndex: Int,
    history: List<Int>,
    upcoming: List<Int>,
    added: List<T>
): LegacyPlayNextQueue<T> {
    require(currentIndex in items.indices)
    require((history + upcoming).all { it in items.indices && it != currentIndex })
    val insertionIndex = currentIndex + 1
    fun remap(index: Int) = if (index >= insertionIndex) index + added.size else index
    return LegacyPlayNextQueue(
        items = items.take(insertionIndex) + added + items.drop(insertionIndex),
        history = history.map(::remap),
        upcoming = (insertionIndex until insertionIndex + added.size).toList() + upcoming.map(::remap)
    )
}
