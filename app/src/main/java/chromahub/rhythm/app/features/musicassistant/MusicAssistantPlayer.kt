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
    private val playerJob = SupervisorJob()
    private val scope = CoroutineScope(playerJob + Dispatchers.Main.immediate)
    private val operations = Mutex()
    private val config = MutableStateFlow<LocalPlayerConfig?>(null)
    private val dispatcher = Executors.newSingleThreadExecutor { action -> Thread(action, "Rhythm-Sendspin").apply { priority = Thread.MAX_PRIORITY } }.asCoroutineDispatcher()
    private val http = musicAssistantSocketClient()
    private val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
        .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Rhythm:Sendspin").apply { setReferenceCounted(false) }
    private val clock = MonotonicClock { SystemClock.elapsedRealtimeNanos() / 1000 }
    @Volatile var sessionId: Int = 0
        private set
    private val sink = MusicAssistantAudioSink(context, AudioTrackSink(context, clock, beforeOpen = foreground, onSessionOpened = {
        wakeLock.acquire(10 * 60 * 1000L)
        sessionId = it; audioSession(it, true)
    }, onSessionClosing = {
        if (sessionId == it) sessionId = 0
        try { audioSession(it, false) } finally { if (wakeLock.isHeld) wakeLock.release() }
    }))
    private val receiver = SendspinPlayer(config, SendspinDeps(
        sink, AndroidDecoderFactory(), object : SendspinKeyStore {
            override fun read(key: String) = runCatching { client.account.read("sendspin.$key") }.getOrNull()
            override fun write(key: String, value: ByteArray) = client.account.write("sendspin.$key", value)
            override fun delete(key: String) = client.account.delete("sendspin.$key")
        }, http, MutableStateFlow(true), { token -> client.command("sendspin/pair_web_player", "pairing_token" to token); Unit }, dispatcher, clock
    ), CoroutineScope(playerJob + Dispatchers.IO))
    private var queue: MusicAssistantQueue? = null
    private var rows = emptyList<MusicAssistantQueueItem>()
    private var playlistData = emptyList<MediaItemData>()
    private var lastQueueFetchAt = -60_000L
    private var lastPositionAt = SystemClock.elapsedRealtime()
    private var serverClockAnchor: Pair<Double, Long>? = null
    private var lastClockProbeAt = -60_000L
    private var localAudioReady = false
    private var localPlayerId: String? = null
    private var error: PlaybackException? = null
    private var released = false
    private var targetId: String? = null
    private val pendingManual = mutableListOf<String>()
    private val metadata = mutableMapOf<String, MediaItem>()

    init {
        active.value = this
        scope.launch {
            receiver.state.collect { state ->
                publishConnectionStatus()
                if (state is PlayerState.Connected) localPlayerId = state.playerId
                val audio = when (state) {
                    is PlayerState.Connected -> state.audio
                    is PlayerState.Reconnecting -> state.audio
                    else -> null
                }
                val ready = audio?.phase == AudioPhase.Playing && !audio.starved
                if (ready != localAudioReady) { localAudioReady = ready; invalidateState() }
                if (state is PlayerState.Failed) {
                    error = PlaybackException(nativeStatus.value, null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                    invalidateState()
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L)
                if (sessionId != 0 && localAudioReady) wakeLock.acquire(10 * 60 * 1000L)
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
                    try { operations.withLock { refresh(forceItems = false) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    // This HTTP request may predate a successful WebSocket reconnect.
                    // Connection status belongs to the current receiver, not that old request.
                    catch (_: Exception) { publishConnectionStatus() }
                }
                delay(1000)
            }
        }
    }
    private fun publishConnectionStatus() {
        if (released || active.value !== this) return
        val current = receiver.state.value
        nativeStatus.value = when (current) {
            is PlayerState.Connected -> "Connected to ${current.serverName}"
            is PlayerState.Failed -> "Music Assistant: ${current.cause}"
            is PlayerState.Reconnecting -> "Reconnecting to Music Assistant"
            is PlayerState.Connecting -> "Connecting to Music Assistant"
            else -> "Music Assistant stopped"
        }
        connectionIssue.value = if (current is PlayerState.Reconnecting)
            "Connection lost. Reconnecting to Music Assistant…" else null
    }
    private suspend fun connect() {
        if (config.value != null) return
        foreground()
        val session = withContext(Dispatchers.IO) { client.account.snapshot() }
        val server = session.server
        val endpoint = server.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "sendspin"
        config.value = LocalPlayerConfig(Endpoint.WebSocket(endpoint, session.token), "Rhythm • ${android.os.Build.MODEL}",
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
    private suspend fun refreshClock() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClockProbeAt < 60_000) return
        lastClockProbeAt = now
        var bestRoundTrip = Long.MAX_VALUE
        repeat(3) {
            val sent = SystemClock.elapsedRealtime()
            val epoch = try { withTimeout(2000) { client.api().serverTime() } }
                catch (_: TimeoutCancellationException) { return@repeat }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { return@repeat }
            val received = SystemClock.elapsedRealtime()
            if (received - sent < bestRoundTrip) {
                bestRoundTrip = received - sent
                serverClockAnchor = epoch to (sent + bestRoundTrip / 2)
            }
        }
    }
    private fun serverNow(monotonicMs: Long) = serverClockAnchor?.let { (epoch, anchor) -> epoch + (monotonicMs - anchor) / 1000.0 }
        ?: (System.currentTimeMillis() / 1000.0)
    private suspend fun refresh(forceItems: Boolean = true) {
        val id = targetId ?: (receiver.state.value as? PlayerState.Connected)?.playerId ?: return
        refreshClock()
        val current = client.api().activeQueue(id) ?: return
        val now = SystemClock.elapsedRealtime()
        val reload = forceItems || now - lastQueueFetchAt >= 60_000 || current.id != queue?.id ||
            current.itemCount != rows.size || current.currentOccurrenceId != queue?.currentOccurrenceId ||
            current.currentIndex != queue?.currentIndex || current.nextOccurrenceId != queue?.nextOccurrenceId
        val items = if (reload) withContext(Dispatchers.IO) { readItems(current.id, current.itemCount) } else rows
        // Read back after pagination; never publish a torn queue when another client edits it.
        val verified = if (reload) client.api().activeQueue(id) ?: return else current
        if (verified.id != current.id || verified.itemCount != items.size ||
            (verified.currentOccurrenceId != null && items.none { it.occurrenceId == verified.currentOccurrenceId })) return
        if (queue?.currentOccurrenceId != verified.currentOccurrenceId || queue?.state != verified.state ||
            queue?.elapsedSeconds != verified.elapsedSeconds) lastPositionAt = now
        queue = verified
        if (reload) { rows = items; lastQueueFetchAt = now; rebuildPlaylist() }
        val currentIndex = items.indexOfFirst { it.occurrenceId == verified.currentOccurrenceId }
        val upcoming = items.drop((currentIndex + 1).coerceAtLeast(0)).map { it.occurrenceId }.toSet()
        pendingManual.retainAll(upcoming)
        error = null
        publishConnectionStatus()
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
    private fun rebuildPlaylist() {
        playlistData = rows.map { row -> MediaItemData.Builder(row.occurrenceId).setMediaItem(mediaItem(row))
            .setDurationUs(row.track?.durationMs?.times(1000) ?: C.TIME_UNSET).setIsSeekable(true).build() }
    }
    override fun getState(): State {
        val q = queue
        val playing = q?.state == "playing"
        val index = rows.indexOfFirst { it.occurrenceId == q?.currentOccurrenceId }.takeIf { it >= 0 } ?: 0
        val sampledAt = SystemClock.elapsedRealtime()
        val buffering = playing && (targetId == null || targetId == localPlayerId) && !localAudioReady
        val legacyDelta = if (playing && q?.elapsedUpdatedEpochSeconds == null) (sampledAt - lastPositionAt) / 1000.0 else 0.0
        val position = ((q?.positionSecondsAt(serverNow(sampledAt)) ?: 0.0) + legacyDelta) * 1000
        return State.Builder().setAvailableCommands(Player.Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_STOP, COMMAND_RELEASE,
            COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS, COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE,
            COMMAND_GET_METADATA, COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SEEK_TO_MEDIA_ITEM, COMMAND_SEEK_BACK, COMMAND_SEEK_FORWARD,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SET_REPEAT_MODE, COMMAND_SET_SHUFFLE_MODE, COMMAND_GET_AUDIO_ATTRIBUTES, COMMAND_GET_VOLUME
        ).build()).setPlaylist(playlistData)
            .setCurrentMediaItemIndex(if (rows.isEmpty()) C.INDEX_UNSET else index)
            .setPlayWhenReady(playing, PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setPlaybackState(if (rows.isEmpty() || error != null) STATE_IDLE else if (buffering) STATE_BUFFERING else STATE_READY)
            .setContentPositionMs(PositionSupplier { position.toLong() + if (playing) ((SystemClock.elapsedRealtime() - sampledAt) * (q?.playbackSpeed ?: 1.0)).toLong() else 0 })
            .setRepeatMode(when (q?.repeat) { "one" -> REPEAT_MODE_ONE; "all" -> REPEAT_MODE_ALL; else -> REPEAT_MODE_OFF })
            .setShuffleModeEnabled(q?.shuffle ?: false).setAudioSessionId(sessionId).setPlayerError(error).build()
    }
    private fun submit(block: suspend () -> Unit): ListenableFuture<Void> {
        val future = SettableFuture.create<Void>()
        val job = scope.launch {
            try {
                operations.withLock { block(); refresh() }
                future.set(null)
            } catch (failure: Exception) {
                if (failure is CancellationException && failure !is TimeoutCancellationException) {
                    future.cancel(false)
                    throw failure
                }
                android.util.Log.e("RhythmMA", "Native playback command failed", failure)
                // A timeout may have committed a mutation. Reconcile, never blindly replay it.
                withTimeoutOrNull(5000) { runCatching { operations.withLock { refresh() } } }
                if (released || active.value !== this@MusicAssistantPlayer) { future.cancel(false); return@launch }
                val message = "Music Assistant command failed. Refresh the server connection."
                if (rows.isEmpty()) error = PlaybackException(message, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
                else commandFailure.value = message
                publishConnectionStatus()
                invalidateState(); future.setException(failure)
            }
        }
        job.invokeOnCompletion { if (!future.isDone) future.cancel(false) }
        return future
    }
    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        require(mediaItems.all(::isMusicAssistant)) { "Local and server queues cannot be mixed" }
        mediaItems.forEach { metadata[uri(it)] = it }
        val requestedShuffle = mediaItems.firstOrNull()?.mediaMetadata?.extras
            ?.takeIf { it.containsKey(PlayNextCommand.REQUESTED_SHUFFLE) }
            ?.getBoolean(PlayNextCommand.REQUESTED_SHUFFLE) ?: shuffleModeEnabled
        return submit {
            pendingManual.clear()
            val id = queueId()
            if (mediaItems.isEmpty()) client.command("player_queues/clear", "queue_id" to id)
            else {
                foreground()
                client.api().replaceQueueAndSelect(id, mediaItems.map(::uri),
                    startIndex.coerceIn(0, mediaItems.lastIndex), (startPositionMs.coerceAtLeast(0) / 1000).toInt(), requestedShuffle)
            }
        }
    }
    override fun handleSetPlayWhenReady(playWhenReady: Boolean) = submit {
        if (playWhenReady) foreground()
        client.api().playerAction(target(), if (playWhenReady) MusicAssistantApi.PlayerAction.PLAY else MusicAssistantApi.PlayerAction.PAUSE)
    }
    override fun handlePrepare() = submit { connect() }
    override fun handleStop() = submit { client.api().playerAction(target(), MusicAssistantApi.PlayerAction.STOP) }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int) = submit {
        val qid = queueId()
        val wasPlaying = playWhenReady
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
        val empty = rows.isEmpty()
        addAt(items, (current + 1).coerceAtLeast(0), manual = true)
        if (empty) { foreground(); client.api().playerAction(target(), MusicAssistantApi.PlayerAction.PLAY) }
    }
    override fun handleAddMediaItems(index: Int, mediaItems: MutableList<MediaItem>) = submit { addAt(mediaItems, index, false) }
    private suspend fun addAt(items: List<MediaItem>, index: Int, manual: Boolean) {
        require(items.isNotEmpty() && items.all(::isMusicAssistant)) { "Only Music Assistant songs can be added to this server queue" }
        val id = queueId(); val before = readItems(id, client.api().activeQueue(target())?.itemCount ?: 0)
        client.api().append(id, items.map(::uri))
        val after = readItems(id, client.api().activeQueue(target())?.itemCount ?: 0)
        val beforeIds = before.map { it.occurrenceId }.toSet()
        val added = orderMusicAssistantAdditions(items.map(::uri), after.filter { it.occurrenceId !in beforeIds })
        items.forEach { metadata[uri(it)] = it }
        // ADD can reshuffle the existing tail too. Keep all unplayed manual requests
        // together, in reservation order, before the remaining context.
        val beforeById = before.associateBy { it.occurrenceId }
        val ordered = if (manual) pendingManual.mapNotNull(beforeById::get) + added else added
        for ((offset, row) in ordered.withIndex()) {
            val now = readItems(id, after.size)
            val from = now.indexOfFirst { it.occurrenceId == row.occurrenceId }
            val destination = (index + offset).coerceAtMost(now.lastIndex)
            if (from != destination) client.command("player_queues/move_item", "queue_id" to id, "queue_item_id" to row.occurrenceId, "pos_shift" to destination - from)
        }
        val verified = readItems(id, after.size)
        if (verified.drop(index).take(ordered.size).map { it.occurrenceId } != ordered.map { it.occurrenceId }) {
            // The server can protect already-buffered audio. Roll back only our identified
            // new occurrences; delete_item itself refuses played/buffered items.
            for (row in added.asReversed()) runCatching {
                client.command("player_queues/delete_item", "queue_id" to id, "item_id_or_index" to row.occurrenceId)
            }
            error("Server has buffered the next song; refresh queue order")
        }
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
            mediaItems.forEach { metadata[uri(it)] = it }; rebuildPlaylist(); invalidateState(); return Futures.immediateVoidFuture()
        }
        return Futures.immediateFailedFuture<Void>(UnsupportedOperationException("Replace server songs with an explicit Play action"))
    }
    fun keepCurrent() = submit {
        refresh()
        val id = queueId()
        val keep = queue?.currentOccurrenceId ?: return@submit
        val remove = rows.filter { it.occurrenceId != keep }.map { it.occurrenceId }
        remove.asReversed().forEach { client.command("player_queues/delete_item", "queue_id" to id, "item_id_or_index" to it) }
        pendingManual.clear()
    }
    fun selectedOutputId(): String? = targetId ?: (receiver.state.value as? PlayerState.Connected)?.playerId
    fun selectOutput(id: String, transfer: Boolean) = submit {
        val source = queueId()
        val targetQueue = client.api().activeQueue(id)?.id ?: error("Output has no queue")
        if (transfer && source != targetQueue) client.api().transfer(source, targetQueue)
        targetId = id
        pendingManual.clear()
    }
    fun setOutputVolume(id: String, volume: Int) = submit { client.api().volume(id, volume) }
    fun setMember(id: String, enabled: Boolean) = submit {
        client.api().groupMembers(target(), if (enabled) listOf(id) else emptyList(), if (enabled) emptyList() else listOf(id))
    }
    val isReleased: Boolean get() = released
    fun disconnectAccount() {
        queue = null
        rows = emptyList()
        playlistData = emptyList()
        pendingManual.clear()
        error = null
        invalidateState()
        release()
    }
    override fun handleRelease(): ListenableFuture<*> {
        if (released) return Futures.immediateVoidFuture()
        released = true
        config.value = null
        if (active.value === this) {
            active.value = null
            nativeStatus.value = "Music Assistant stopped"
            connectionIssue.value = null
        }
        val completion = SettableFuture.create<Void>()
        playerJob.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Await the audio loop's finally block before closing its dispatcher.
                playerJob.join()
                http.close()
                dispatcher.close()
                if (wakeLock.isHeld) wakeLock.release()
                completion.set(null)
            } catch (failure: Exception) { completion.setException(failure) }
        }
        return completion
    }
    companion object {
        val active = MutableStateFlow<MusicAssistantPlayer?>(null)
        val nativeStatus = MutableStateFlow("Music Assistant stopped")
        val connectionIssue = MutableStateFlow<String?>(null)
        val commandFailure = MutableStateFlow<String?>(null)
        fun isMusicAssistant(item: MediaItem) = item.mediaId.startsWith("MUSIC_ASSISTANT::") || item.localConfiguration?.uri?.scheme == "musicassistant"
        private fun uri(item: MediaItem): String = if (item.mediaId.startsWith("MUSIC_ASSISTANT::")) item.mediaId.removePrefix("MUSIC_ASSISTANT::")
            else Uri.decode(item.localConfiguration?.uri?.encodedPath?.removePrefix("/"))
    }
}
