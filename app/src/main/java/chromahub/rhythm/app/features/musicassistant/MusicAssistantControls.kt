/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.musicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.musicassistant.api.MusicAssistantPlayer as Output
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Shared output chooser: selecting an output observes its queue; transfer is an explicit action. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MusicAssistantControls(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player by MusicAssistantPlayer.active.collectAsState()
    val status by MusicAssistantPlayer.nativeStatus.collectAsState()
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = modifier) { Text(stringResource(R.string.ma_outputs)) }
    if (!show) return
    val scope = rememberCoroutineScope()
    val client = remember(context) { MusicAssistantProviderClient(context) }
    var outputs by remember { mutableStateOf(emptyList<Output>()) }
    var selected by remember { mutableStateOf(player?.selectedOutputId()) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    suspend fun refresh() {
        outputs = client.api().players().filter { it.available }
        selected = player?.selectedOutputId()
    }
    fun perform(action: suspend () -> Unit) {
        scope.launch {
            busy = true; failure = false
            try { action(); refresh() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failure = true }
            finally { busy = false }
        }
    }
    LaunchedEffect(player) {
        try { refresh() } catch (_: Exception) { failure = true }
    }
    AlertDialog(onDismissRequest = { show = false }, title = { Text(stringResource(R.string.ma_outputs)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (player == null) stringResource(R.string.ma_start_song) else status)
                if (failure) Text(stringResource(R.string.ma_output_failed), color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(outputs, key = { it.id }) { output ->
                        Column {
                            Row {
                                RadioButton(selected = selected == output.id, onClick = {
                                    player?.let { perform { it.selectOutput(output.id, false).awaitCompletion() } }
                                }, enabled = player != null && !busy)
                                Text(output.name, Modifier.padding(top = 12.dp))
                            }
                            if (output.volume != null) {
                                var volume by remember(output.id, output.volume) { mutableFloatStateOf(output.volume.toFloat()) }
                                Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..100f,
                                    enabled = !busy, onValueChangeFinished = {
                                        perform { client.api().volume(output.id, volume.toInt()) }
                                    })
                            }
                            if (player != null && selected != output.id) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(enabled = !busy, onClick = { perform { player!!.selectOutput(output.id, true).awaitCompletion() } }) {
                                        Text(stringResource(R.string.ma_transfer))
                                    }
                                    val grouped = outputs.firstOrNull { it.id == selected }?.groupMembers?.contains(output.id) == true || output.syncedTo == selected
                                    TextButton(enabled = !busy, onClick = { perform { player!!.setMember(output.id, !grouped).awaitCompletion() } }) {
                                        Text(stringResource(if (grouped) R.string.ma_ungroup else R.string.ma_group))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { show = false }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { perform { refresh() } }) { Text(stringResource(R.string.ma_refresh)) } })
}

private suspend fun ListenableFuture<*>.awaitCompletion(): Unit = suspendCancellableCoroutine { continuation ->
    addListener({
        if (continuation.isActive) try { get(); continuation.resume(Unit) } catch (e: Exception) { continuation.resumeWithException(e) }
    }, java.util.concurrent.Executor { it.run() })
    // Do not cancel a server mutation when a dialog closes; its acknowledgement must be reconciled.
}
