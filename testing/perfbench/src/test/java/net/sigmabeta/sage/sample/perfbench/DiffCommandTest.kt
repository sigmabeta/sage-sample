package net.sigmabeta.sage.sample.perfbench

import java.io.File
import java.nio.file.Files
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffCommandTest {
    @Test
    fun `reads a compatible result file`() {
        val file = tempResult(result(uiP50 = 10.0))

        assertTrue(readResult(file) is ResultRead.Decoded)
    }

    @Test
    fun `turns an unreadable file into a refusal instead of throwing`() {
        val file = Files.createTempFile("perfbench", ".json").toFile().apply { writeText("not json") }

        assertTrue(readResult(file) is ResultRead.Incompatible)
    }

    private fun result(uiP50: Double): PerfResult = PerfResult(
        generated = "2026-11-11T12:00:00+00:00",
        config = PerfConfig(scenarios = listOf("ui.home_scroll")),
        machine = DeviceFingerprint("Google", "Pixel 8", "AP1A", "arm64-v8a", "none", "0.1.0"),
        axes = mapOf(
            "ui" to UiAxis(
                config = UiAxisConfig(iterations = 5),
                results = listOf(UiResult("ui.home_scroll", 100, uiP50, uiP50, uiP50, null, emptyList())),
            ),
        ),
    )

    private fun tempResult(result: PerfResult): File =
        Files.createTempFile("perfbench", ".json").toFile().apply {
            writeText(PerfbenchJson.encodeToString(PerfResult.serializer(), result))
        }
}
