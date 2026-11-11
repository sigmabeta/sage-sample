package net.sigmabeta.sage.sample.perfbench

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffArgsTest {
    @Test
    fun `parses the required run paths and defaults json off`() {
        val args = parseDiffArgs(listOf("--a", "/tmp/a.json", "--b", "/tmp/b.json"))

        assertEquals(File("/tmp/a.json"), args.baseline)
        assertEquals(File("/tmp/b.json"), args.current)
        assertFalse(args.json)
    }

    @Test
    fun `parses the json flag`() {
        assertTrue(parseDiffArgs(listOf("--a", "a", "--b", "b", "--json")).json)
    }

    @Test
    fun `rejects a missing or valueless run path`() {
        assertThrows(IllegalArgumentException::class.java) { parseDiffArgs(listOf("--a", "a")) }
        assertThrows(IllegalArgumentException::class.java) { parseDiffArgs(listOf("--a", "--b", "b")) }
    }

    @Test
    fun `rejects an unknown option`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseDiffArgs(listOf("--a", "a", "--b", "b", "--verbose"))
        }
    }
}
