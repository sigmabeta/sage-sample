package net.sigmabeta.sage.sample.perfbench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StartupReportTest {
    @Test
    fun `startup result reports time-to-first-content percentiles and the samples`() {
        val result = startupResult(
            StartupMeasurement(
                scenario = "startup.cold",
                timeToFirstContentMillis = listOf(10.0, 20.0, 30.0, 40.0),
            ),
        )

        assertEquals("startup.cold", result.scenario)
        assertEquals(25.0, result.timeToFirstContentP50Ms, TOLERANCE)
        assertEquals(37.0, result.timeToFirstContentP90Ms, TOLERANCE)
        assertEquals(39.7, result.timeToFirstContentP99Ms, TOLERANCE)
        assertEquals(listOf(10.0, 20.0, 30.0, 40.0), result.samplesMs)
    }

    @Test
    fun `a single sample is every percentile`() {
        val result = startupResult(StartupMeasurement("startup.warm", listOf(123.4)))

        assertEquals(123.4, result.timeToFirstContentP50Ms, TOLERANCE)
        assertEquals(123.4, result.timeToFirstContentP90Ms, TOLERANCE)
        assertEquals(123.4, result.timeToFirstContentP99Ms, TOLERANCE)
    }

    @Test
    fun `a startup result requires at least one sample`() {
        assertThrows(IllegalArgumentException::class.java) {
            startupResult(StartupMeasurement("startup.cold", emptyList()))
        }
    }

    @Test
    fun `startup scenario ids are stable and resolvable`() {
        assertEquals(listOf("startup.cold", "startup.warm"), StartupScenario.all.map { it.id })
        assertEquals(StartupScenario.COLD, StartupScenario.fromId("startup.cold"))
        assertEquals(StartupScenario.WARM, StartupScenario.fromId("startup.warm"))
        assertNull(StartupScenario.fromId("ui.home_scroll"))
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
