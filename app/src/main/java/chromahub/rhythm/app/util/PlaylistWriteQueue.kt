/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** Ordered saves owned by the process, so clearing a UI cannot cancel its final write. */
class PlaylistWriteQueue(private val scope: CoroutineScope) {
    private var pending: Deferred<Unit>? = null

    @Synchronized
    fun enqueue(save: suspend () -> Unit): Deferred<Unit> {
        val previous = pending
        return scope.async(start = CoroutineStart.LAZY) {
            // A failed earlier write must not prevent a later, complete snapshot saving.
            previous?.join()
            save()
        }.also { pending = it; it.start() }
    }

    /** A newly created UI must see the previous UI's final snapshot before loading Room. */
    suspend fun awaitPending() {
        val lastWrite = synchronized(this) { pending }
        lastWrite?.join()
    }
}
