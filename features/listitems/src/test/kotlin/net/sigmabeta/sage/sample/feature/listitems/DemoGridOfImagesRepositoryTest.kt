package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.ui.StringGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Grid of Images reuses the deterministic demo generator and asks it for fifty rows. Like List
 * Items, it reads the Settings demo outcome at call time.
 */
class DemoGridOfImagesRepositoryTest {
    private val settings = FakeSampleSettingsManager()
    private val repository = DemoGridOfImagesRepository(settings)

    @Test
    fun `it generates fifty items with stable ids`() = runTest {
        val content = contentOf(repository)

        assertEquals(50, content.size)
        assertEquals((1L..50L).toList(), content.map { it.id })
    }

    @Test
    fun `names and captions come from the shared generator`() = runTest {
        val content = contentOf(repository)

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
        assertEquals(contentOf(repository), contentOf(repository))
    }

    @Test
    fun `the first twenty five rows match the list items repository`() = runTest {
        val listItems = contentOf(DemoSampleItemRepository(FakeSampleSettingsManager()))
        val gridItems = contentOf(repository)

        assertEquals(listItems, gridItems.take(listItems.size))
    }

    @Test
    fun `content outcome returns the generated rows`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)

        assertEquals(
            LCE.Content(demoSampleItems(GRID_OF_IMAGES_ITEM_COUNT)),
            repository.load().single(),
        )
    }

    @Test
    fun `empty outcome returns a successful empty result`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(LCE.Content(emptyList<SampleItem>()), repository.load().single())
    }

    @Test
    fun `error outcome fails under the grid operation name`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)

        val emission = repository.load().single()

        assertTrue(emission is LCE.Error)
        emission as LCE.Error
        assertEquals(GRID_OF_IMAGES_OPERATION_NAME, emission.operationName)
        assertTrue(emission.error is IllegalStateException)
    }

    @Test
    fun `changing the outcome between calls changes the next result`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)
        assertTrue(repository.load().single() is LCE.Content)

        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(LCE.Content(emptyList<SampleItem>()), repository.load().single())
    }

    @Test
    fun `a window returns the requested slice of the deterministic set`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)
        val all = contentOf(repository)

        assertEquals(all.subList(20, 40), contentOf(repository, offset = 20, limit = 20))
        assertEquals(all.subList(40, 50), contentOf(repository, offset = 40, limit = 20))
    }

    @Test
    fun `a window starting past the end is an empty page`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)

        assertEquals(
            emptyList<SampleItem>(),
            contentOf(repository, offset = GRID_OF_IMAGES_ITEM_COUNT, limit = 20),
        )
    }

    @Test
    fun `empty and error outcomes apply to window loads too`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)
        assertEquals(
            LCE.Content(emptyList<SampleItem>()),
            repository.load(offset = 20, limit = 5).single(),
        )

        settings.setDemoOutcome(DemoOutcome.ERROR)
        assertTrue(repository.load(offset = 20, limit = 5).single() is LCE.Error)
    }

    @Test
    fun `empty outcome reaches the grid as one empty state`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)

        val rows = GridOfImagesState(repository.load().single())
            .toListItems(FakeStringProvider())

        assertEquals(
            "str-GRID_OF_IMAGES_EMPTY",
            rows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
    }

    @Test
    fun `error outcome reaches the grid under its operation name`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)

        val error = GridOfImagesState(repository.load().single())
            .toListItems(FakeStringProvider())
            .filterIsInstance<ErrorStateListModel>()
            .single()

        assertEquals(GRID_OF_IMAGES_OPERATION_NAME, error.failedOperationName)
    }

    private suspend fun contentOf(
        repository: GridOfImagesRepository,
        offset: Int = 0,
        limit: Int = GRID_OF_IMAGES_ITEM_COUNT,
    ): List<SampleItem> {
        val emission = repository.load(offset, limit).single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }

    private suspend fun contentOf(repository: SampleItemRepository): List<SampleItem> {
        val emission = repository.load().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
