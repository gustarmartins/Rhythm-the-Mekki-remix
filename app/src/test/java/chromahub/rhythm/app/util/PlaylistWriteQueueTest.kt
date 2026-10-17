/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.util

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlaylistWriteQueueTest {
    @Test fun clearingCallerDoesNotCancelItsSaveAndReopeningWaitsForIt() = runBlocking {
        withTimeout(5000) {
            val processJob = SupervisorJob()
            val queue = PlaylistWriteQueue(CoroutineScope(coroutineContext + processJob))
            try {
                val releaseDisk = CompletableDeferred<Unit>()
                val started = CompletableDeferred<Unit>()
                var disk = "old"
                val closingUi = launch {
                    queue.enqueue { started.complete(Unit); releaseDisk.await(); disk = "saved" }.await()
                }
                started.await()
                closingUi.cancelAndJoin()
                val reopenedUi = async { queue.awaitPending(); disk }
                yield()
                assertFalse(reopenedUi.isCompleted)
                releaseDisk.complete(Unit)
                assertEquals("saved", reopenedUi.await())
            } finally { processJob.cancelAndJoin() }
        }
    }

    @Test fun laterSnapshotCannotBeOverwrittenByDelayedEarlierSave() = runBlocking {
        withTimeout(5000) {
            val queue = PlaylistWriteQueue(this)
            val releaseDisk = CompletableDeferred<Unit>()
            val writes = mutableListOf<String>()
            val older = queue.enqueue { releaseDisk.await(); writes.add("older") }
            val newer = queue.enqueue { writes.add("newer") }
            yield()
            assertTrue(writes.isEmpty())
            releaseDisk.complete(Unit)
            older.await(); newer.await()
            assertEquals(listOf("older", "newer"), writes)
        }
    }

    @Test fun aFailedSaveDoesNotPreventTheNextSnapshot() = runBlocking {
        withTimeout(5000) {
            val processJob = SupervisorJob()
            val queue = PlaylistWriteQueue(CoroutineScope(coroutineContext + processJob))
            try {
                val failed = queue.enqueue { error("disk error") }
                var saved = false
                val next = queue.enqueue { saved = true }
                assertTrue(runCatching { failed.await() }.isFailure)
                next.await()
                assertTrue(saved)
            } finally { processJob.cancelAndJoin() }
        }
    }
}
