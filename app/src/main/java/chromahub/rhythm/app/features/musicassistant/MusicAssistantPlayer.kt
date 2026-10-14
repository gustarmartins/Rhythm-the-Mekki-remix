/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import chromahub.rhythm.app.features.musicassistant.api.*
import chromahub.rhythm.app.infrastructure.service.PlayNextCommand
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.music_assistant.sendspin.SendspinPlayer
import io.music_assistant.sendspin.android.AndroidDecoderFactory
import io.music_assistant.sendspin.android.AudioTrackSink
import io.music_assistant.sendspin.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors

/** Server-owned queue exposed through Rhythm's existing Media3 session and player UI. */
@UnstableApi
internal class MusicAssistantPlayer(
    context: Context,
    private val foreground: () -> Unit,
    private val audioSession: (Int, Boolean) -> Unit,
) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val client = MusicAssistantProviderClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val operations = Mutex()
    private val config = MutableStateFlow<LocalPlayerConfig?>(null)
    private val dispatcher = Executors.newSingleThreadExecutor { action -> Thread(action, "Rhythm-Sendspin").apply { priority = Thread.MAX_PRIORITY } }.asCoroutineDispatcher()
    private val http = HttpClient(OkHttp) { install(WebSockets) }
    private val clock = MonotonicClock { SystemClock.elapsedRealtimeNanos() / 1000 }
    @Volatile var sessionId: Int = 0
        private set
    private val sink = AudioTrackSink(context, clock, onSessionOpened = {
        sessionId = it; audioSession(it, true)
    }, onSessionClosing = {
        if (sessionId == it) sessionId = 0; audioSession(it, false)
    })
    private val receiver = SendspinPlayer(config, SendspinDeps(
        sink, AndroidDecoderFactory(), object : SendspinKeyStore {
            override fun read(key: String) = runCatching { client.account.read("sendspin.$key") }.getOrNull()
            override fun write(key: String, value: ByteArray) = client.account.write("sendspin.$key", value)
            override fun delete(key: String) = client.account.delete("sendspin.$key")
        }, http, MutableStateFlow(true), { token -> client.command("sendspin/pair_web_player", "pairing_token" to token); Unit }, dispatcher, clock
    ), scope)
    private var queue: MusicAssistantQueue? = null
    private var rows = emptyList<MusicAssistantQueueItem>()
    private var lastPositionAt = SystemClock.elapsedRealtime()
    private var error: PlaybackException? = null
    private var released = false
    private var targetId: String? = null
    private val pendingManual = mutableListOf<String>()
    private val metadata = mutableMapOf<String, MediaItem>()
    private var desiredPlaying = false

    init {
        scope.launch {
            receiver.state.collect { state ->
                nativeStatus.value = when (state) {
                    is PlayerState.Connected -> "Connected to ${state.serverName}"
                    is PlayerState.Failed -> "Music Assistant: ${state.cause}"
                    is PlayerState.Reconnecting -> "Reconnecting to Music Assistant"
                    is PlayerState.Connecting -> "Connecting to Music Assistant"
                    else -> "Music Assistant stopped"
                }
                if (state is PlayerState.Failed) {
                    error = PlaybackException(nativeStatus.value, null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                    invalidateState()
                }
            }
        }
        scope.launch {
            receiver.events.collect { event ->
                if (event is PlayerEvent.PlaybackStopped && event.cause in listOf(StopCause.FocusLost, StopCause.SinkFailed)) {
                    submit { queue?.let { client.api().playerAction(target(), MusicAssistantApi.PlayerAction.PAUSE) } }
                }
                if (event is PlayerEvent.PlaybackStarted) android.util.Log.i("RhythmMA", "Native PCM playback started; session=$sessionId")
            }
        }
        scope.launch {
            while (isActive) {
                if (targetId != null || receiver.state.value is PlayerState.Connected) {
                    try { operations.withLock { refresh() } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { nativeStatus.value = "Music Assistant unavailable; reconnecting" }
                }
                delay(1000)
            }
        }
    }
    private fun connect() {
        if (config.value != null) return
        foreground()
        val server = client.account.server
        val endpoint = server.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "sendspin"
        config.value = LocalPlayerConfig(Endpoint.WebSocket(endpoint, client.account.token), "Rhythm • ${android.os.Build.MODEL}",
            listOf(AudioCodec.FLAC, AudioCodec.PCM, AudioCodec.OPUS), 4 * 1024 * 1024, 0)
    }
    private suspend fun target(): String {
        targetId?.let { return it }
        connect()
        val state = withTimeout(30000) { receiver.state.first { it is PlayerState.Connected || it is PlayerState.Failed } }
        check(state is PlayerState.Connected) { "Music Assistant audio connection failed" }
        return state.playerId
    }
    private suspend fun queueId() = client.api().activeQueue(target())?.id ?: error("Music Assistant player has no queue")
    private suspend fun readItems(id: String, count: Int): List<MusicAssistantQueueItem> {
        val result = ArrayList<MusicAssistantQueueItem>()
        while (result.size < count) {
            val page = client.api().queueItems(id, result.size, 500)
            result.addAll(page)
            if (page.size < 500) break
        }
        return result
    }
    private suspend fun refresh() {
        val id = targetId ?: (receiver.state.value as? PlayerState.Connected)?.playerId ?: return
        val current = client.api().activeQueue(id) ?: return
        val items = readItems(current.id, current.itemCount)
        // Do not publish a torn queue when another client changes it during pagination.
        val verified = client.api().activeQueue(id) ?: return
        if (verified.itemCount != items.size) return
        queue = verified; rows = items; lastPositionAt = SystemClock.elapsedRealtime()
        desiredPlaying = verified.state == "playing"
        val currentIndex = items.indexOfFirst { it.occurrenceId == verified.currentOccurrenceId }
        val upcoming = items.drop((currentIndex + 1).coerceAtLeast(0)).map { it.occurrenceId }.toSet()
        pendingManual.retainAll(upcoming)
        error = null
        invalidateState()
    }
    private fun mediaItem(row: MusicAssistantQueueItem): MediaItem {
        val track = row.track
        val original = track?.uri?.let(metadata::get)
        val extras = Bundle(original?.mediaMetadata?.extras ?: Bundle.EMPTY).apply { putString(PlayNextCommand.ENTRY_TOKEN, row.occurrenceId) }
        return (original?.buildUpon() ?: MediaItem.Builder().setMediaId("MUSIC_ASSISTANT::${track?.uri ?: row.occurrenceId}")
            .setUri(track?.let { client.buildStreamUrl(it.uri) }))
            .setMediaMetadata((original?.mediaMetadata?.buildUpon() ?: MediaMetadata.Builder()
                .setTitle(track?.title ?: row.name).setArtist(track?.artists?.joinToString(", ")).setAlbumTitle(track?.album)
                .setDurationMs(track?.durationMs)).setExtras(extras).setIsPlayable(true).build()).build()
    }
    override fun getState(): State {
        val q = queue
        val playing = q?.state == "playing"
        val index = rows.indexOfFirst { it.occurrenceId == q?.currentOccurrenceId }.takeIf { it >= 0 } ?: 0
        val position = ((if (playing) q?.elapsedSeconds else q?.resumePositionSeconds?.takeIf { it > 0 } ?: q?.elapsedSeconds) ?: 0.0) * 1000
        return State.Builder().setAvailableCommands(Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_STOP, COMMAND_RELEASE,
            COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS, COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE,
            COMMAND_GET_METADATA, COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SEEK_TO_MEDIA_ITEM,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SET_REPEAT_MODE, COMMAND_SET_SHUFFLE_MODE, COMMAND_GET_AUDIO_ATTRIBUTES, COMMAND_GET_VOLUME
        ).build()).setPlaylist(rows.map { row -> MediaItemData.Builder(row.occurrenceId).setMediaItem(mediaItem(row))
            .setDurationUs(row.track?.durationMs?.times(1000) ?: C.TIME_UNSET).setIsSeekable(true).build() })
            .setCurrentMediaItemIndex(if (rows.isEmpty()) C.INDEX_UNSET else index)
            .setPlayWhenReady(playing, PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setPlaybackState(if (rows.isEmpty()) STATE_IDLE else STATE_READY)
            .setContentPositionMs(PositionSupplier { position.toLong() + if (playing) SystemClock.elapsedRealtime() - lastPositionAt else 0 })
            .setRepeatMode(when (q?.repeat) { "one" -> REPEAT_MODE_ONE; "all" -> REPEAT_MODE_ALL; else -> REPEAT_MODE_OFF })
            .setShuffleModeEnabled(q?.shuffle ?: false).setAudioSessionId(sessionId).setPlayerError(error).build()
    }
    private fun submit(block: suspend () -> Unit): ListenableFuture<Void> {
        val future = SettableFuture.create<Void>()
        scope.launch {
            try {
                operations.withLock { block(); refresh() }
                future.set(null)
            } catch (cancelled: CancellationException) { future.cancel(false); throw cancelled }
            catch (failure: Exception) {
                // A timeout may have committed a mutation. Reconcile, never blindly replay it.
                runCatching { operations.withLock { refresh() } }
                error = PlaybackException("Music Assistant command failed. Refresh the server connection.", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
                invalidateState(); future.setException(failure)
            }
        }
        return future
    }
    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        require(mediaItems.all(::isMusicAssistant)) { "Local and server queues cannot be mixed" }
        mediaItems.forEach { metadata[uri(it)] = it }
        return submit {
            pendingManual.clear()
            val id = queueId()
            if (mediaItems.isEmpty()) client.command("player_queues/clear", "queue_id" to id)
            else {
                foreground()
                client.command("player_queues/play_media", "queue_id" to id, "media" to mediaItems.map(::uri), "option" to "replace",
                    "start_item" to uri(mediaItems[startIndex.coerceIn(0, mediaItems.lastIndex)]))
                if (startPositionMs > 0) client.api().seek(id, (startPositionMs / 1000).toInt())
            }
        }
    }
    override fun handleSetPlayWhenReady(playWhenReady: Boolean) = submit {
        desiredPlaying = playWhenReady
        if (playWhenReady) foreground()
        client.api().playerAction(target(), if (playWhenReady) MusicAssistantApi.PlayerAction.PLAY else MusicAssistantApi.PlayerAction.PAUSE)
    }
    override fun handlePrepare() = submit { connect() }
    override fun handleStop() = submit { client.api().playerAction(target(), MusicAssistantApi.PlayerAction.STOP) }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int) = submit {
        val qid = queueId()
        val wasPlaying = queue?.state == "playing"
        when (seekCommand) {
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> client.command("player_queues/next", "queue_id" to qid)
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> client.command("player_queues/previous", "queue_id" to qid)
            else -> if (mediaItemIndex == currentMediaItemIndex) client.api().seek(qid, (positionMs.coerceAtLeast(0) / 1000).toInt())
                else client.command("player_queues/play_index", "queue_id" to qid, "index" to rows[mediaItemIndex].occurrenceId, "seek_position" to (positionMs.coerceAtLeast(0) / 1000))
        }
        if (!wasPlaying) client.api().playerAction(target(), MusicAssistantApi.PlayerAction.PAUSE)
    }
    override fun handleSetRepeatMode(repeatMode: Int) = submit { client.command("player_queues/repeat", "queue_id" to queueId(), "repeat_mode" to when (repeatMode) { REPEAT_MODE_ONE -> "one"; REPEAT_MODE_ALL -> "all"; else -> "off" }); Unit }
    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean) = submit { client.command("player_queues/shuffle", "queue_id" to queueId(), "shuffle_enabled" to shuffleModeEnabled); Unit }
    fun enqueueNext(items: List<MediaItem>): ListenableFuture<Void> = submit {
        refresh()
        val current = rows.indexOfFirst { it.occurrenceId == queue?.currentOccurrenceId }
        val tail = rows.indexOfLast { it.occurrenceId in pendingManual }.coerceAtLeast(current)
        addAt(items, (tail + 1).coerceAtLeast(0), manual = true)
    }
    override fun handleAddMediaItems(index: Int, mediaItems: MutableList<MediaItem>) = submit { addAt(mediaItems, index, false) }
    private suspend fun addAt(items: List<MediaItem>, index: Int, manual: Boolean) {
        require(items.isNotEmpty() && items.all(::isMusicAssistant)) { "Only Music Assistant songs can be added to this server queue" }
        val id = queueId(); val before = readItems(id, client.api().activeQueue(target())?.itemCount ?: 0)
        client.api().append(id, items.map(::uri))
        val after = readItems(id, client.api().activeQueue(target())?.itemCount ?: 0)
        val added = after.filter { row -> before.none { it.occurrenceId == row.occurrenceId } }
        check(added.size == items.size) { "Queue changed concurrently; refresh before reordering" }
        items.forEach { metadata[uri(it)] = it }
        for ((offset, row) in added.withIndex()) {
            val now = readItems(id, after.size)
            val from = now.indexOfFirst { it.occurrenceId == row.occurrenceId }
            val destination = (index + offset).coerceAtMost(now.lastIndex)
            if (from != destination) client.command("player_queues/move_item", "queue_id" to id, "queue_item_id" to row.occurrenceId, "pos_shift" to destination - from)
        }
        val verified = readItems(id, after.size)
        check(verified.drop(index).take(added.size).map { it.occurrenceId } == added.map { it.occurrenceId }) { "Server has buffered the next song; refresh queue order" }
        if (manual) pendingManual.addAll(added.map { it.occurrenceId })
    }
    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        val ids = rows.subList(fromIndex, toIndex).map { it.occurrenceId }
        return submit { val id = queueId(); ids.forEach { client.command("player_queues/delete_item", "queue_id" to id, "item_id_or_index" to it) } }
    }
    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        val ids = rows.subList(fromIndex, toIndex).map { it.occurrenceId }
        return submit {
            val id = queueId()
            val order = if (newIndex < fromIndex) ids.withIndex().toList() else ids.withIndex().toList().reversed()
            for ((offset, item) in order) {
                val current = readItems(id, queue?.itemCount ?: 0).indexOfFirst { it.occurrenceId == item }
                val shift = newIndex + offset - current
                if (shift != 0) client.command("player_queues/move_item", "queue_id" to id, "queue_item_id" to item, "pos_shift" to shift)
            }
        }
    }
    override fun handleReplaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>): ListenableFuture<*> {
        // Lyric/title updates must not replace or restart a server occurrence.
        if (toIndex - fromIndex == mediaItems.size && rows.subList(fromIndex, toIndex).map { it.track?.uri } == mediaItems.map(::uri)) {
            mediaItems.forEach { metadata[uri(it)] = it }; invalidateState(); return Futures.immediateVoidFuture()
        }
        return Futures.immediateFailedFuture<Void>(UnsupportedOperationException("Replace server songs with an explicit Play action"))
    }
    override fun handleRelease(): ListenableFuture<*> {
        if (!released) {
            released = true; config.value = null
            scope.launch { delay(100); scope.cancel(); http.close(); dispatcher.close() }
        }
        return Futures.immediateVoidFuture()
    }
    companion object {
        val nativeStatus = MutableStateFlow("Music Assistant stopped")
        fun isMusicAssistant(item: MediaItem) = item.mediaId.startsWith("MUSIC_ASSISTANT::") || item.localConfiguration?.uri?.scheme == "musicassistant"
        private fun uri(item: MediaItem): String = if (item.mediaId.startsWith("MUSIC_ASSISTANT::")) item.mediaId.removePrefix("MUSIC_ASSISTANT::")
            else Uri.decode(item.localConfiguration?.uri?.encodedPath?.removePrefix("/"))
    }
}
