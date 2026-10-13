package chromahub.rhythm.app.util

import org.junit.Assert.*
import org.junit.Test

class DeviceResumePolicyTest {
    @Test fun initialConnectedRouteNeverStartsPlayback() {
        assertFalse(DeviceResumePolicy("qcy").routeChanged("qcy"))
    }
    @Test fun carSelectionWhileHeadphonesRemainConnectedIsANewRoute() {
        val policy = DeviceResumePolicy("qcy")
        assertTrue(policy.routeChanged("car"))
        assertFalse(policy.routeChanged("car"))
    }
    @Test fun brandNewConnectionDoesNotRequireAnEarlierDisconnect() {
        assertTrue(DeviceResumePolicy(null).routeChanged("car"))
    }
    @Test fun disconnectDoesNotPlayOnSpeakerButReconnectCanResume() {
        val policy = DeviceResumePolicy("qcy")
        assertFalse(policy.routeChanged(null))
        assertTrue(policy.routeChanged("qcy"))
    }
    @Test fun disabledRouteEventIsConsumedAndCannotReplayWhenEnabled() {
        val policy = DeviceResumePolicy("qcy")
        policy.routeChanged("car") // caller has Auto Resume off
        assertFalse(policy.routeChanged("car"))
    }
    @Test fun missingEarlierFileDoesNotMovePlaybackToAnotherSong() {
        val restored = restorePlaybackOccurrences(listOf("gone", "a", "b"), 2, 12500,
            mapOf("a" to "A", "b" to "B"))!!
        assertEquals(listOf("A", "B"), restored.items)
        assertEquals(1, restored.index)
        assertEquals(12500L, restored.positionMs)
    }
    @Test fun duplicateCurrentOccurrenceAndQueueOrderSurvive() {
        val restored = restorePlaybackOccurrences(listOf("a", "b", "a", "c"), 2, 500,
            mapOf("a" to "A", "b" to "B", "c" to "C"))!!
        assertEquals(listOf("A", "B", "A", "C"), restored.items)
        assertEquals(2, restored.index)
        assertEquals(500L, restored.positionMs)
    }
    @Test fun missingCurrentFileResetsPositionOfNextAvailableSong() {
        val restored = restorePlaybackOccurrences(listOf("a", "gone", "b"), 1, 90000,
            mapOf("a" to "A", "b" to "B"))!!
        assertEquals(1, restored.index)
        assertEquals(0L, restored.positionMs)
        assertNull(restorePlaybackOccurrences(listOf("gone"), 0, 0, emptyMap<String, String>()))
    }
}
