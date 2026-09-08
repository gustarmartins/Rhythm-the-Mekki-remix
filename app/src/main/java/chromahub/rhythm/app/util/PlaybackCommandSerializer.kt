/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Serializes playback commands to prevent race conditions in queue mutations.
 * Uses a mutex to ensure only one command executes at a time.
 */
class PlaybackCommandSerializer(
    private val coroutineContext: CoroutineContext = Dispatchers.Main
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(coroutineContext)

    /**
     * Execute a command with serialization.
     * Commands are executed in the order they are submitted.
     */
    fun <T> executeCommand(block: suspend () -> T): Job {
        return scope.launch {
            mutex.withLock {
                block()
            }
        }
    }

    /**
     * Execute a command synchronously with serialization.
     * This will block until the command completes.
     */
    suspend fun <T> executeCommandSync(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }
}
