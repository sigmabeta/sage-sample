package net.sigmabeta.sage.sample.benchmark

import android.annotation.SuppressLint
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.json.BenchmarkData
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.macrobenchmarkWithStartupMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import net.sigmabeta.sage.sample.perfbench.PerfConfig
import net.sigmabeta.sage.sample.perfbench.TraceSectionMeasurement
import net.sigmabeta.sage.sample.perfbench.UiAxis
import net.sigmabeta.sage.sample.perfbench.UiAxisConfig
import net.sigmabeta.sage.sample.perfbench.UiMeasurement
import net.sigmabeta.sage.sample.perfbench.UiResult
import net.sigmabeta.sage.sample.perfbench.UiScenario
import net.sigmabeta.sage.sample.perfbench.uiResult
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scrolls each core sample surface — Home, the text and image List Items lists, the 50-image grid,
 * and Content detail — and records frame timing plus the trace-section sums behind the app's
 * `TraceSection`s (none for the text-only list). Every scenario is written into one perfbench
 * `results-ui.json` (device fingerprint + `ui` axis) in the run's additional test output directory,
 * so no scenario overwrites another.
 *
 * Each scenario starts on its screen through the app's deep link (`sagesample://browse/list-items`),
 * so the navigation UI is never part of the measured surface.
 *
 * Run via:
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * The captured Perfetto trace per iteration lands under
 *   benchmark/build/outputs/connected_android_test_additional_output/.../
 * and can be opened directly in https://ui.perfetto.dev.
 */
@RunWith(AndroidJUnit4::class)
class UiScrollBenchmark {

