/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.streaming

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chromahub.rhythm.app.features.streaming.presentation.components.bottomsheets.NearbyServerScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Optional real LAN qualification: -e discoveryHost <MA LAN address>. */
@RunWith(AndroidJUnit4::class)
class NearbyServerDiscoveryAndroidTest {
    @Test fun discoversMusicAssistantAgainAfterRescan() {
        val host = InstrumentationRegistry.getArguments().getString("discoveryHost").orEmpty()
        assumeTrue("Supply a Music Assistant LAN host to test Android NSD", host.isNotBlank())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        lateinit var scanner: NearbyServerScanner
        instrumentation.runOnMainSync {
            scanner = NearbyServerScanner(instrumentation.targetContext, "MUSIC_ASSISTANT", scope)
        }
        try {
            repeat(2) {
                instrumentation.runOnMainSync { scanner.rescan() }
                var found = false
                val deadline = System.nanoTime() + 12_000_000_000L
                while (!found && System.nanoTime() < deadline) {
                    instrumentation.runOnMainSync {
                        found = scanner.discoveredServers.any { it.host == host && it.type == "Music Assistant" }
                    }
                    if (!found) Thread.sleep(100)
                }
                assertTrue("MA advertisement should resolve on scan ${it + 1}", found)
                instrumentation.runOnMainSync { scanner.stopScan() }
            }
        } finally {
            instrumentation.runOnMainSync { scanner.stopScan(); scope.cancel() }
        }
    }
}
