/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming.data.repository

import kotlinx.coroutines.CancellationException

/** An old network result cannot publish after logout or into a replacement account. */
internal class CatalogSessionGeneration {
    private var generation = 0L
    private var accepting = true
    private var service: String? = null
    /** The observer and first request may notice a service switch in either order. */
    @Synchronized fun select(serviceId: String, clear: () -> Unit): Long {
        if (service != null && service != serviceId) {
            generation++
            accepting = true
            clear()
        }
        service = serviceId
        return generation
    }
    @Synchronized fun current(): Long = generation
    @Synchronized fun isCurrent(expected: Long): Boolean = accepting && expected == generation
    @Synchronized fun requireCurrent(expected: Long, allowClosed: Boolean = false) {
        if (expected != generation || (!accepting && !allowClosed)) throw CancellationException("Streaming account changed")
    }
    @Synchronized fun close(clear: () -> Unit) {
        generation++
        accepting = false
        clear()
    }
    @Synchronized fun open() { generation++; accepting = true }
    @Synchronized fun <T> publish(expected: Long, allowClosed: Boolean = false, block: () -> T): T {
        requireCurrent(expected, allowClosed)
        return block()
    }
}
