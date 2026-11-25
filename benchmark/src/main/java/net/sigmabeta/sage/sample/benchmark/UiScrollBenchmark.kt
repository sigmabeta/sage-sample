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
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
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
import java.util.regex.Pattern
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
                DeviceBenchmark.lockPortrait(device)
                pressHome()
                // Start directly on the measured screen. The link is the app's own route model, so
                // the benchmark exercises the same deep-link path a user would.
                DeviceBenchmark.launchDeepLink(device, scenario.route)
                scenario.awaitContent(this)
                // One unmeasured cycle loads the screen's images, so every measured iteration starts
                // from the same cache state instead of only the first one paying for the loads.
                scenario.gesture(this, WARM_UP_CYCLES)
            },
        ) {
            scenario.gesture(this, SCROLL_CYCLES)
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
        val gesture: MacrobenchmarkScope.(cycles: Int) -> Unit,
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
        const val SCROLL_CYCLES = 3
        const val WARM_UP_CYCLES = 1

        /** Each vertical stroke toward the end moves this share of the scroller's height. */
        const val SCROLL_PERCENT = 0.6f

        /**
         * The stroke back is longer than the stroke out, so it stops against the list's start edge:
         * the same place every cycle. Equal strokes are not quite symmetric (the row came back 4 px
         * short on a Pixel 6a), and a shortfall that varies would make the workload vary.
         */
        const val RETURN_PERCENT = 0.8f

        /** Each stroke across Home's image row moves this share of the row's width, out and back. */
        const val ROW_SCROLL_PERCENT = 0.7f
        const val ROW_RETURN_PERCENT = 0.9f

        /** Stroke speed: slow enough that the release has no velocity, so nothing flings. */
        const val SCROLL_DP_PER_SECOND = 1_200f
        const val GESTURE_ATTEMPTS = 3
        const val GESTURE_MARGIN_DIVISOR = 10
        const val DIFF_ENTRIES = 6
        val ANY_TEXT: Pattern = Pattern.compile(".+")
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
                    gesture = { cycles -> homeScroll(cycles) },
                )

                UiScenario.LIST_ITEMS_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "listItemsScroll",
                    route = "browse/list-items",
                    landing = By.text("List Items"),
                    landingGone = By.text("Content detail"),
                    // Text-only rows: the list emits no trace sections.
                    traceSections = emptyList(),
                    gesture = { cycles -> scrollDownAndBack(cycles) },
                )

                UiScenario.GRID_OF_IMAGES_SCROLL -> UiScrollScenario(
                    scenario = scenario,
                    testName = "gridOfImagesScroll",
                    route = "browse/grid-of-images",
                    landing = By.text("Grid of images"),
                    landingGone = By.text("Content detail"),
                    traceSections = listOf("GridImage", "CrossfadeImage"),
                    gesture = { cycles -> scrollDownAndBack(cycles) },
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
                    gesture = { cycles -> scrollDownAndBack(cycles) },
                )
            }
        }

        /**
         * Scrolls the outer vertical list toward its end and back, [cycles] times, as a fixed
         * workload: the same strokes, from the same offset, every pass. The bounded, symmetric cycle
         * makes the images at the top leave and re-enter composition inside the measured window.
         *
         * Each stroke is a `UiObject2.scroll`: a constant-speed drag that holds still before lifting,
         * so the release has no velocity and the list does not fling. `device.drag` moves in a few
         * steps and lifts at full speed, so the list flung and the next drag caught it at a random
         * offset: frame counts for one commit varied by up to 45% between nightlies. Each cycle must
         * end where it started, or the run fails rather than record a different workload.
         */
        fun MacrobenchmarkScope.scrollDownAndBack(cycles: Int) {
            val speed = strokeSpeed()
            val top = withScroller(::verticalScroller, ::positionOf)
            repeat(cycles) { cycle ->
                // `scroll` names the content direction: DOWN moves toward the end, UP back to the top.
                stroke(::verticalScroller, Direction.DOWN, SCROLL_PERCENT, speed)
                stroke(::verticalScroller, Direction.UP, RETURN_PERCENT, speed)
                checkReturned("the list", top, "after scroll cycle ${cycle + 1} of $cycles")
            }
        }

        /**
         * Home's vertical plan has two rows, both composed by the time the landing wait finishes,
         * so a vertical-only stroke records no image trace sections. Reveal the nested horizontal
         * image row, scroll it across and back [cycles] times to compose new cells, then return the
         * outer list to the top. The row must end where it started after each cycle, and the list
         * must end at its top.
         */
        fun MacrobenchmarkScope.homeScroll(cycles: Int) {
            val speed = strokeSpeed()
            val top = withScroller(::verticalScroller, ::positionOf)
            stroke(::verticalScroller, Direction.DOWN, SCROLL_PERCENT, speed)
            val rowStart = withScroller(::horizontalScroller, ::positionOf)
            repeat(cycles) { cycle ->
                stroke(::horizontalScroller, Direction.RIGHT, ROW_SCROLL_PERCENT, speed)
                stroke(::horizontalScroller, Direction.LEFT, ROW_RETURN_PERCENT, speed)
                val row = withScroller(::horizontalScroller, ::positionOf)
                check(row == rowStart) {
                    "Home's image row did not return to its start after cycle ${cycle + 1} of $cycles: " +
                        positionDiff(rowStart, row)
                }
            }
            stroke(::verticalScroller, Direction.UP, RETURN_PERCENT, speed)
            checkReturned("Home", top, "after the image-row cycles")
        }

        /** [SCROLL_DP_PER_SECOND] in pixels per second on this device. */
        fun strokeSpeed(): Int {
            val density = InstrumentationRegistry.getInstrumentation().context.resources.displayMetrics.density
            return (SCROLL_DP_PER_SECOND * density).toInt()
        }

        /** One no-fling stroke on a freshly resolved scroller, then waits for the list to settle. */
        fun MacrobenchmarkScope.stroke(
            find: (UiDevice) -> UiObject2,
            direction: Direction,
            percent: Float,
            speed: Int,
        ) {
            withScroller(find) { it.scroll(direction, percent, speed) }
            device.waitForIdle()
        }

        /**
         * Fails unless the vertical list shows exactly what it showed at [start]: the same texts at
         * the same heights. A pass that starts anywhere else measures a different workload.
         */
        fun MacrobenchmarkScope.checkReturned(what: String, start: List<String>, whenChecked: String) {
            val now = withScroller(::verticalScroller, ::positionOf)
            check(now == start) { "$what did not return to where it started $whenChecked: ${positionDiff(start, now)}" }
        }

        /** The entries only [start] or only [now] has, for a failed position check's message. */
        fun positionDiff(start: List<String>, now: List<String>): String =
            "only at start ${(start - now.toSet()).take(DIFF_ENTRIES)}, only now ${(now - start.toSet()).take(DIFF_ENTRIES)}"

        /**
         * Where a scroller is, as the texts and content descriptions it shows with their offsets.
         * It is a position fingerprint, not a content check: two reads at the same scroll offset are
         * equal, and any offset change moves at least one entry. Fails when the scroller shows
         * nothing to read, since then no position could be checked.
         */
        fun positionOf(scroller: UiObject2): List<String> {
            val origin = scroller.visibleBounds
            val nodes = scroller.findObjects(By.text(ANY_TEXT)) + scroller.findObjects(By.desc(ANY_TEXT))
            val entries = nodes.mapNotNull { node ->
                runCatching {
                    val bounds = node.visibleBounds
                    "${node.text ?: node.contentDescription}@${bounds.left - origin.left},${bounds.top - origin.top}"
                }.getOrNull()
            }.sorted()
            check(entries.isNotEmpty()) { "The scroller shows no text to locate it by" }
            return entries
        }

        /**
         * Runs [gesture] on a freshly resolved scroller, retrying if the node detached between lookup
         * and gesture. A lazy list recomposes as it settles, so a node found a moment earlier can go
         * stale (`StaleObjectException`) before the gesture reads its bounds.
         */
        fun <T> MacrobenchmarkScope.withScroller(find: (UiDevice) -> UiObject2, gesture: (UiObject2) -> T): T {
            var lastStale: StaleObjectException? = null
            repeat(GESTURE_ATTEMPTS) {
                try {
                    val scroller = find(device)
                    val margin = device.displayWidth / GESTURE_MARGIN_DIVISOR
                    // Keep strokes off the edges, where the system gesture zones are. A row is too short
                    // for vertical margins and only needs horizontal ones.
                    val bounds = scroller.visibleBounds
                    if (bounds.height() > bounds.width()) {
                        scroller.setGestureMargin(margin)
                    } else {
                        scroller.setGestureMargins(margin, 0, margin, 0)
                    }
                    return gesture(scroller)
                } catch (stale: StaleObjectException) {
                    lastStale = stale
                    device.waitForIdle()
                }
            }
            throw lastStale ?: error("The scroller went stale $GESTURE_ATTEMPTS times")
        }

        /**
         * The screen's outer vertical scrollable. Home nests a horizontal row inside its vertical
         * list, so `By.scrollable` matches several nodes; pick the largest upright one.
         */
        fun verticalScroller(device: UiDevice): UiObject2 =
            scrollers(device).filter { (_, bounds) -> bounds.height() > bounds.width() }
                .maxByOrNull { (_, bounds) -> bounds.height() * bounds.width() }
                ?.first
                ?: error("No vertical scrollable container")

        /** The largest landscape scrollable: Home's nested image row, not the vertical list. */
        fun horizontalScroller(device: UiDevice): UiObject2 =
            scrollers(device).filter { (_, bounds) -> bounds.width() > bounds.height() }
                .maxByOrNull { (_, bounds) -> bounds.width() * bounds.height() }
                ?.first
                ?: error("No horizontal scrollable row")

        /** Every scrollable node with its bounds; a node that goes stale mid-lookup is skipped. */
        fun scrollers(device: UiDevice): List<Pair<UiObject2, android.graphics.Rect>> =
            device.findObjects(By.scrollable(true))
                .mapNotNull { node -> runCatching { node.visibleBounds }.getOrNull()?.let { node to it } }
    }
}
