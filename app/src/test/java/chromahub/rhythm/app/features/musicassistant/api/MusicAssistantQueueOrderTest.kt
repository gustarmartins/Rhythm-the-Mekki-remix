/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant.api

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MusicAssistantQueueOrderTest {
    private fun row(id: String, uri: String, mappings: String = "[]") = MusicAssistantQueueItem.fromJson(JsonParser.parseString("""
        {"queue_item_id":"$id","media_item":{"media_type":"track","uri":"$uri","item_id":"1","provider":"library","name":"song","provider_mappings":$mappings}}
    """).asJsonObject)
    @Test fun shuffledBatchRestoresRequestOrderAndKeepsDuplicateOccurrences() {
        val a = "library://track/1"; val b = "library://track/2"
        val ordered = orderMusicAssistantAdditions(listOf(a,b,a), listOf(row("a1",a),row("a2",a),row("b",b)))
        assertEquals(listOf("a1","b","a2"), ordered.map { it.occurrenceId })
    }
    @Test fun providerItemCanResolveToItsLibraryIdentity() {
        val item = row("a", "library://track/1", """[{"provider_domain":"spotify","provider_instance":"spotify-123","item_id":"abc"}]""")
        assertEquals(listOf(item), orderMusicAssistantAdditions(listOf("spotify://track/abc"), listOf(item)))
    }
    @Test fun concurrentAppendIsNotMistakenForOurRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            orderMusicAssistantAdditions(listOf("library://track/1"), listOf(row("a","library://track/1"),row("b","library://track/2")))
        }
    }
    @Test fun sameCountDifferentTrackIsRejectedWithoutGuessingByTitle() {
        assertThrows(IllegalStateException::class.java) {
            orderMusicAssistantAdditions(listOf("library://track/1"), listOf(row("a","library://track/2")))
        }
    }
}
