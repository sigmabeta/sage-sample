package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_ITEM_COUNT
import net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.demo.DemoContent
import net.sigmabeta.sage.sample.models.SampleItem

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.PaginationType
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridOfImagesStateTest {
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
    fun `the state asks the shared screen for a regular grid`() {
        assertEquals(ColumnType.Regular(160), GridOfImagesState().columnType)
    }

    @Test
    fun `loading content maps to loading rows`() {
        val rows = GridOfImagesState(
            LCE.Loading(GRID_OF_IMAGES_OPERATION_NAME),
        ).toListItems(strings)

        val loadingRows = rows.filterIsInstance<LoadingItemListModel>()
        assertTrue(loadingRows.isNotEmpty())
        assertTrue(loadingRows.all { it.loadingType == LoadingType.SQUARE })
        assertTrue(loadingRows.all { it.loadOperationName == GRID_OF_IMAGES_OPERATION_NAME })
        assertTrue(rows.none { it is GridImageListModel })
    }

    @Test
    fun `content maps to grid image rows with stable ids and generated urls`() {
        val rows = GridOfImagesState(LCE.Content(items))
            .toListItems(strings)
            .filterIsInstance<GridImageListModel>()

        assertEquals(listOf(11L, 22L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf(
                "generated://grid-of-images/11?width=256&height=256",
                "generated://grid-of-images/22?width=256&height=256",
            ),
            rows.map { it.sourceInfo.info },
        )
        rows.forEach { row -> assertEquals(Icon.Person, row.imagePlaceholder) }
        assertEquals(
            listOf(
                GridOfImagesAction.ItemClicked(ContentId("grid-of-images:11")),
                GridOfImagesAction.ItemClicked(ContentId("grid-of-images:22")),
            ),
            rows.map { it.clickAction },
        )
    }

    @Test
    fun `all fifty demo items map to one grid cell each`() {
        val demoItems = DemoContent.sampleItems(0, GRID_OF_IMAGES_ITEM_COUNT, GRID_OF_IMAGES_ITEM_COUNT)

        val rows = GridOfImagesState(LCE.Content(demoItems))
            .toListItems(strings)
            .filterIsInstance<GridImageListModel>()

        assertEquals(GRID_OF_IMAGES_ITEM_COUNT, rows.size)
        assertEquals(demoItems.map { it.id }, rows.map { it.dataId })
        assertEquals(rows.size, rows.map { it.dataId }.distinct().size)
    }

    @Test
    fun `different items map to different image sources`() {
        val rows = GridOfImagesState(LCE.Content(items))
            .toListItems(strings)
            .filterIsInstance<GridImageListModel>()

        assertNotEquals(rows[0].sourceInfo, rows[1].sourceInfo)
    }

    @Test
    fun `empty content maps to one localized empty state`() {
        val rows = GridOfImagesState(LCE.Content(emptyList())).toListItems(strings)

        val emptyState = rows.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-GRID_OF_IMAGES_EMPTY", emptyState.explanation)
        assertEquals(Icon.Album, emptyState.icon)
        assertFalse(emptyState.showCrossOut)
        assertTrue(rows.none { it is GridImageListModel })
        assertEquals(1, rows.size)
    }

    @Test
    fun `error maps to one localized error state under the grid operation`() {
        val rows = GridOfImagesState(
            LCE.Error(
                GRID_OF_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        ).toListItems(strings)

        val errorState = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(GRID_OF_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("str-GRID_OF_IMAGES_ERROR", errorState.errorString)
        assertEquals("boom", errorState.error.message)
        assertTrue(rows.none { it is GridImageListModel })
        assertEquals(1, rows.size)
    }

    @Test
    fun `uninitialized content shows loading rather than a blank grid`() {
        val rows = GridOfImagesState(LCE.Uninitialized).toListItems(strings)

        assertTrue(rows.any { it is LoadingItemListModel })
    }

    @Test
    fun `every terminal state renders at least one row`() {
        val states = listOf(
            GridOfImagesState(LCE.Loading(GRID_OF_IMAGES_OPERATION_NAME)),
            GridOfImagesState(LCE.Content(emptyList())),
            GridOfImagesState(LCE.Error(GRID_OF_IMAGES_OPERATION_NAME, IllegalStateException("boom"))),
            GridOfImagesState(LCE.Uninitialized),
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
            GridOfImagesState(LCE.Content(emptyList())),
            GridOfImagesState(LCE.Error(GRID_OF_IMAGES_OPERATION_NAME, IllegalStateException("boom"))),
        ).forEach { state ->
            val title = state.title(strings)
            assertEquals("str-GRID_OF_IMAGES_TITLE", title.title)
            assertTrue(title.shouldShowBack)
        }
    }

    @Test
    fun `the state asks SAGE for standard paging with the square loading type`() {
        val paging = GridOfImagesState().paginationType as PaginationType.Standard

        assertEquals(LoadingType.SQUARE, paging.loadingType)
        assertEquals(GRID_OF_IMAGES_PAGE_SIZE, paging.pageSize)
    }

    @Test
    fun `content appends a footer loader while another page is in flight`() {
        val rows = GridOfImagesState(
            content = LCE.Content(items),
            loadingMore = true,
        ).toListItems(strings)

        assertEquals(
            listOf(11L, 22L),
            rows.filterIsInstance<GridImageListModel>().map { it.dataId },
        )
        val loader = rows.last() as LoadingItemListModel
        assertEquals("$GRID_OF_IMAGES_OPERATION_NAME.load_more", loader.loadOperationName)
        assertEquals(LoadingType.SQUARE, loader.loadingType)
        assertEquals(rows.size, rows.map { it.dataId }.distinct().size)
    }

    @Test
    fun `prepending a page shows a header loader above the cells`() {
        val rows = GridOfImagesState(
            content = LCE.Content(items),
            windowStart = 20,
            loadingPrevious = true,
        ).toListItems(strings)

        val loader = rows.first() as LoadingItemListModel
        assertEquals("$GRID_OF_IMAGES_OPERATION_NAME.load_previous", loader.loadOperationName)
        assertEquals(
            listOf(11L, 22L),
            rows.filterIsInstance<GridImageListModel>().map { it.dataId },
        )
    }

    @Test
    fun `no boundary loader is shown when no page is in flight`() {
        val rows = GridOfImagesState(LCE.Content(items)).toListItems(strings)

        assertTrue(rows.none { it is LoadingItemListModel })
    }

    @Test
    fun `an empty window never shows a boundary loader`() {
        val rows = GridOfImagesState(
            content = LCE.Content(emptyList()),
            loadingMore = true,
            loadingPrevious = true,
        ).toListItems(strings)

        assertEquals(1, rows.size)
        assertTrue(rows.single() is EmptyStateListModel)
    }

    @Test
    fun `an error page never shows a boundary loader`() {
        val rows = GridOfImagesState(
            content = LCE.Error(GRID_OF_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
            loadingMore = true,
            loadingPrevious = true,
        ).toListItems(strings)

        assertEquals(1, rows.size)
        assertTrue(rows.single() is ErrorStateListModel)
    }
}
