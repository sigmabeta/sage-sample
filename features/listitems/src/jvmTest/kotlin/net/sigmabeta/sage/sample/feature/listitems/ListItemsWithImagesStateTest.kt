package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.list.PaginationType
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemsWithImagesStateTest {
    private val strings = FakeStringProvider()

    private val items = listOf(
        SampleItem(
            id = 11L,
            name = "Sleve McDichael",
            caption = "Fermentum pellentesque iaculis",
        ),
        SampleItem(
            id = 22L,
            name = "Exie Clayton",
            caption = "Lorem ipsum dolor",
        ),
    )

    @Test
    fun `loading content maps to loading rows`() {
        val rows = ListItemsWithImagesState(
            LCE.Loading(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME),
        ).toListItems(strings)

        val loadingRows = rows.filterIsInstance<LoadingItemListModel>()
        assertTrue(loadingRows.isNotEmpty())
        assertTrue(loadingRows.all { it.loadingType == LoadingType.TEXT_CAPTION_IMAGE })
        assertTrue(loadingRows.all { it.loadOperationName == LIST_ITEMS_WITH_IMAGES_OPERATION_NAME })
        assertTrue(rows.none { it is ImageNameCaptionListModel })
    }

    @Test
    fun `content maps to image name caption rows with stable ids and generated urls`() {
        val rows = ListItemsWithImagesState(LCE.Content(items))
            .toListItems(strings)
            .filterIsInstance<ImageNameCaptionListModel>()

        assertEquals(listOf(11L, 22L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf("Fermentum pellentesque iaculis", "Lorem ipsum dolor"),
            rows.map { it.caption },
        )
        assertEquals(
            listOf(
                "generated://list-items-with-images/11?width=128&height=128",
                "generated://list-items-with-images/22?width=128&height=128",
            ),
            rows.map { it.sourceInfo.info },
        )
        rows.forEach { row -> assertEquals(Icon.Person, row.imagePlaceholder) }
        assertEquals(
            listOf(11L, 22L),
            rows.map {
                (it.clickAction as ListItemsWithImagesAction.ItemClicked).item.id
            },
        )
    }

    @Test
    fun `the same item always maps to the same image source`() {
        assertEquals(imageSourceFor(items[0]), imageSourceFor(items[0]))
    }

    @Test
    fun `different items map to different image sources`() {
        assertNotEquals(imageSourceFor(items[0]), imageSourceFor(items[1]))
    }

    @Test
    fun `empty content maps to one localized empty state`() {
        val rows = ListItemsWithImagesState(LCE.Content(emptyList())).toListItems(strings)

        val emptyState = rows.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-LIST_ITEMS_WITH_IMAGES_EMPTY", emptyState.explanation)
        assertEquals(Icon.Person, emptyState.icon)
        assertFalse(emptyState.showCrossOut)
        assertTrue(rows.none { it is ImageNameCaptionListModel })
        assertEquals(1, rows.size)
    }

    @Test
    fun `error maps to one localized error state under the images operation`() {
        val rows = ListItemsWithImagesState(
            LCE.Error(
                LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        ).toListItems(strings)

        val errorState = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("str-LIST_ITEMS_WITH_IMAGES_ERROR", errorState.errorString)
        assertEquals("boom", errorState.error.message)
        assertTrue(rows.none { it is ImageNameCaptionListModel })
        assertEquals(1, rows.size)
    }

    @Test
    fun `uninitialized content shows loading rather than a blank list`() {
        val rows = ListItemsWithImagesState(LCE.Uninitialized).toListItems(strings)

        assertTrue(rows.any { it is LoadingItemListModel })
    }

    @Test
    fun `every terminal state renders at least one row`() {
        val states = listOf(
            ListItemsWithImagesState(LCE.Loading(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME)),
            ListItemsWithImagesState(LCE.Content(emptyList())),
            ListItemsWithImagesState(
                LCE.Error(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
            ),
            ListItemsWithImagesState(LCE.Uninitialized),
        )

        states.forEach { state ->
            val rows = state.toListItems(strings)
            assertTrue("$state rendered no rows", rows.isNotEmpty())
            assertEquals(rows.size, rows.map { it.dataId }.distinct().size)
        }
    }

    @Test
    fun `empty and error keep the screen title and back`() {
        listOf(
            ListItemsWithImagesState(LCE.Content(emptyList())),
            ListItemsWithImagesState(
                LCE.Error(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
            ),
        ).forEach { state ->
            val title = state.title(strings)
            assertEquals("str-LIST_ITEMS_WITH_IMAGES_TITLE", title.title)
            assertTrue(title.shouldShowBack)
        }
    }

    @Test
    fun `the state asks SAGE for standard paging with the images loading type`() {
        val paging = ListItemsWithImagesState().paginationType as PaginationType.Standard

        assertEquals(LoadingType.TEXT_CAPTION_IMAGE, paging.loadingType)
        assertEquals(LIST_ITEMS_WITH_IMAGES_PAGE_SIZE, paging.pageSize)
    }

    @Test
    fun `content appends a footer loader while another page is in flight`() {
        val rows = ListItemsWithImagesState(
            content = LCE.Content(items),
            loadingMore = true,
        ).toListItems(strings)

        assertEquals(
            listOf(11L, 22L),
            rows.filterIsInstance<ImageNameCaptionListModel>().map { it.dataId },
        )
        val loader = rows.last() as LoadingItemListModel
        assertEquals("$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_more", loader.loadOperationName)
        assertEquals(LoadingType.TEXT_CAPTION_IMAGE, loader.loadingType)
        assertEquals(rows.size, rows.map { it.dataId }.distinct().size)
    }

    @Test
    fun `prepending a page shows a header loader above the rows`() {
        val rows = ListItemsWithImagesState(
            content = LCE.Content(items),
            windowStart = 10,
            loadingPrevious = true,
        ).toListItems(strings)

        val loader = rows.first() as LoadingItemListModel
        assertEquals("$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_previous", loader.loadOperationName)
        assertEquals(
            listOf(11L, 22L),
            rows.filterIsInstance<ImageNameCaptionListModel>().map { it.dataId },
        )
    }

    @Test
    fun `no boundary loader is shown when no page is in flight`() {
        val rows = ListItemsWithImagesState(LCE.Content(items)).toListItems(strings)

        assertTrue(rows.none { it is LoadingItemListModel })
    }

    @Test
    fun `an empty window never shows a boundary loader`() {
        val rows = ListItemsWithImagesState(
            content = LCE.Content(emptyList()),
            loadingMore = true,
            loadingPrevious = true,
        ).toListItems(strings)

        assertEquals(1, rows.size)
        assertTrue(rows.single() is EmptyStateListModel)
    }

    @Test
    fun `an error page never shows a boundary loader`() {
        val rows = ListItemsWithImagesState(
            content = LCE.Error(
                LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
            loadingMore = true,
            loadingPrevious = true,
        ).toListItems(strings)

        assertEquals(1, rows.size)
        assertTrue(rows.single() is ErrorStateListModel)
    }
}
