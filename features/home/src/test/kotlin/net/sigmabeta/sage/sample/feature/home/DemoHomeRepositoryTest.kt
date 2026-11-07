package net.sigmabeta.sage.sample.feature.home

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.settings.DemoOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated Home datasource honors the Settings demo outcome at call time, so both terminal
 * screen states are reachable without source changes.
 */
class DemoHomeRepositoryTest {
    private val settings = FakeHomeSettingsManager()
    private val repository = DemoHomeRepository(settings)

    @Test
    fun `content outcome generates a catalog`() {
        settings.setDemoOutcome(DemoOutcome.CONTENT)

        assertEquals(18, repository.itemCount("big_image_1"))
        assertNotNull(repository.itemAt("big_image_1", 0))
    }

    @Test
    fun `empty outcome yields no items`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(0, repository.itemCount("big_image_1"))
        assertNull(repository.itemAt("big_image_1", 0))
        assertEquals(
            LCE.Content(emptyList<HomeItem>()),
            repository.gridImages("grid_images_2", limit = 5).single(),
        )
    }

    @Test
    fun `error outcome fails every call`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)

        assertThrows(IllegalStateException::class.java) {
            repository.itemCount("big_image_1")
        }
        assertThrows(IllegalStateException::class.java) {
            repository.itemAt("big_image_1", 0)
        }
        assertTrue(repository.gridImages("grid_images_2", limit = 5).single() is LCE.Error)
    }

    @Test
    fun `the outcome is read at call time, not cached at construction`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)
        assertEquals(18, repository.itemCount("big_image_1"))

        settings.setDemoOutcome(DemoOutcome.EMPTY)
        assertEquals(0, repository.itemCount("big_image_1"))

        settings.setDemoOutcome(DemoOutcome.ERROR)
        assertTrue(repository.gridImages("grid_images_2", limit = 5).single() is LCE.Error)
    }
}
