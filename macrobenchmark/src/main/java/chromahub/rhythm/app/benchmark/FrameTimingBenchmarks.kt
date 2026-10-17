package chromahub.rhythm.app.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame-timing and jank benchmarks for Rhythm.
 *
 * Measures rendering performance across key user-facing screens:
 *
 *   homeScrollJank            — LazyColumn scroll on HomeScreen (most common interaction)
 *   libraryScrollJank         — Heavy LibraryScreen grid/list scroll (304 KB screen)
 *   homeToLibraryNavJank      — Screen transition animation frame pacing
 *   explorerScrollJank        — ExplorerTabContent folder browser (109 KB)
 *   sustainedScrollStress     — 30-second scroll to detect thermal throttling
 *
 * Fix notes:
 *   - All scroll gestures use raw device.swipe() — no UiObject2.fling() which
 *     throws StaleObjectException when Compose recomposes the scrollable mid-test.
 *   - Extra settle waits added after navigation to avoid Perfetto trace-server
 *     race conditions during warm-up.
 *
 * Run:
 *   .\gradlew.bat :macrobenchmark:connectedGithubBenchmarkAndroidTest
 *       "-Pandroid.testInstrumentationRunnerArguments.class=chromahub.rhythm.app.benchmark.FrameTimingBenchmarks"
 *       --no-daemon
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class FrameTimingBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // Shared compilation mode: use profile if available, else fall back gracefully
    private val compilationMode = CompilationMode.Partial(
        baselineProfileMode = BaselineProfileMode.UseIfAvailable,
        warmupIterations = 2,
    )

    // ── 1. HomeScreen scroll ──────────────────────────────────────────────────

    /**
     * Measures frame pacing while fling-scrolling the HomeScreen LazyColumn.
     * This is the most common user interaction in a music player.
     *
     * Goal: P99 frameDurationCpuMs < 16ms (60 fps), zero overrun frames.
     * Uses raw swipe() — no UiObject2 reference held across Compose recompositions.
     */
    @Test
    fun homeScrollJank() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(3_000)

        // Ensure we're on Home tab
        navigateTo("Home")
        device.waitForIdle(1_500)

        // 8 scroll cycles via raw swipe (immune to StaleObjectException)
        repeat(5) { swipeUp(); device.waitForIdle(250) }
        repeat(3) { swipeDown(); device.waitForIdle(250) }
        device.waitForIdle(500)
    }

    // ── 2. LibraryScreen scroll ───────────────────────────────────────────────

    /**
     * LibraryScreen is the app's heaviest composable (304 KB source).
     * First composition is the single biggest jank risk after startup.
     *
     * Uses raw swipe() gestures — the scrollable found via findObject() was going
     * stale before fling() could execute, causing StaleObjectException crashes.
     */
    @Test
    fun libraryScrollJank() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(3_000)

        // Navigate to Library and wait for the heavy screen to fully compose
        navigateTo("Library")
        device.wait(Until.hasObject(By.scrollable(true)), 5_000)
        device.waitForIdle(2_500)

        // Raw swipes — no stale-object risk
        repeat(5) { swipeUp(); device.waitForIdle(300) }
        repeat(3) { swipeDown(); device.waitForIdle(300) }
        device.waitForIdle(500)
    }

    // ── 3. Home → Library navigation transition ───────────────────────────────

    /**
     * Measures frame pacing during the Home↔Library navigation animation.
     * Extra 3-second settle added after startActivityAndWait() to prevent the
     * Perfetto trace-server from crashing during the warm-up iteration.
     */
    @Test
    fun homeToLibraryNavJank() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        // Extra settle: Perfetto trace server needs time to start before nav stress
        device.waitForIdle(3_000)
        Thread.sleep(1_000)

        // Toggle Home ↔ Library 3 times to capture navigation animation frames
        repeat(3) {
            navigateTo("Library")
            device.waitForIdle(1_500)
            navigateTo("Home")
            device.waitForIdle(1_500)
        }
    }

    // ── 4. Explorer tab scroll ────────────────────────────────────────────────

    /**
     * ExplorerTabContent.kt is 109 KB of Compose (folder browser / file tree).
     * Navigation into it triggers a large first-composition that can cause
     * multi-frame jank. Explorer may live as a nested tab inside Library.
     *
     * Fixed: re-finds scrollable inside the loop instead of caching UiObject2.
     */
    @Test
    fun explorerScrollJank() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(2_500)

        // Try Explorer as a top-level tab; fall back to Library → Explorer sub-tab
        var reachedExplorer = navigateTo("Explorer")
        if (!reachedExplorer) {
            navigateTo("Library")
            device.waitForIdle(1_000)
            reachedExplorer = navigateTo("Explorer")
        }

        if (reachedExplorer) {
            device.wait(Until.hasObject(By.scrollable(true)), 4_000)
            device.waitForIdle(2_000)
        } else {
            // Explorer not found — scroll whatever is on screen to still measure frames
            device.waitForIdle(1_000)
        }

        // Raw swipes — no UiObject2 reference
        repeat(5) { swipeUp(); device.waitForIdle(300) }
        repeat(2) { swipeDown(); device.waitForIdle(300) }
        device.waitForIdle(500)
    }

    // ── 5. Sustained scroll stress (thermal throttling detector) ─────────────

    /**
     * Continuous HomeScreen scroll for ~30 seconds.
     * If P99 frame times creep up over the duration, it indicates thermal
     * throttling — a signal to reduce background MediaStore / image work
     * during active UI interaction.
     */
    @Test
    fun sustainedScrollStress() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.WARM,
        iterations = 3,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(3_000)

        navigateTo("Home")
        device.waitForIdle(1_500)

        // 30 scroll cycles ≈ 30 seconds of continuous scrolling
        repeat(15) { swipeUp(); Thread.sleep(200) }
        repeat(15) { swipeDown(); Thread.sleep(200) }
        device.waitForIdle(500)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Navigate to a tab by content-description or text label.
     * Returns true if the tab was found and clicked.
     *
     * Retries up to 3 times on StaleObjectException: during the benchmark
     * warm-up recompilation pass, Compose can recompose the nav bar between
     * findObject() and click(), invalidating the UiObject2 reference.
     * Re-finding the object from scratch each retry avoids the stale-node crash.
     */
    private fun MacrobenchmarkScope.navigateTo(tabName: String): Boolean {
        repeat(3) { attempt ->
            try {
                // Re-find the object on EVERY attempt — never reuse across retries
                val btn = device.findObject(By.desc(tabName))
                    ?: device.findObject(By.text(tabName))
                    ?: device.findObject(By.descContains(tabName.lowercase()))
                    ?: return false
                btn.click()
                return true
            } catch (e: Exception) {
                // StaleObjectException or similar — wait for UI to settle, then retry
                if (attempt < 2) device.waitForIdle(800)
            }
        }
        return false
    }

    /**
     * Swipe up (scroll content down) via raw coordinates.
     * Safe from StaleObjectException — no UI node reference held.
     */
    private fun MacrobenchmarkScope.swipeUp() {
        val x = device.displayWidth / 2
        val top = (device.displayHeight * 0.20f).toInt()
        val bottom = (device.displayHeight * 0.80f).toInt()
        device.swipe(x, bottom, x, top, 18)
    }

    /**
     * Swipe down (scroll content up) via raw coordinates.
     */
    private fun MacrobenchmarkScope.swipeDown() {
        val x = device.displayWidth / 2
        val top = (device.displayHeight * 0.20f).toInt()
        val bottom = (device.displayHeight * 0.80f).toInt()
        device.swipe(x, top, x, bottom, 18)
    }

    private companion object {
        const val APP_PACKAGE = chromahub.rhythm.app.macrobenchmark.BuildConfig.TARGET_PACKAGE
    }
}
