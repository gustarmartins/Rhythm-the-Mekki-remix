/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicAssistantPositionTest {
    private fun queue(state: String, elapsed: Double = 10.0, resume: Double = 0.0, speed: Double = 1.0) =
        MusicAssistantQueue.fromJson(JsonParser.parseString("""{"queue_id":"q","state":"$state","elapsed_time":$elapsed,"elapsed_time_last_updated":1000,"resume_pos":$resume,"playback_speed":$speed}""").asJsonObject)
    @Test fun repeatedSnapshotKeepsAdvancingOnServerClock() {
        val q = queue("playing")
        assertEquals(13.0, q.positionSecondsAt(1003.0), 0.001)
        assertEquals(14.0, q.positionSecondsAt(1004.0), 0.001)
        assertEquals(16.0, queue("playing", speed = 1.5).positionSecondsAt(1004.0), 0.001)
    }
    @Test fun stopBackedPauseUsesResumeAndDoesNotAdvance() {
        assertEquals(42.0, queue("idle", elapsed = 0.0, resume = 42.0).positionSecondsAt(9000.0), 0.001)
    }
    @Test fun realPauseRetainsFractionalElapsedPosition() {
        assertEquals(10.5, queue("paused", elapsed = 10.5, resume = 10.0).positionSecondsAt(9000.0), 0.001)
    }
    @Test fun futureServerTimestampCannotRunBackwards() {
        assertEquals(10.0, queue("playing").positionSecondsAt(999.0), 0.001)
    }
}
