package net.sigmabeta.sage.sample.perfbench

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfResultSerializationTest {
    @Test
    fun `round-trips a fully populated result`() {
        val result = PerfResult(
            generated = "2026-11-11T12:00:00+00:00",
            config = PerfConfig(scenarios = listOf("ui.home_scroll", "startup.cold")),
            machine = fingerprint(),
            axes = mapOf(
                "ui" to UiAxis(
                    config = UiAxisConfig(iterations = 5),
                    results = listOf(
                        UiResult(
                            scenario = "ui.home_scroll",
                            frames = 240,
                            frameP50Ms = 8.0,
                            frameP90Ms = 12.0,
                            frameP99Ms = 30.0,
                            jankPercent = 4.5,
                            traceSections = listOf(
                                TraceSectionSum(name = "GridImage", sumMs = 120.0, count = 240),
                            ),
                        ),
                    ),
                ),
                "startup" to StartupAxis(
                    config = StartupAxisConfig(iterations = 5),
                    results = listOf(
                        StartupResult(
                            scenario = "startup.cold",
                            timeToFirstContentP50Ms = 320.0,
                            timeToFirstContentP90Ms = 410.0,
                            timeToFirstContentP99Ms = 500.0,
                            samplesMs = listOf(300.0, 320.0, 410.0),
                        ),
                    ),
                ),
            ),
        )

        val back = PerfbenchJson.decodeFromString<PerfResult>(PerfbenchJson.encodeToString(result))

        assertEquals(result, back)
    }

    @Test
    fun `writes the snake_case schema keys and axis discriminator`() {
        val json = PerfbenchJson.encodeToString(minimalResult())

        assertTrue(json.contains("\"class\": \"ui\""))
        assertTrue(json.contains("\"class\": \"startup\""))
        assertTrue(json.contains("\"android_build\""))
        assertTrue(json.contains("\"thermal_status\""))
        assertTrue(json.contains("\"app_version\""))
        assertTrue(json.contains("\"frame_p50_ms\""))
        assertTrue(json.contains("\"jank_percent\""))
        assertTrue(json.contains("\"trace_sections\""))
        assertTrue(json.contains("\"time_to_first_content_p50_ms\""))
        assertTrue(json.contains("\"samples_ms\""))
    }

    @Test
    fun `keeps each axis subclass through the map`() {
        val back = PerfbenchJson.decodeFromString<PerfResult>(PerfbenchJson.encodeToString(minimalResult()))

        assertIs<UiAxis>(back.axes["ui"])
        assertIs<StartupAxis>(back.axes["startup"])
    }

    @Test
    fun `carries no corpus block`() {
        val root = PerfbenchJson.parseToJsonElement(PerfbenchJson.encodeToString(minimalResult())).jsonObject

        assertFalse(root.containsKey("corpus"))
    }

    @Test
    fun `no axis carries a score`() {
        val axes = PerfbenchJson.parseToJsonElement(PerfbenchJson.encodeToString(minimalResult()))
            .jsonObject.getValue("axes").jsonObject

        axes.forEach { (axis, element) ->
            assertFalse("axis $axis still carries a score", element.jsonObject.containsKey("score"))
        }
    }

    private fun minimalResult(): PerfResult = PerfResult(
        generated = "2026-11-11T12:00:00+00:00",
        config = PerfConfig(scenarios = listOf("ui.home_scroll", "startup.cold")),
        machine = fingerprint(),
        axes = mapOf(
            "ui" to UiAxis(
                config = UiAxisConfig(iterations = 5),
                results = listOf(
                    UiResult("ui.home_scroll", 100, 8.0, 12.0, 30.0, 4.5, emptyList()),
                ),
            ),
            "startup" to StartupAxis(
                config = StartupAxisConfig(iterations = 5),
                results = listOf(StartupResult("startup.cold", 320.0, 410.0, 500.0, listOf(300.0, 320.0, 410.0))),
            ),
        ),
    )

    private fun fingerprint(): DeviceFingerprint = DeviceFingerprint(
        manufacturer = "Google",
        model = "Pixel 8",
        androidBuild = "AP1A.240505.004",
        abi = "arm64-v8a",
        thermalStatus = "none",
        appVersion = "0.1.0",
    )
}
