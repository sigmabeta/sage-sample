package net.sigmabeta.sage.sample.feature.contentdetail

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Content detail builds its generated item from one predetermined seed, so the screen is
 * randomized in content but identical on every visit.
 */
class DemoContentDetailRepositoryTest {
    @Test
    fun `it generates stable ids and section sizes`() = runTest {
        val content = contentOf(DemoContentDetailRepository())

        assertEquals(1L, content.id)
        assertEquals(4, content.details.size)
        assertEquals(4, content.actions.size)
        assertEquals((1L..5L).toList(), content.related.map { it.id })
        assertEquals((1L..8L).toList(), content.items.map { it.id })
    }

    @Test
    fun `every generated field is populated`() = runTest {
        val content = contentOf(DemoContentDetailRepository())

        assertTrue(content.title.isNotBlank())
        assertTrue(content.details.all { it.label.isNotBlank() })
        assertTrue(content.details.all { it.value.isNotBlank() })
        assertTrue(content.actions.all { it.label.isNotBlank() })
        assertTrue(content.related.all { it.name.isNotBlank() })
        assertTrue(content.items.all { it.name.isNotBlank() })
        assertTrue(content.items.all { it.caption.isNotBlank() })
    }

    @Test
    fun `the predetermined seed makes every load identical`() = runTest {
        val repository = DemoContentDetailRepository()

        assertEquals(contentOf(repository), contentOf(repository))
    }

    @Test
    fun `a different seed changes the generated content`() = runTest {
        assertNotEquals(
            demoContentDetail(CONTENT_DETAIL_SEED),
            demoContentDetail(CONTENT_DETAIL_SEED + 1L),
        )
    }

    private suspend fun contentOf(repository: ContentDetailRepository): ContentDetail {
        val emission = repository.load().toList().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
