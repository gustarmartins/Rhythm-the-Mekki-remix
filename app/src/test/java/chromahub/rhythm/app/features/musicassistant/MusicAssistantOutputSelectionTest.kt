/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MusicAssistantOutputSelectionTest {
    @Test fun choosingRemoteBeforePlaybackNeverConnectsOrMutatesPhoneQueue() = runBlocking {
        var reads = 0
        selectMusicAssistantOutput("spinners", false,
            sourceQueue = { error("Phone must not be connected to select a remote output") },
            destinationQueue = { reads++; "remote-group-queue" },
            transferQueue = { _, _ -> error("Selection must not transfer or start playback") })
        assertEquals(1, reads)
    }

    @Test fun explicitTransferUsesResolvedGroupQueuesExactlyOnce() = runBlocking {
        val transfers = mutableListOf<Pair<String, String>>()
        selectMusicAssistantOutput("group-member", true, { "source-queue" }, { "group-leader" },
            { source, target -> transfers += source to target })
        assertEquals(listOf("source-queue" to "group-leader"), transfers)
    }

    @Test fun selectingMemberOfSameQueueDoesNotTransferItToItself() = runBlocking {
        selectMusicAssistantOutput("member", true, { "leader" }, { "leader" },
            { _, _ -> error("Already observing the same group queue") })
    }

    @Test fun unavailableDestinationDoesNotFallBackToPhoneOrReplayMutation() = runBlocking {
        var destinationReads = 0
        try {
            selectMusicAssistantOutput("offline", true,
                { error("Do not connect source after failed destination") },
                { destinationReads++; throw java.io.IOException("offline") },
                { _, _ -> error("Do not transfer after failed lookup") })
            fail("Expected lookup failure")
        } catch (expected: java.io.IOException) {
            assertEquals("offline", expected.message)
        }
        assertEquals(1, destinationReads)
    }
}
