package net.sigmabeta.sage.sample.feature.listitems

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
 * The generated repository is deterministic: one static seed produces the same rows on every
 * load, and the text comes from SAGE's `StringGenerator` rather than hard-coded literals. It reads
 * the Settings demo outcome at call time, so the empty and error states are reachable.
 */
class DemoSampleItemRepositoryTest {
    private val settings = FakeSampleSettingsManager()
    private val repository = DemoSampleItemRepository(settings)

    @Test
    fun `it generates 25 items with stable ids`() = runTest {
        val content = contentOf(repository)

        assertEquals(25, content.size)
        assertEquals((1L..25L).toList(), content.map { it.id })
    }

    @Test
    fun `names are generated person names`() = runTest {
        val content = contentOf(repository)

        content.forEach { item ->
            val tokens = item.name.split(' ')
            assertEquals(2, tokens.size)
            assertTrue(tokens[0] in StringGenerator.FIRST_NAMES)
            assertTrue(tokens[1] in StringGenerator.LAST_NAMES)
        }
    }

    @Test
    fun `captions are three to five single-line lorem words`() = runTest {
        val content = contentOf(repository)

        content.forEach { item ->
            val words = item.caption.split(' ')
            assertTrue(
                "Expected 3-5 words but was '${item.caption}'",
                words.size in 3..5,
            )
            assertTrue(
                "Caption contains a line break: '${item.caption}'",
                !item.caption.contains('\n'),
            )
            assertTrue(
                "Unexpected word in '${item.caption}'",
                words.all { it in StringGenerator.RANDOM_WORDS },
            )
        }
    }

    @Test
    fun `the static seed makes every load identical`() = runTest {
        assertEquals(contentOf(repository), contentOf(repository))
    }

    @Test
    fun `content outcome returns the generated rows`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)

        assertEquals(
            LCE.Content(demoSampleItems(25)),
            repository.load().single(),
        )
    }

    @Test
    fun `empty outcome returns a successful empty result`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(LCE.Content(emptyList<SampleItem>()), repository.load().single())
    }

    @Test
    fun `error outcome fails under the list items operation name`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)

        val emission = repository.load().single()

        assertTrue(emission is LCE.Error)
        emission as LCE.Error
        assertEquals(LIST_ITEMS_OPERATION_NAME, emission.operationName)
        assertTrue(emission.error is IllegalStateException)
    }

    @Test
    fun `changing the outcome between calls changes the next result`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)
        assertTrue(repository.load().single() is LCE.Content)

        settings.setDemoOutcome(DemoOutcome.ERROR)

        assertTrue(repository.load().single() is LCE.Error)
    }

    @Test
    fun `empty outcome reaches both list screens as one empty state`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)
        val loaded = repository.load().single()
        val strings = FakeStringProvider()

        val textRows = ListItemsState(loaded).toListItems(strings)
        val imageRows = ListItemsWithImagesState(loaded).toListItems(strings)

        assertEquals(
            "str-LIST_ITEMS_EMPTY",
            textRows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
        assertEquals(
            "str-LIST_ITEMS_WITH_IMAGES_EMPTY",
            imageRows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
    }

    @Test
    fun `error outcome reaches both list screens under their own operation names`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)
        val loaded = repository.load().single()
        val strings = FakeStringProvider()

        val textError = ListItemsState(loaded)
            .toListItems(strings)
            .filterIsInstance<ErrorStateListModel>()
            .single()
        val imageError = ListItemsWithImagesState(loaded)
            .toListItems(strings)
            .filterIsInstance<ErrorStateListModel>()
            .single()

        assertEquals(LIST_ITEMS_OPERATION_NAME, textError.failedOperationName)
        assertEquals(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, imageError.failedOperationName)
    }

    private suspend fun contentOf(repository: SampleItemRepository): List<SampleItem> {
        val emission = repository.load().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
