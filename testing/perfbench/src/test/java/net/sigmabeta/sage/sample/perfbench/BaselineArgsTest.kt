package net.sigmabeta.sage.sample.perfbench

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaselineArgsTest {
    @Test
    fun `splits the recordings in order and keeps the output path`() {
        val a = tempFile()
        val b = tempFile()

        val args = parseBaselineArgs(listOf("--runs", "${a.path}, ${b.path}", "--out", "out.json"))

        assertEquals(listOf(a, b), args.recordings)
        assertEquals(File("out.json"), args.outFile)
    }

    @Test
    fun `rejects a missing recording`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBaselineArgs(listOf("--runs", "/no/such/results.json", "--out", "out.json"))
        }
    }

    @Test
    fun `requires both runs and out`() {
        assertThrows(IllegalArgumentException::class.java) { parseBaselineArgs(listOf("--out", "out.json")) }
        assertThrows(IllegalArgumentException::class.java) { parseBaselineArgs(listOf("--runs", tempFile().path)) }
    }

    @Test
    fun `rejects an unknown option`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBaselineArgs(listOf("--runs", tempFile().path, "--out", "out.json", "--json", "yes"))
        }
    }

    @Test
    fun `formats the spread relative to the median`() {
        assertEquals("7.00 / 8.00 / 9.00 (25.0%)", spreadLine(listOf(9.0, 7.0, 8.0)))
    }

    private fun tempFile(): File =
        Files.createTempFile("perfbench-baseline", ".json").toFile().apply { deleteOnExit() }
}
