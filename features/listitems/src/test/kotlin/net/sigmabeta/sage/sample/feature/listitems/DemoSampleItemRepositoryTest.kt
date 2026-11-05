package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.ui.StringGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated repository is deterministic: one static seed produces the same rows on every
 * load, and the text comes from SAGE's `StringGenerator` rather than hard-coded literals.
 */
class DemoSampleItemRepositoryTest {
    @Test
    fun `it generates 25 items with stable ids`() = runTest {
        val content = contentOf(DemoSampleItemRepository())

        assertEquals(25, content.size)
        assertEquals((1L..25L).toList(), content.map { it.id })
    }

    @Test
    fun `names are generated person names`() = runTest {
        val content = contentOf(DemoSampleItemRepository())

        content.forEach { item ->
            val tokens = item.name.split(' ')
            assertEquals(2, tokens.size)
            assertTrue(tokens[0] in StringGenerator.FIRST_NAMES)
            assertTrue(tokens[1] in StringGenerator.LAST_NAMES)
        }
    }

    @Test
    fun `captions are three to five single-line lorem words`() = runTest {
        val content = contentOf(DemoSampleItemRepository())

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
        val repository = DemoSampleItemRepository()

        assertEquals(contentOf(repository), contentOf(repository))
    }

    private suspend fun contentOf(repository: SampleItemRepository): List<SampleItem> {
        val emission = repository.load().toList().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
