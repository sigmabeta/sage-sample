package net.sigmabeta.sage.sample.perfbench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiReportTest {
    @Test
    fun `percentile interpolates between the two bracketing samples`() {
        val sorted = listOf(10.0, 20.0, 30.0, 40.0)

        assertEquals(25.0, percentile(sorted, 50), TOLERANCE)
        assertEquals(37.0, percentile(sorted, 90), TOLERANCE)
        assertEquals(39.7, percentile(sorted, 99), TOLERANCE)
    }

    @Test
    fun `jank percent is the share of frames that missed their deadline`() {
        assertEquals(50.0, jankPercent(listOf(1.0, -2.0, 0.0, 3.0)), TOLERANCE)
        assertEquals(0.0, jankPercent(listOf(-1.0, -2.0)), TOLERANCE)
        assertEquals(100.0, jankPercent(listOf(0.5)), TOLERANCE)
    }

    @Test
    fun `ui result reports frame percentiles, frame count, jank, and trace sums`() {
        val result = uiResult(
            UiMeasurement(
                scenario = "ui.home_scroll",
                frameCpuMillis = listOf(10.0, 20.0, 30.0, 40.0),
                frameOverrunMillis = listOf(1.0, -2.0, 0.0, 3.0),
                traceSections = listOf(
                    TraceSectionMeasurement(name = "GridImage", sumMs = 12.5, count = 7),
                    TraceSectionMeasurement(name = "CrossfadeImage", sumMs = 8.0, count = 7),
                ),
            ),
        )

        assertEquals("ui.home_scroll", result.scenario)
        assertEquals(4, result.frames)
        assertEquals(25.0, result.frameP50Ms, TOLERANCE)
        assertEquals(37.0, result.frameP90Ms, TOLERANCE)
        assertEquals(39.7, result.frameP99Ms, TOLERANCE)
        assertEquals(50.0, result.jankPercent!!, TOLERANCE)
        assertEquals(
            listOf(
                TraceSectionSum(name = "GridImage", sumMs = 12.5, count = 7),
                TraceSectionSum(name = "CrossfadeImage", sumMs = 8.0, count = 7),
            ),
            result.traceSections,
        )
    }

    @Test
    fun `jank percent is null when no overrun samples were captured`() {
        val result = uiResult(
            UiMeasurement(
                scenario = "ui.home_scroll",
                frameCpuMillis = listOf(5.0),
                frameOverrunMillis = emptyList(),
            ),
        )

        assertNull(result.jankPercent)
    }

    @Test
    fun `ui scenario ids are stable and resolvable`() {
        assertEquals(
            listOf(
                "ui.home_scroll",
                "ui.list_items_scroll",
                "ui.grid_of_images_scroll",
                "ui.content_detail_scroll",
            ),
            UiScenario.all.map { it.id },
        )
        assertEquals(UiScenario.HOME_SCROLL, UiScenario.fromId("ui.home_scroll"))
        assertEquals(UiScenario.GRID_OF_IMAGES_SCROLL, UiScenario.fromId("ui.grid_of_images_scroll"))
        assertNull(UiScenario.fromId("ui.unknown"))
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
