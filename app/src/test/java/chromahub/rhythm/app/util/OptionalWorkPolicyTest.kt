package chromahub.rhythm.app.util

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class OptionalWorkPolicyTest {
    @Test fun thresholdChargingAndHysteresis() {
        assertTrue(automaticBatterySaver(true, 15, false, 15, false))
        assertFalse(automaticBatterySaver(true, 16, false, 15, false))
        assertTrue(automaticBatterySaver(true, 17, false, 15, true))
        assertFalse(automaticBatterySaver(true, 18, false, 15, true))
        assertFalse(automaticBatterySaver(true, 1, true, 15, true))
        assertFalse(automaticBatterySaver(false, 1, false, 15, true))
        assertFalse(automaticBatterySaver(true, null, false, 15, false))
        assertTrue(automaticBatterySaver(true, 30, false, 30, false))
    }
    @Test fun perFeatureChoicesApplyInForegroundAndBackground() {
        for (foreground in listOf(true, false)) {
            val policy = OptionalWorkPolicy(foreground, true, setOf(OptionalWork.ARTWORK, OptionalWork.LIBRARY))
            assertFalse(policy.allows(OptionalWork.ARTWORK))
            assertFalse(policy.allows(OptionalWork.LIBRARY))
            assertTrue(policy.allows(OptionalWork.LYRICS))
            assertEquals(1, policy.concurrency)
        }
        assertEquals(3, OptionalWorkPolicy(foreground = true).concurrency)
    }
    @Test fun backgroundTransitionDrainsExistingWorkBeforeAdmittingMore() = runBlocking {
        withTimeout(5_000) {
            val gate = OptionalWorkGate(OptionalWorkPolicy(foreground = true))
            val release = CompletableDeferred<Unit>()
            var started = 0
            val jobs = List(3) { launch { gate.run(OptionalWork.LIBRARY) { started++; release.await() } } }
            yield()
            assertEquals(3, started)
            gate.update(OptionalWorkPolicy())
            val waiting = launch { gate.run(OptionalWork.LYRICS) { started++ } }
            yield()
            assertEquals(3, started)
            release.complete(Unit)
            jobs.joinAll(); waiting.join()
            assertEquals(4, started)
        }
    }
    @Test fun saverSuspendsUntilAllowedAndCancellationDoesNotLeakCapacity() = runBlocking {
        withTimeout(5_000) {
            val gate = OptionalWorkGate(OptionalWorkPolicy(true, true, setOf(OptionalWork.ARTWORK)))
            var ran = false
            val cancelled = launch { gate.run(OptionalWork.ARTWORK) { fail("Cancelled task ran") } }
            yield(); cancelled.cancelAndJoin()
            val waiting = launch { gate.run(OptionalWork.ARTWORK) { ran = true } }
            yield(); assertFalse(ran)
            gate.run(OptionalWork.LYRICS) { /* allowed exception does not wait for artwork */ }
            gate.update(OptionalWorkPolicy(foreground = true))
            waiting.join(); assertTrue(ran)
            try { gate.run(OptionalWork.LIBRARY) { error("failure") } } catch (_: IllegalStateException) { }
            gate.run(OptionalWork.LIBRARY) { assertTrue(true) }
        }
    }
}
