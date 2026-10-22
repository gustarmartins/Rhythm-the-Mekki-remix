package chromahub.rhythm.app.infrastructure.service

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class MetadataTransitionPolicyTest {
    @Test fun metadataAutoCallbackKeepsCurrentOccurrence() {
        assertTrue(isUnchangedOccurrenceAutoTransition(
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, "song", "entry-1", "song", "entry-1"))
    }

    @Test fun duplicateSongsRemainSeparateOccurrences() {
        assertFalse(isUnchangedOccurrenceAutoTransition(
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, "song", "entry-2", "song", "entry-1"))
    }

    @Test fun realRepeatsAndExplicitSelectionsAreNeverFiltered() {
        for (reason in listOf(Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)) {
            assertFalse(isUnchangedOccurrenceAutoTransition(reason, "song", "entry", "song", "entry"))
        }
    }

    @Test fun firstObservationAndUntokenizedQueuesRemainAuthoritative() {
        assertFalse(isUnchangedOccurrenceAutoTransition(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            "song", "entry", null, null))
        assertFalse(isUnchangedOccurrenceAutoTransition(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            "song", null, "song", null))
        assertFalse(isUnchangedOccurrenceAutoTransition(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            "song", "", "song", ""))
        assertFalse(isUnchangedOccurrenceAutoTransition(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            "other", "entry", "song", "entry"))
    }
}
