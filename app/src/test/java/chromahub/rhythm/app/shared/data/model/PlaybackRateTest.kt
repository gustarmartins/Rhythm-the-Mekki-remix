/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.data.model

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlaybackRateTest {
    @Test fun independentControlsPreserveTheOtherValue() {
        val store = PlaybackRateStore(PlaybackRate()) {}
        store.update { it.withSpeed(1.25f) }
        store.update { it.withPitch(0.8f) }
        assertEquals(PlaybackRate(1.25f, 0.8f), store.state.value)
        store.update { it.withSpeed(1.5f) }
        assertEquals(0.8f, store.pitch.value)
    }

    @Test fun linkingImmediatelyAlignsPitchAndEitherControlMovesBoth() {
        val store = PlaybackRateStore(PlaybackRate(1.25f, 0.8f)) {}
        store.update { it.withSync(true) }
        assertEquals(PlaybackRate(1.25f, 1.25f, true), store.state.value)
        store.update { it.withPitch(0.75f) }
        assertEquals(PlaybackRate(0.75f, 0.75f, true), store.state.value)
        store.update { it.withSpeed(1.5f) }
        assertEquals(PlaybackRate(1.5f, 1.5f, true), store.state.value)
        store.update { it.withSync(false) }
        store.update { it.withPitch(1f) }
        assertEquals(PlaybackRate(1.5f, 1f), store.state.value)
    }

    @Test fun applyPersistsOneCompleteStateBeforeNotifyingEngine() = runBlocking {
        var disk = PlaybackRate()
        var writes = 0
        val store = PlaybackRateStore(disk) { disk = it; writes++ }
        val observed = mutableListOf<PlaybackRate>()
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            store.state.collect { value ->
                assertEquals(disk, value)
                observed.add(value)
            }
        }
        store.update { PlaybackRate(1.3f, 0.7f, false) }
        kotlinx.coroutines.yield()
        assertEquals(1, writes)
        assertEquals(listOf(PlaybackRate(), PlaybackRate(1.3f, 0.7f)), observed)
        observer.cancel()
        // A new process/service reads the same complete pair; opening a UI is only a reader.
        val restored = PlaybackRateStore(disk) { error("Reopening must not write defaults") }
        assertEquals(store.state.value, restored.state.value)
    }

    @Test fun linkedValuesSurviveRestartAndUnlinkWithoutResetting() {
        var disk = PlaybackRate()
        val first = PlaybackRateStore(disk) { disk = it }
        first.update { PlaybackRate(1.4f, 1.4f, true) }
        val restarted = PlaybackRateStore(disk) { disk = it }
        assertEquals(PlaybackRate(1.4f, 1.4f, true), restarted.state.value)
        restarted.update { it.withSync(false) }
        assertEquals(PlaybackRate(1.4f, 1.4f, false), disk)
    }

    @Test fun invalidLegacyPreferencesCannotReachTheAudioProcessor() {
        assertEquals(PlaybackRate(), PlaybackRate(Float.NaN, Float.POSITIVE_INFINITY).normalized())
        assertEquals(PlaybackRate(0.25f, 3f), PlaybackRate(-1f, 100f).normalized())
        assertEquals(PlaybackRate(1.25f, 1.25f, true), PlaybackRate(1.25f, 0.8f, true).normalized())
    }
}
