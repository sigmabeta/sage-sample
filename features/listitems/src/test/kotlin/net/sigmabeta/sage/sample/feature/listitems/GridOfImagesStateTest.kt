package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
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

        assertTrue(rows.any { it is LoadingItemListModel })
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
            listOf(11L, 22L),
            rows.map { (it.clickAction as GridOfImagesAction.ItemClicked).item.id },
        )
    }

    @Test
    fun `all fifty demo items map to one grid cell each`() {
        val demoItems = demoSampleItems(GRID_OF_IMAGES_ITEM_COUNT)

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
    fun `error maps to an error state`() {
        val rows = GridOfImagesState(
            LCE.Error(
                GRID_OF_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        ).toListItems(strings)

        val errorState = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(GRID_OF_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }
}
