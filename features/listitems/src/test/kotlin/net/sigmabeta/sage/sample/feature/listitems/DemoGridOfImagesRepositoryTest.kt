package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.ui.StringGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Grid of Images reuses the deterministic demo generator and asks it for fifty rows.
 */
class DemoGridOfImagesRepositoryTest {
    @Test
    fun `it generates fifty items with stable ids`() = runTest {
        val content = contentOf(DemoGridOfImagesRepository())

        assertEquals(50, content.size)
        assertEquals((1L..50L).toList(), content.map { it.id })
    }

    @Test
    fun `names and captions come from the shared generator`() = runTest {
        val content = contentOf(DemoGridOfImagesRepository())

        content.forEach { item ->
            val tokens = item.name.split(' ')
            assertEquals(2, tokens.size)
            assertTrue(tokens[0] in StringGenerator.FIRST_NAMES)
            assertTrue(tokens[1] in StringGenerator.LAST_NAMES)
            assertTrue(item.caption.isNotBlank())
        }
    }

    @Test
    fun `the shared seed makes every load identical`() = runTest {
        val repository = DemoGridOfImagesRepository()

        assertEquals(contentOf(repository), contentOf(repository))
    }

    @Test
    fun `the first twenty five rows match the list items repository`() = runTest {
        val listItems = contentOf(DemoSampleItemRepository())
        val gridItems = contentOf(DemoGridOfImagesRepository())

        assertEquals(listItems, gridItems.take(listItems.size))
    }

    private suspend fun contentOf(repository: GridOfImagesRepository): List<SampleItem> {
        val emission = repository.load().toList().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }

    private suspend fun contentOf(repository: SampleItemRepository): List<SampleItem> {
        val emission = repository.load().toList().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
