package net.sigmabeta.sage.sample.perfbench

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffTest {
    @Test
    fun `geometric mean summarises ratios and cancels symmetric changes`() {
        assertEquals(2.0, geometricMean(listOf(1.0, 4.0)), TOLERANCE)
        assertEquals(4.0, geometricMean(listOf(2.0, 8.0)), TOLERANCE)
        assertEquals(1.0, geometricMean(listOf(2.0, 0.5)), TOLERANCE)
        assertEquals(0.5, geometricMean(listOf(0.5)), TOLERANCE)
    }

    @Test
    fun `geometric mean rejects empty and non-positive input`() {
        assertThrows(IllegalArgumentException::class.java) { geometricMean(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { geometricMean(listOf(1.0, 0.0)) }
        assertThrows(IllegalArgumentException::class.java) { geometricMean(listOf(1.0, -2.0)) }
    }

    @Test
    fun `compares the ui axis by frame p50`() {
        val baseline = result(axes = mapOf("ui" to uiAxis(ui("ui.home_scroll", 12.0))))
        val current = result(axes = mapOf("ui" to uiAxis(ui("ui.home_scroll", 8.0))))

        val compared = assertIs<DiffOutcome.Compared>(compareRuns(baseline, current))
        val axis = compared.report.axes.single()

        assertEquals("ui", axis.axis)
        assertEquals("frame p50 (ms)", axis.metricLabel)
        val scenario = axis.scenarios.single()
        assertEquals(12.0, scenario.baselineMetric, TOLERANCE)
        assertEquals(8.0, scenario.currentMetric, TOLERANCE)
        assertEquals(1.5, scenario.ratio, TOLERANCE)
        assertEquals(1.5, compared.report.geometricMean, TOLERANCE)
    }

    @Test
    fun `compares the startup axis by time to first content p50`() {
        val baseline = result(axes = mapOf("startup" to startupAxis(startup("startup.cold", 400.0))))
        val current = result(axes = mapOf("startup" to startupAxis(startup("startup.cold", 500.0))))

        val compared = assertIs<DiffOutcome.Compared>(compareRuns(baseline, current))
        val axis = compared.report.axes.single()

        assertEquals("startup", axis.axis)
        assertEquals("time to first content p50 (ms)", axis.metricLabel)
        assertEquals(0.8, axis.scenarios.single().ratio, TOLERANCE)
    }

    @Test
    fun `compares every axis present in both runs with per-axis and overall geometric means`() {
        val baseline = result(
            axes = mapOf(
                "ui" to uiAxis(ui("ui.home_scroll", 4.0)),
                "startup" to startupAxis(startup("startup.cold", 8.0)),
            ),
        )
        val current = result(
            axes = mapOf(
                "ui" to uiAxis(ui("ui.home_scroll", 2.0)),
                "startup" to startupAxis(startup("startup.cold", 2.0)),
            ),
        )

        val compared = assertIs<DiffOutcome.Compared>(compareRuns(baseline, current))

        assertEquals(listOf("startup", "ui"), compared.report.axes.map { it.axis })
        assertEquals(4.0, compared.report.axes.first { it.axis == "startup" }.geometricMean, TOLERANCE)
        assertEquals(2.0, compared.report.axes.first { it.axis == "ui" }.geometricMean, TOLERANCE)
        assertEquals(geometricMean(listOf(4.0, 2.0)), compared.report.geometricMean, TOLERANCE)
    }

    @Test
    fun `refuses runs whose scenario sets differ across axes`() {
        val baseline = result(axes = mapOf("ui" to uiAxis(ui("ui.home_scroll", 12.0))))
        val current = result(axes = mapOf("startup" to startupAxis(startup("startup.cold", 400.0))))

        val refused = assertIs<DiffOutcome.Refused>(compareRuns(baseline, current))
        val mismatch = assertIs<DiffRefusal.ScenarioSetMismatch>(refused.reason)

        assertEquals(listOf("ui:ui.home_scroll"), mismatch.baselineOnly)
        assertEquals(listOf("startup:startup.cold"), mismatch.currentOnly)
    }

    @Test
    fun `refuses runs that measured different scenarios of the same axis`() {
        val baseline = result(axes = mapOf("ui" to uiAxis(ui("ui.home_scroll", 12.0))))
        val current = result(axes = mapOf("ui" to uiAxis(ui("ui.grid_of_images_scroll", 12.0))))

        val refused = assertIs<DiffOutcome.Refused>(compareRuns(baseline, current))
        val mismatch = assertIs<DiffRefusal.ScenarioSetMismatch>(refused.reason)

        assertEquals(listOf("ui:ui.home_scroll"), mismatch.baselineOnly)
        assertEquals(listOf("ui:ui.grid_of_images_scroll"), mismatch.currentOnly)
    }

    @Test
    fun `refuses when both runs measured no scenarios`() {
        val baseline = result(axes = mapOf("ui" to uiAxis()))
        val current = result(axes = mapOf("ui" to uiAxis()))

        val refused = assertIs<DiffOutcome.Refused>(compareRuns(baseline, current))

        assertIs<DiffRefusal.EmptyScenarioSet>(refused.reason)
    }

    @Test
    fun `serializes a diff report in the shared snake_case dialect`() {
        val report = DiffReport(
            axes = listOf(
                AxisComparison(
                    axis = "ui",
                    metricLabel = "frame p50 (ms)",
                    scenarios = listOf(
                        ScenarioComparison("ui.home_scroll", baselineMetric = 12.0, currentMetric = 10.0, ratio = 1.2),
                    ),
                    geometricMean = 1.2,
                ),
            ),
            geometricMean = 1.2,
        )

        val json = PerfbenchJson.encodeToString(report)
        val back = PerfbenchJson.decodeFromString<DiffReport>(json)

        assertEquals(report, back)
        assertTrue(json.contains("\"geometric_mean\""))
        assertTrue(json.contains("\"metric_label\""))
        assertTrue(json.contains("\"baseline_metric\""))
        assertTrue(json.contains("\"current_metric\""))
    }

    private fun result(axes: Map<String, Axis>): PerfResult = PerfResult(
        generated = "2026-11-11T12:00:00+00:00",
        config = PerfConfig(scenarios = emptyList()),
        machine = DeviceFingerprint(
            manufacturer = "Google",
            model = "Pixel 8",
            androidBuild = "AP1A",
            abi = "arm64-v8a",
            thermalStatus = "none",
            appVersion = "0.1.0",
        ),
        axes = axes,
    )

    private fun uiAxis(vararg results: UiResult): UiAxis =
        UiAxis(UiAxisConfig(iterations = 5), results.toList())

    private fun startupAxis(vararg results: StartupResult): StartupAxis =
        StartupAxis(StartupAxisConfig(iterations = 5), results.toList())

    private fun ui(scenario: String, frameP50Ms: Double): UiResult = UiResult(
        scenario = scenario,
        frames = 100,
        frameP50Ms = frameP50Ms,
        frameP90Ms = frameP50Ms,
        frameP99Ms = frameP50Ms,
        jankPercent = null,
        traceSections = emptyList(),
    )

    private fun startup(scenario: String, p50Ms: Double): StartupResult = StartupResult(
        scenario = scenario,
        timeToFirstContentP50Ms = p50Ms,
        timeToFirstContentP90Ms = p50Ms,
        timeToFirstContentP99Ms = p50Ms,
        samplesMs = listOf(p50Ms),
    )

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
