package net.sigmabeta.sage.sample.perfbench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ResultArtifactTest {
    @Test
    fun `each axis gets its own result file named after it`() {
        assertEquals("results-ui.json", axisResultFileName("ui"))
        assertEquals("results-startup.json", axisResultFileName("startup"))
    }

    @Test
    fun `axis result files never collide with each other or the whole-run file`() {
        assertNotEquals(axisResultFileName("ui"), axisResultFileName("startup"))
        assertNotEquals(RESULTS_FILE_NAME, axisResultFileName("ui"))
        assertNotEquals(RESULTS_FILE_NAME, axisResultFileName("startup"))
    }
}
