package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.persistentListOf
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `a single-item section renders its header and item without a scroller`() {
        val rows = stateWith(LCE.Content(section("Hero", bigImageItem()))).toListItems(strings)

        assertEquals(listOf("Hero"), rows.filterIsInstance<SectionHeaderListModel>().map { it.title })
        assertEquals(1, rows.filterIsInstance<GridImageListModel>().size)
        assertTrue(rows.none { it is HorizontalScrollerListModel })
    }

    @Test
    fun `a multi-item section wraps its items in a stable horizontal scroller`() {
        val items = persistentListOf<ListModel>(gridItem(11L), gridItem(22L))
        val rows = stateWith(LCE.Content(HomeModuleSection("Gallery", items))).toListItems(strings)

        val scroller = rows.filterIsInstance<HorizontalScrollerListModel>().single()
        assertEquals("home.gallery.scroller".hashCode().toLong(), scroller.dataId)
        assertEquals(items, scroller.scrollingItems)
        assertEquals("Gallery", rows.filterIsInstance<SectionHeaderListModel>().single().title)
    }

    @Test
    fun `an uninitialized section contributes no rows`() {
        val rows = stateWith(LCE.Uninitialized).toListItems(strings)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `a content section with no items contributes no rows`() {
        val rows = stateWith(LCE.Content(HomeModuleSection("Empty", persistentListOf())))
            .toListItems(strings)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `a loading section renders headed cover placeholders in a scroller`() {
        val rows = stateWith(LCE.Loading("home.gallery.load")).toListItems(strings)

        val header = rows.filterIsInstance<LoadingItemListModel>()
            .single { it.loadingType == LoadingType.SECTION_HEADER }
        assertEquals("home.gallery.load.section.header", header.loadOperationName)

        val scroller = rows.filterIsInstance<HorizontalScrollerListModel>().single()
        val placeholders = scroller.scrollingItems.filterIsInstance<LoadingItemListModel>()
        assertEquals(6, placeholders.size)
        assertTrue(placeholders.all { it.loadingType == LoadingType.COVER })
        assertTrue(placeholders.all { it.loadOperationName == "home.gallery.load" })
    }

    @Test
    fun `an erroring section renders its own error row`() {
        val failure = IllegalStateException("boom")
        val rows = stateWith(LCE.Error("home.gallery.load", failure)).toListItems(strings)

        val error = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals("home.gallery.load", error.failedOperationName)
        assertEquals(failure, error.error)
    }

    @Test
    fun `combined rows from every section have unique data ids`() {
        val state = HomeState(
            sections = persistentListOf(
                HomeSectionState("hero", 0, LCE.Content(section("Hero", bigImageItem()))),
                HomeSectionState(
                    "gallery",
                    1,
                    LCE.Content(
                        HomeModuleSection(
                            "Gallery",
                            persistentListOf(gridItem(11L), gridItem(22L)),
                        ),
                    ),
                ),
            ),
        )

        val rows = state.toActual(strings).listItems

        assertTrue(rows.none { it is ErrorStateListModel })
        assertEquals(rows.size, rows.map { it.dataId }.toSet().size)
    }

    private fun stateWith(lce: LCE<HomeModuleSection>) = HomeState(
        sections = persistentListOf(HomeSectionState("gallery", 0, lce)),
    )

    private fun section(title: String, vararg items: ListModel) = HomeModuleSection(
        title = title,
        items = persistentListOf(*items),
    )

    private fun bigImageItem() = GridImageListModel(
        dataId = 5L,
        name = "Hero item",
        sourceInfo = SourceInfo("generated://home/1"),
        imagePlaceholder = Icon.Album,
        clickAction = SageAction.Noop,
        aspectRatio = 0.75f,
        maxWidthDp = 400f,
    )

    private fun gridItem(id: Long) = GridImageListModel(
        dataId = id,
        name = "Item $id",
        sourceInfo = SourceInfo("generated://home/$id"),
        imagePlaceholder = Icon.Album,
        clickAction = SageAction.Noop,
    )
}
