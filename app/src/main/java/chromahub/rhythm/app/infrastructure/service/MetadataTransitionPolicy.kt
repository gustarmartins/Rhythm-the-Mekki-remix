package chromahub.rhythm.app.infrastructure.service

import androidx.media3.common.Player

/**
 * AUTO moves to a different occurrence; repeating the same occurrence uses REPEAT.
 * Some metadata replacements report AUTO without changing the occurrence token.
 * Do not turn those callbacks into new listening events or reset a chosen playback rate.
 * Missing tokens are deliberately not deduplicated: duplicate song IDs are valid queue entries.
 */
internal fun isUnchangedOccurrenceAutoTransition(
    reason: Int,
    mediaId: String?,
    entryToken: String?,
    previousMediaId: String?,
    previousEntryToken: String?
): Boolean = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
    !mediaId.isNullOrBlank() && mediaId == previousMediaId &&
    !entryToken.isNullOrBlank() && entryToken == previousEntryToken
