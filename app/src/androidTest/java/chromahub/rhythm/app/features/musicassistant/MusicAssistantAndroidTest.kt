/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.session.SessionCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.features.streaming.data.repository.StreamingMusicRepositoryImpl
import chromahub.rhythm.app.features.streaming.data.repository.StreamingServiceSessionRepository
import chromahub.rhythm.app.infrastructure.service.MediaPlaybackService
import chromahub.rhythm.app.infrastructure.service.PlayNextCommand
import chromahub.rhythm.app.shared.data.model.AppSettings
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** Explicit real-server test. Credentials are supplied privately to the debug app, never to CI. */
@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicAssistantAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun <T> main(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }
    private fun waitFor(label: String, timeoutSeconds: Int = 90, condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds.toLong())
        while (System.nanoTime() < end) {
            if (main(condition)) return
            Thread.sleep(250)
        }
        fail("Timed out: $label; ${MusicAssistantPlayer.nativeStatus.value}")
    }
    @Test fun nativeLibraryAndMediaSessionPlayback(): Unit = runBlocking {
        val input = File(context.filesDir, "ma-test-account.json")
        assumeTrue("Supply a private real-server account to run this test", input.isFile)
        val credentials = JsonParser.parseString(input.readText()).asJsonObject
        val server = credentials.get("server").asString
        val username = credentials.get("username").asString
        val client = MusicAssistantProviderClient(context)
        client.login(server, username, credentials.get("password").asString).getOrThrow()
        input.delete()
        val prefs = context.getSharedPreferences("music_assistant_native", 0)
        assertFalse("Password must not be persisted", prefs.all.values.any { it.toString() == credentials.get("password").asString })
        assertFalse("Token must be encrypted", prefs.all.values.any { it.toString() == client.account.token })
        main {
            StreamingServiceSessionRepository(context).connect("MUSIC_ASSISTANT", server, username)
            AppSettings.getInstance(context).setStreamingService("MUSIC_ASSISTANT")
            AppSettings.getInstance(context).setAppMode("STREAMING")
        }
        val repository = StreamingMusicRepositoryImpl(context)
        val songs = repository.syncCatalog(3000)
        assertTrue("Real music library must be visible", songs.size > 100)
        val tracks = client.fetchLibrarySongs(3000).getOrThrow()
        val flac = tracks.first { it.codec?.lowercase() == "flac" }
        val mp3 = tracks.first { it.codec?.lowercase() in listOf("mp3", "mpeg") }
        fun item(song: chromahub.rhythm.app.features.streaming.data.provider.ProviderSong) = MediaItem.Builder()
            .setMediaId("MUSIC_ASSISTANT::${song.providerId}").setUri(client.buildStreamUrl(song.providerId))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album)
                .setDurationMs(song.durationMs).setArtworkUri(song.artworkUrl?.let(android.net.Uri::parse)).build()).build()
        val future = main { MediaController.Builder(context, SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))).buildAsync() }
        val controller = future.get(30, TimeUnit.SECONDS)
        try {
            main { controller.setMediaItems(listOf(item(flac))); controller.prepare(); controller.play() }
            waitFor("FLAC playing in Rhythm") { controller.isPlaying && (MusicAssistantPlayer.active.value?.sessionId ?: 0) > 0 && controller.currentPosition > 2000 }
            assertTrue(main { controller.currentMediaItem?.mediaId == "MUSIC_ASSISTANT::${flac.providerId}" })
            main {
                AppSettings.getInstance(context).setBluetoothLyricsEnabled(true)
                AppSettings.getInstance(context).setBluetoothLyricsTextMode(chromahub.rhythm.app.shared.data.model.BluetoothLyricsTextMode.ORIGINAL)
            }
            val lyricData = Bundle().apply {
                putString("song_id", "MUSIC_ASSISTANT::${flac.providerId}")
                putStringArrayList("lyric_texts", arrayListOf("Rhythm lyric test"))
                putStringArrayList("lyric_romanizations", arrayListOf("Rhythm lyric test"))
                putLongArray("lyric_timestamps", longArrayOf(0))
                putString("lyrics_source", "Android regression fixture")
            }
            main { controller.sendCustomCommand(SessionCommand("UPDATE_LYRICS_DATA", Bundle.EMPTY), lyricData) }.get(10, TimeUnit.SECONDS)
            val positionBeforeLyrics = main { controller.currentPosition }
            waitFor("native lyric metadata") { controller.mediaMetadata.title.toString().contains("Rhythm lyric test") || controller.mediaMetadata.artist.toString().contains("Rhythm lyric test") }
            assertTrue("Lyric metadata must not rewind audio", main { controller.currentPosition } >= positionBeforeLyrics - 500)
            assertEquals("Lyric metadata must not replace the queue", 1, main { controller.mediaItemCount })
            main { controller.pause() }
            waitFor("pause") { !controller.playWhenReady }
            val paused = main { controller.currentPosition }
            Thread.sleep(1300)
            val afterPause = main { controller.currentPosition }
            assertTrue("Paused position must stay still: $paused -> $afterPause", kotlin.math.abs(afterPause - paused) < 1000)
            val request = Bundle().apply { putParcelableArrayList(PlayNextCommand.ITEMS, arrayListOf(item(mp3).toBundleIncludeLocalConfiguration(), item(flac).toBundleIncludeLocalConfiguration())) }
            val added = main { controller.sendCustomCommand(SessionCommand(PlayNextCommand.ACTION, Bundle.EMPTY), request) }.get(90, TimeUnit.SECONDS)
            assertEquals("Native FIFO server enqueue acknowledged", 0, added.resultCode)
            waitFor("queue readback") { controller.mediaItemCount == 3 }
            assertFalse("Adding songs must keep pause", main { controller.playWhenReady })
            assertEquals("FIFO keeps request order", listOf(flac.providerId, mp3.providerId, flac.providerId),
                main { (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaId.removePrefix("MUSIC_ASSISTANT::") } })
            val ids = main { (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaMetadata.extras?.getString(PlayNextCommand.ENTRY_TOKEN) } }
            assertEquals("Duplicate tracks retain occurrence identities", 3, ids.toSet().size)
            main { controller.seekToNextMediaItem(); controller.play() }
            waitFor("MP3 playing in Rhythm") { controller.isPlaying && controller.currentMediaItem?.mediaId == "MUSIC_ASSISTANT::${mp3.providerId}" && controller.currentPosition > 1500 }
            main { controller.seekTo(12000) }
            waitFor("seek acknowledgement") { controller.currentPosition in 11500..22000 }
            main { controller.pause() }
            waitFor("final pause") { !controller.playWhenReady }
            android.util.Log.i("RhythmMA-Test", "PASS native catalog=${songs.size}, FLAC, MP3, pause, FIFO, duplicates, seek")
            Unit
        } finally {
            main { controller.pause(); controller.release() }
        }
    }
}
