package net.sigmabeta.sage.sample.feature.contentdetail
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.contentItemContentId
import net.sigmabeta.sage.sample.models.relatedContentId
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_ITEM_COUNT
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.searchSeedFor
import net.sigmabeta.sage.sample.repository.demo.DemoContent
import net.sigmabeta.sage.sample.repository.demo.DemoContentDetailRepository
import net.sigmabeta.sage.sample.models.ContentDetail

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Content detail is addressed by [ContentId]: each known ID generates its own item, the same ID
 * reproduces it exactly, and an ID no surface produced is an explicit not-found.
 */
class DemoContentDetailRepositoryTest {
    private val repository = DemoContentDetailRepository()

    @Test
    fun `a known id generates populated content`() = runTest {
        val content = requireNotNull(contentOf(ContentId("list-items:7")))

        assertEquals(ContentId("list-items:7"), content.id)
        assertTrue(content.title.isNotBlank())
        assertEquals(4, content.details.size)
        assertEquals(4, content.actions.size)
        assertEquals((1L..5L).toList(), content.related.map { it.id })
        assertEquals((1L..8L).toList(), content.items.map { it.id })
        assertTrue(content.details.all { it.label.isNotBlank() })
        assertTrue(content.details.all { it.value.isNotBlank() })
        assertTrue(content.actions.all { it.label.isNotBlank() })
        assertTrue(content.related.all { it.name.isNotBlank() })
        assertTrue(content.items.all { it.name.isNotBlank() })
        assertTrue(content.items.all { it.caption.isNotBlank() })
    }

    @Test
    fun `two different ids generate different content`() = runTest {
        val first = contentOf(ContentId("list-items:7"))
        val second = contentOf(ContentId("list-items:8"))

        assertNotEquals(first, second)
        assertNotEquals(first?.title, second?.title)
    }

    @Test
    fun `the same id reproduces exactly`() = runTest {
        val id = ContentId("home:big_image_1:5")

        assertEquals(contentOf(id), contentOf(id))
    }

    @Test
    fun `list and grid details use their row's name`() = runTest {
        val item = DemoContent.sampleItems(0, 7, GRID_OF_IMAGES_ITEM_COUNT).last()

        listOf("list-items", "list-items-with-images", "grid-of-images").forEach { namespace ->
            assertEquals(
                "$namespace detail should match its row",
                item.name,
                contentOf(ContentId("$namespace:${item.id}"))?.title,
            )
        }
    }

    @Test
    fun `home and search details use their row's name`() = runTest {
        val home = requireNotNull(DemoContent.homeItem(seed = "big_image_1", index = 4))
        val seed = searchSeedFor("sample", SearchResultKind.LIST)
        val result = DemoContent.searchResultsForSeed(seed)[2]

        assertEquals(
            home.title,
            contentOf(ContentId("home:big_image_1:${home.id}"))?.title,
        )
        assertEquals(
            result.name,
            contentOf(ContentId("search:list:$seed:${result.id}"))?.title,
        )
    }

    @Test
    fun `a related row detail reuses the row it opened`() = runTest {
        val parent = requireNotNull(contentOf(ContentId("list-items:7")))
        val related = parent.related.first()
        val child = requireNotNull(contentOf(relatedContentId(parent.id, related.id)))

        assertEquals(related.name, child.title)
    }

    @Test
    fun `a content part detail reuses the row it opened`() = runTest {
        val parent = requireNotNull(contentOf(ContentId("grid-of-images:7")))
        val item = parent.items.first()
        val child = requireNotNull(contentOf(contentItemContentId(parent.id, item.id)))

        assertEquals(item.name, child.title)
    }

    @Test
    fun `a nested related row also reuses the row it opened`() = runTest {
        val parent = requireNotNull(contentOf(ContentId("list-items:7")))
        val child = requireNotNull(
            contentOf(relatedContentId(parent.id, parent.related.first().id)),
        )
        val nested = child.related.first()
        val grandchild = requireNotNull(contentOf(relatedContentId(child.id, nested.id)))

        assertEquals(nested.name, grandchild.title)
    }

    @Test
    fun `an unknown id produces the not-found result`() = runTest {
        val emission = repository.load(ContentId("stale-link:42")).toList().single()

        assertEquals(LCE.Content(null), emission)
        assertNull((emission as LCE.Content).data)
    }

    @Test
    fun `ids from every content surface are known`() = runTest {
        listOf(
            "home:big_image_1:5",
            "browse:featured",
            "search:list:-123456:3",
            "list-items:7",
            "list-items-with-images:7",
            "grid-of-images:7",
            "content-detail:related:list-items:7:4",
        ).forEach { value ->
            assertNotNull("$value should generate content", contentOf(ContentId(value)))
        }
    }

    private suspend fun contentOf(id: ContentId): ContentDetail? {
        val emission = repository.load(id).toList().single()
        assertTrue(emission is LCE.Content)
        return (emission as LCE.Content).data
    }
}