    // The JUnit4 rule discards the typed result this needs, so it calls the macrobenchmark entry
    // point the rule wraps directly.
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalMetricApi::class, ExperimentalBenchmarkConfigApi::class)
    @Test
    fun scrollScenarios() {
        DeviceBenchmark.beginTest("scrollScenarios")
        try {
            // Probe the device facts (including thermal status "at start") before the measured loop.
            val environment = DeviceBenchmark.environment()
            val scenarios = selectedScenarios()
            val results = scenarios.map { scenario ->
                DeviceBenchmark.section(scenario.scenario.id) { runScenario(scenario) }
            }

            DeviceBenchmark.write(
                DeviceBenchmark.perfResult(
                    environment = environment,
                    config = PerfConfig(scenarios = scenarios.map { it.scenario.id }),
                    axes = mapOf(
                        "ui" to UiAxis(
                            config = UiAxisConfig(iterations = DeviceBenchmark.ITERATIONS),
                            results = results,
                        ),
                    ),
                ),
            )
        } finally {
            DeviceBenchmark.endTest("scrollScenarios")
        }
    }

    /**
     * The scenarios to run: all of them, or the one named by the `scenario` instrumentation argument
     * (e.g. `...args=scenario:ui.grid_of_images_scroll`). A single-scenario run is the fast way to
     * tune one device recipe without paying for the whole suite.
     */
    private fun selectedScenarios(): List<UiScrollScenario> {
        val id = InstrumentationRegistry.getArguments().getString(SCENARIO_ARG) ?: return SCENARIOS
        val scenario = UiScenario.fromId(id) ?: error("Unknown scenario id '$id' (from --scenario)")
        return SCENARIOS.filter { it.scenario == scenario }
    }

    /** Runs one measured scroll scenario and returns its frame + trace-section result. */
    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalMetricApi::class, ExperimentalBenchmarkConfigApi::class)
    private fun runScenario(scenario: UiScrollScenario): UiResult {
        val testResult = macrobenchmarkWithStartupMode(
            uniqueName = "UiScrollBenchmark_${scenario.testName}",
            className = this::class.java.name,
            testName = scenario.testName,
            packageName = DeviceBenchmark.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()) +
                scenario.traceSections.map { TraceSectionMetric(it, TraceSectionMetric.Mode.Sum) },
            compilationMode = CompilationMode.DEFAULT,
            iterations = DeviceBenchmark.ITERATIONS,
            experimentalConfig = null,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                // Start directly on the measured screen. The link is the app's own route model, so
                // the benchmark exercises the same deep-link path a user would.
                DeviceBenchmark.launchDeepLink(device, scenario.route)
                scenario.awaitContent(this)
            },
        ) {
            scenario.gesture(this)
        }

        return uiResult(uiMeasurement(scenario, testResult))
    }

    /** Pools the macrobenchmark's frame samples and labelled trace sums into a [UiMeasurement]. */
    private fun uiMeasurement(scenario: UiScrollScenario, testResult: BenchmarkData.TestResult): UiMeasurement =
        UiMeasurement(
            scenario = scenario.scenario.id,
            // FrameTimingMetric reports frameDurationCpuMs/frameOverrunMs as pooled samples, one
            // list per iteration; flatten so percentiles and jank come from every measured frame.
            frameCpuMillis = testResult.sampledMetrics[FRAME_DURATION_METRIC]?.runs?.flatten().orEmpty(),
            frameOverrunMillis = testResult.sampledMetrics[FRAME_OVERRUN_METRIC]?.runs?.flatten().orEmpty(),
            traceSections = scenario.traceSections.mapNotNull { name ->
                val sum = testResult.metrics["${name}SumMs"] ?: return@mapNotNull null
                val count = testResult.metrics["${name}Count"]
                TraceSectionMeasurement(name = name, sumMs = sum.median, count = count?.median?.toInt() ?: 0)
            },
        )

    /**
     * One scroll scenario's device recipe: the deep-link route to start on, the text that proves the
     * screen's content has landed, the gesture to run inside the measured loop, and the trace
     * sections that screen emits. Keeping all four together makes adding a screen a local change.
     */
    private class UiScrollScenario(
        val scenario: UiScenario,
        val testName: String,
        val route: String,
        val landing: BySelector,
        val landingGone: BySelector?,
        val traceSections: List<String>,
        val gesture: MacrobenchmarkScope.() -> Unit,
    ) {
        /** Waits for content, then for the screen's own vertical scroller. */
        fun awaitContent(scope: MacrobenchmarkScope) {
            with(scope) {
                if (!device.wait(Until.hasObject(landing), NAV_TIMEOUT_MS)) {
                    // Keep what the device actually showed: the in-process dump does not depend on
                    // the uiautomator shell tool being able to reach the window.
                    runCatching {
                        device.dumpWindowHierarchy(
                            DeviceBenchmark.artifactFile("failure-$testName.xml"),
                        )
                    }
                    runCatching {
                        device.takeScreenshot(
                            DeviceBenchmark.artifactFile("failure-$testName.png"),
                        )
                    }
                    error(
                        "Deep link '$route' did not land on its content " +
                            "(focused package ${device.currentPackageName})",
                    )
                }
                // A Browse-menu row carries the same text as the destination's title, so a link that
                // only switched to the Browse tab would otherwise look like a landing. The last
                // menu row is gone once the destination screen is pushed.
                if (landingGone != null) {
                    check(device.wait(Until.gone(landingGone), NAV_TIMEOUT_MS)) {
                        "Deep link '$route' stayed on the Browse menu"
                    }
                }
                check(device.wait(Until.hasObject(By.scrollable(true)), NAV_TIMEOUT_MS)) {
                    "Screen '$route' exposed no scrollable content"
                }
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val NAV_TIMEOUT_MS = 10_000L
        const val SCENARIO_ARG = "scenario"
        const val SCROLL_TOP_FRACTION = 0.25f
        const val SCROLL_STEPS = 10
        const val SCROLL_CYCLES = 3
        const val SCROLL_FRACTION = 0.6f
        const val SCROLL_EDGE_FRACTION = 0.1f
        const val FRAME_DURATION_METRIC = "frameDurationCpuMs"
        const val FRAME_OVERRUN_METRIC = "frameOverrunMs"

        /**
         * The scenarios, built by mapping [UiScenario.all] through an exhaustive `when`, so every
         * enum value has a row and a new one is a compile error here rather than a runtime lookup
         * failure. The mapping preserves enum order, so [scrollScenarios] reports them as declared.
         */
        val SCENARIOS: List<UiScrollScenario> = UiScenario.all.map { scenario ->
            when (scenario) {
                UiScenario.HOME_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "homeScroll",
                    route = "home",
                    // The first content row proves Home's modules emitted, not just that the shell
                    // composed. `startsWith` tolerates a randomized plan's instance number.
                    landing = By.textStartsWith("Big image"),
                    landingGone = null,
                    traceSections = listOf("GridImage", "CrossfadeImage"),
                    // Home's two-row stable plan already composes both rows by the time the shell
                    // lands, so a vertical-only gesture records no image sections. Reveal the
                    // nested image row and scroll it horizontally to compose new cells, then return
                    // to the top.
                    gesture = { homeScroll() },
                )

                UiScenario.LIST_ITEMS_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "listItemsScroll",
                    route = "browse/list-items",
                    landing = By.text("List Items"),
                    landingGone = By.text("Content detail"),
                    // Text-only rows: the list emits no trace sections.
                    traceSections = emptyList(),
                    gesture = { scrollDownAndBack(SCROLL_FRACTION) },
                )

                UiScenario.GRID_OF_IMAGES_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "gridOfImagesScroll",
                    route = "browse/grid-of-images",
                    landing = By.text("Grid of images"),
                    landingGone = By.text("Content detail"),
                    traceSections = listOf("GridImage", "CrossfadeImage"),
                    gesture = { scrollDownAndBack(SCROLL_FRACTION) },
                )

                UiScenario.CONTENT_DETAIL_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "contentDetailScroll",
                    // A known generated ID: the detail's title is derived from it, so the route is
                    // stable without depending on whichever content was opened last.
                    route = "home/content-detail/grid-of-images:7",
                    // The detail's title is generated from the ID and its "Details" section starts
                    // below the hero, so neither is a stable above-the-fold landing signal. The top
                    // bar's back affordance proves a pushed screen opened; Home (where a failed
                    // deep link leaves the app) has none.
                    landing = By.desc("Back"),
                    landingGone = null,
                    // The hero and the related/item rows are images; there are no grid cells.
                    traceSections = listOf("CrossfadeImage"),
                    // A bounded cycle leaves and re-enters the top region, recomposing the hero and
                    // its images inside the measured window instead of settling at the bottom of a
                    // long page.
                    gesture = { scrollDownAndBack(SCROLL_FRACTION) },
                )
            }
        }

        /**
         * Scrolls the outer vertical list toward its end by [fraction] of the screen and back,
         * [SCROLL_CYCLES] times. Unlike a fling this is bounded and symmetric, so the images at the
         * top leave and re-enter composition within the measured window.
         */
        fun MacrobenchmarkScope.scrollDownAndBack(fraction: Float) {
            val centerX = device.displayWidth / 2
            val top = (device.displayHeight * SCROLL_TOP_FRACTION).toInt()
            val bottom = (top + device.displayHeight * fraction).toInt()
            repeat(SCROLL_CYCLES) {
                device.drag(centerX, bottom, centerX, top, SCROLL_STEPS)
                device.drag(centerX, top, centerX, bottom, SCROLL_STEPS)
            }
        }

        /**
         * Home's vertical plan has two rows, both composed by the time the landing wait finishes,
         * so a vertical-only drag records no image trace sections. Reveal the nested horizontal
         * image row, drag it across and back to compose new cells, then return the outer list to
         * the top. The row is looked up after the vertical move because that is when it is fully
         * on screen.
         */
        fun MacrobenchmarkScope.homeScroll() {
            val centerX = device.displayWidth / 2
            val top = (device.displayHeight * SCROLL_TOP_FRACTION).toInt()
            val bottom = (top + device.displayHeight * SCROLL_FRACTION).toInt()

            device.drag(centerX, bottom, centerX, top, SCROLL_STEPS)
            horizontalScroller(device)?.let { row ->
                val bounds = row.visibleBounds
                val y = bounds.centerY()
                val edge = (bounds.width() * SCROLL_EDGE_FRACTION).toInt()
                repeat(SCROLL_CYCLES) {
                    device.drag(bounds.right - edge, y, bounds.left + edge, y, SCROLL_STEPS)
                    device.drag(bounds.left + edge, y, bounds.right - edge, y, SCROLL_STEPS)
                }
            }
            device.drag(centerX, top, centerX, bottom, SCROLL_STEPS)
        }

        /** The largest landscape scrollable: Home's nested image row, not the vertical list. */
        fun horizontalScroller(device: UiDevice): UiObject2? =
            device.findObjects(By.scrollable(true))
                .mapNotNull { node -> runCatching { node.visibleBounds }.getOrNull()?.let { node to it } }
                .filter { (_, bounds) -> bounds.width() > bounds.height() }
                .maxByOrNull { (_, bounds) -> bounds.width() * bounds.height() }
                ?.first
    }
}
