package net.sigmabeta.sage.sample.benchmark

import android.annotation.SuppressLint
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.json.BenchmarkData
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.macrobenchmarkWithStartupMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.sigmabeta.sage.sample.perfbench.PerfConfig
import net.sigmabeta.sage.sample.perfbench.StartupAxis
import net.sigmabeta.sage.sample.perfbench.StartupAxisConfig
import net.sigmabeta.sage.sample.perfbench.StartupMeasurement
import net.sigmabeta.sage.sample.perfbench.StartupResult
import net.sigmabeta.sage.sample.perfbench.StartupScenario
import net.sigmabeta.sage.sample.perfbench.startupResult
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures app startup — cold and warm — up to Home's first content, and writes a perfbench
 * `results-startup.json` (device fingerprint + `startup` axis) to the run's additional test output
 * directory.
 *
 * "First content" is the first content-bearing image on Home. In the `benchmark` build type the app
 * calls `Activity.reportFullyDrawn()` the first time a `CrossfadeImage` composes, which the
 * macrobenchmark's [StartupTimingMetric] records as `timeToFullDisplayMs`. That time-to-full-display
 * is what this benchmark reports as time-to-first-content. Navigating elsewhere would measure
 * startup *plus* navigation, not startup.
 *
 * Run via:
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    // The JUnit4 rule discards the typed result this needs, so it calls the macrobenchmark entry
    // point the rule wraps directly.
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalMetricApi::class, ExperimentalBenchmarkConfigApi::class)
    @Test
    fun coldAndWarmStartup() {
        DeviceBenchmark.beginTest("coldAndWarmStartup")
        try {
            // Probe the device facts (including thermal status "at start") before the measured loop.
            val environment = DeviceBenchmark.environment()
            // Sign in once, outside the measurement: the Session is stored, so no measured launch
            // pays for the PBKDF2 hashing a first sign-in runs.
            DeviceBenchmark.signInOnce(
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
            )
            val results = StartupScenario.all.map { scenario ->
                DeviceBenchmark.section(scenario.id) { startup(scenario) }
            }

            DeviceBenchmark.write(
                DeviceBenchmark.perfResult(
                    environment = environment,
                    config = PerfConfig(scenarios = StartupScenario.all.map { it.id }),
                    axes = mapOf(
                        "startup" to StartupAxis(
                            config = StartupAxisConfig(iterations = DeviceBenchmark.ITERATIONS),
                            results = results,
                        ),
                    ),
                ),
            )
        } finally {
            DeviceBenchmark.endTest("coldAndWarmStartup")
        }
    }

    /** Runs one measured startup scenario and returns its time-to-first-content result. */
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalMetricApi::class, ExperimentalBenchmarkConfigApi::class)
    private fun startup(scenario: StartupScenario): StartupResult {
        val mode = when (scenario) {
            StartupScenario.COLD -> StartupMode.COLD
            StartupScenario.WARM -> StartupMode.WARM
        }
        val testName = "${scenario.name.lowercase()}Startup"
        val testResult = macrobenchmarkWithStartupMode(
            uniqueName = "StartupBenchmark_$testName",
            className = this::class.java.name,
            testName = testName,
            packageName = DeviceBenchmark.TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = DeviceBenchmark.ITERATIONS,
            experimentalConfig = null,
            startupMode = mode,
            setupBlock = {
                pressHome()
            },
        ) {
            // The launcher intent plus the sign-in extra: the Session is already stored, so the hook
            // only reuses it, but the launch is the same one an unmeasured run would make.
            startActivityAndWait { DeviceBenchmark.putSignIn(it) }
            // The Sign-in gate's startup check draws a blank first frame, so Home's first image can
            // compose after the launch is reported complete. Keep the trace open until it has, or
            // the app's reportFullyDrawn() lands outside the measured window.
            device.wait(Until.hasObject(By.textStartsWith(HOME_CONTENT_PREFIX)), HOME_CONTENT_TIMEOUT_MS)
        }

        return startupResult(startupMeasurement(scenario.id, testResult))
    }

    /** Pools the macrobenchmark's launch samples into a [StartupMeasurement]. */
    private fun startupMeasurement(scenario: String, testResult: BenchmarkData.TestResult): StartupMeasurement {
        // StartupTimingMetric emits a *single* metric (one value per launch), so it lands in
        // `metrics` — not `sampledMetrics`, which is for multi-sample metrics like frame timing.
        // timeToFullDisplayMs exists only if the app reported itself fully drawn.
        val samples = testResult.metrics[FULL_DISPLAY_METRIC]?.runs.orEmpty()
        check(samples.isNotEmpty()) {
            "No $FULL_DISPLAY_METRIC samples — the benchmark build must report the app fully drawn " +
                "when Home shows its first image. Is the app's BENCHMARK build type installed and " +
                "LocalFirstContentReporter wired?"
        }
        return StartupMeasurement(scenario = scenario, timeToFirstContentMillis = samples)
    }

    private companion object {
        const val FULL_DISPLAY_METRIC = "timeToFullDisplayMs"

        /** Home's first content row (`Big image 1`), the same text the UI axis lands on. */
        const val HOME_CONTENT_PREFIX = "Big image"
        const val HOME_CONTENT_TIMEOUT_MS = 10_000L
    }
}
