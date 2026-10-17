package chromahub.rhythm.app.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup benchmarks for Rhythm — measures cold/warm/hot startup time to
 * quantify the improvement after applying the Baseline Profile.
 *
 * Test matrix:
 *   startupColdNoCompilation        — JIT only, fresh install baseline
 *   startupColdWithBaselineProfile  — ART AOT via Baseline Profile (target: -40%)
 *   startupColdFullCompilation      — Full AOT ahead-of-time (upper bound)
 *   startupWarmWithProfile          — Warm restart with profile
 *   startupHotWithProfile           — Hot restart (process alive)
 *
 * Perfetto traces are captured automatically for each run and saved to
 *   /sdcard/Android/data/chromahub.rhythm.app.macrobenchmark/
 *
 * Run:
 *   .\gradlew.bat :macrobenchmark:connectedGithubBenchmarkAndroidTest
 *       "-Pandroid.testInstrumentationRunnerArguments.class=chromahub.rhythm.app.benchmark.StartupBenchmarks"
 *       --no-daemon
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // ── 1. Cold – No Compilation (JIT baseline) ──────────────────────────────

    /**
     * Cold startup with NO compilation — simulates a fresh install (JIT only).
     * This is the true baseline: no pre-compiled code, no profile.
     * Compare timeToInitialDisplay with startupColdWithBaselineProfile.
     */
    @Test
    fun startupColdNoCompilation() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(
            StartupTimingMetric(),
            TraceSectionMetric("RhythmApplication.onCreate"),
        ),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(5_000)
    }

    // ── 2. Cold – Baseline Profile (primary optimization target) ─────────────

    /**
     * Cold startup WITH the generated Baseline Profile (35K+ classes pre-compiled by ART).
     * This is the primary benchmark; it quantifies the Baseline Profile ROI.
     *
     * Expected improvement vs startupColdNoCompilation:
     *   timeToInitialDisplay  : -30% to -45%
     *   timeToFullDisplay     : -20% to -35%
     */
    @Test
    fun startupColdWithBaselineProfile() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(
            StartupTimingMetric(),
            TraceSectionMetric("RhythmApplication.onCreate"),
        ),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 1,
        ),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(5_000)
    }

    // ── 3. Cold – Full AOT Compilation (upper bound) ──────────────────────────

    /**
     * Cold startup with FULL ahead-of-time compilation (speed profile).
     * This is the theoretical upper bound — everything pre-compiled, no JIT at all.
     * Useful to know how much headroom the Baseline Profile leaves vs full AOT.
     */
    @Test
    fun startupColdFullCompilation() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(
            StartupTimingMetric(),
        ),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(5_000)
    }

    // ── 4. Warm start – process alive, Activity recreated ────────────────────

    /**
     * Warm startup — process is already alive in background, Activity is recreated.
     * Measures Activity recreation + Compose recomposition overhead.
     * With profile: Compose hot-path already AOT'd, should be <800ms TTID.
     */
    @Test
    fun startupWarmWithProfile() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(
            StartupTimingMetric(),
            FrameTimingMetric(),
        ),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.UseIfAvailable,
            warmupIterations = 1,
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(3_000)

        // Navigate to Library and back to exercise warm recomposition paths
        repeat(3) {
            val btn = device.findObject(By.desc("Library"))
                ?: device.findObject(By.text("Library"))
                ?: return@repeat
            try { btn.click(); return@repeat } catch (_: Exception) { device.waitForIdle(500) }
        }
        device.waitForIdle(2_000)
    }

    // ── 5. Hot start – frame pacing when returning from recents ──────────────

    /**
     * Hot startup — measures frame pacing when the app is brought back from recents.
     * HOT mode doesn't produce StartupTimingMetric events (no activity-launch
     * atrace signal), so we use FrameTimingMetric to capture the re-entry
     * render cost (onResume → first Compose frame drawn).
     *
     * Expected P99 frameDurationCpuMs < 16ms with profile (all hot paths AOT'd).
     */
    @Test
    fun startupHotFramePacing() = benchmarkRule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
        ),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.UseIfAvailable,
            warmupIterations = 1,
        ),
        startupMode = StartupMode.HOT,
        iterations = 8,
        setupBlock = {
            grantRhythmRuntimePermissions(APP_PACKAGE)
        },
    ) {
        startActivityAndWait()
        device.waitForIdle(2_000)
        // Briefly scroll to generate frames after hot-resume
        val x = device.displayWidth / 2
        val top = (device.displayHeight * 0.3f).toInt()
        val bot = (device.displayHeight * 0.7f).toInt()
        repeat(3) {
            device.swipe(x, bot, x, top, 20)
            device.waitForIdle(300)
        }
    }

    private companion object {
        const val APP_PACKAGE = chromahub.rhythm.app.macrobenchmark.BuildConfig.TARGET_PACKAGE
    }
}
