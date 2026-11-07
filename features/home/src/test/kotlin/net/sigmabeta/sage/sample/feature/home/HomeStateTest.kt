package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.EmptyStateListModel
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
    fun `an unstarted section contributes no rows`() {
        val rows = stateWith(LCE.Uninitialized, hasEmitted = false).toListItems(strings)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `empty demo mode shows one screen-level empty state in the stable plan`() {
        val rows = stateWithSections(stableSections(LCE.Uninitialized)).toListItems(strings)

        val empty = rows.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-HOME_EMPTY", empty.explanation)
        assertEquals(Icon.Home, empty.icon)
        assertTrue(
            rows.none {
                it is SectionHeaderListModel ||
                    it is LoadingItemListModel ||
                    it is ErrorStateListModel
            },
        )
    }

    @Test
    fun `empty content sections show one screen-level empty state in a randomized plan`() {
        val emptySection = LCE.Content(HomeModuleSection("Empty", persistentListOf()))
        val rows = stateWithSections(randomizedSections(emptySection)).toListItems(strings)

        assertEquals(
            "str-HOME_EMPTY",
            rows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
    }

    @Test
    fun `a mix of uninitialized and empty content is still empty demo mode`() {
        val rows = stateWithSections(
            listOf(
                "big_image_1" to LCE.Uninitialized,
                "grid_images_2" to LCE.Content(HomeModuleSection("Empty", persistentListOf())),
            ),
        ).toListItems(strings)

        assertEquals(
            "str-HOME_EMPTY",
            rows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
    }

    @Test
    fun `an empty section beside content contributes no rows and no screen-level empty state`() {
        val rows = stateWithSections(
            listOf(
                "big_image_1" to LCE.Content(HomeModuleSection("Empty", persistentListOf())),
                "grid_images_2" to LCE.Content(section("Gallery", gridItem(11L))),
            ),
        ).toListItems(strings)

        assertTrue(rows.none { it is EmptyStateListModel })
        assertEquals("Gallery", rows.filterIsInstance<SectionHeaderListModel>().single().title)
    }

    @Test
    fun `a plan with no modules shows the empty state instead of a blank screen`() {
        val rows = HomeState().toListItems(strings)

        assertEquals(
            "str-HOME_EMPTY",
            rows.filterIsInstance<EmptyStateListModel>().single().explanation,
        )
    }

    @Test
    fun `loading sections render placeholders instead of the empty state`() {
        val rows = stateWithSections(stableSections(LCE.Loading("home.module.load")))
            .toListItems(strings)

        assertTrue(rows.none { it is EmptyStateListModel })
        assertTrue(rows.filterIsInstance<LoadingItemListModel>().isNotEmpty())
    }

    @Test
    fun `a plan that has not emitted yet does not show the empty state`() {
        val rows = stateWithSections(
            stableSections(LCE.Uninitialized),
            hasEmitted = false,
        ).toListItems(strings)

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
    fun `all modules failing collapse to one screen-level error in the stable plan`() {
        val rows = stateWithSections(
            listOf(
                "big_image_1" to LCE.Error("home.big_image_1.load", IllegalStateException("hero")),
                "grid_images_2" to LCE.Error("home.grid_images_2.load", IllegalStateException("grid")),
            ),
        ).toListItems(strings)

        val error = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals("str-HOME_ALL_ERROR", error.errorString)
        assertEquals("hero", error.error.message)
    }

    @Test
    fun `all modules failing collapse to one screen-level error in a randomized plan`() {
        val sections = (1..10).map { index ->
            "module_$index" to LCE.Error(
                "home.module_$index.load",
                IllegalStateException("failure $index"),
            )
        }
        val rows = stateWithSections(sections).toListItems(strings)

        assertEquals(
            "str-HOME_ALL_ERROR",
            rows.filterIsInstance<ErrorStateListModel>().single().errorString,
        )
    }

    @Test
    fun `a loading sibling keeps errors inline instead of collapsing the screen`() {
        val rows = stateWithSections(
            listOf(
                "big_image_1" to LCE.Error("home.big_image_1.load", IllegalStateException("hero")),
                "grid_images_2" to LCE.Loading("home.grid_images_2.load"),
            ),
        ).toListItems(strings)

        assertEquals(
            "home.big_image_1.load",
            rows.filterIsInstance<ErrorStateListModel>().single().failedOperationName,
        )
        assertTrue(rows.filterIsInstance<LoadingItemListModel>().isNotEmpty())
    }

    @Test
    fun `partial errors stay inline and preserve successful siblings`() {
        val failure = IllegalStateException("boom")
        val rows = stateWithSections(
            listOf(
                "big_image_1" to LCE.Error("home.big_image_1.load", failure),
                "grid_images_2" to LCE.Content(section("Gallery", gridItem(11L), gridItem(22L))),
            ),
        ).toListItems(strings)

        val error = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals("home.big_image_1.load", error.failedOperationName)
        assertEquals(failure, error.error)

        assertEquals("Gallery", rows.filterIsInstance<SectionHeaderListModel>().single().title)
        val gallery = rows.filterIsInstance<HorizontalScrollerListModel>().single()
        assertEquals(2, gallery.scrollingItems.filterIsInstance<GridImageListModel>().size)
        assertTrue(rows.none { it is EmptyStateListModel })
    }

    @Test
    fun `combined rows from every section have unique data ids`() {
        val state = HomeState(
            sections = persistentListOf(
                HomeSectionState(
                    "hero",
                    0,
                    LCE.Content(section("Hero", bigImageItem())),
                    hasEmitted = true,
                ),
                HomeSectionState(
                    "gallery",
                    1,
                    LCE.Content(
                        HomeModuleSection(
                            "Gallery",
                            persistentListOf(gridItem(11L), gridItem(22L)),
                        ),
                    ),
                    hasEmitted = true,
                ),
            ),
        )

        val rows = state.toActual(strings).listItems

        assertTrue(rows.none { it is ErrorStateListModel })
        assertEquals(rows.size, rows.map { it.dataId }.toSet().size)
    }

    private fun stateWith(
        lce: LCE<HomeModuleSection>,
        hasEmitted: Boolean = true,
    ) = HomeState(
        sections = persistentListOf(
            HomeSectionState("gallery", 0, lce, hasEmitted = hasEmitted),
        ),
    )

    private fun stateWithSections(
        sections: List<Pair<String, LCE<HomeModuleSection>>>,
        hasEmitted: Boolean = true,
    ) = HomeState(
        sections = sections.mapIndexed { index, (id, lce) ->
            HomeSectionState(id, index, lce, hasEmitted = hasEmitted)
        }.toImmutableList(),
    )

    /** One section per Home definition, as the stable factory plan produces. */
    private fun stableSections(lce: LCE<HomeModuleSection>): List<Pair<String, LCE<HomeModuleSection>>> =
        listOf("big_image_1" to lce, "grid_images_2" to lce)

    /** A plan slot-per-module list covering the randomized plan's 2–10 instances. */
    private fun randomizedSections(
        lce: LCE<HomeModuleSection>,
    ): List<Pair<String, LCE<HomeModuleSection>>> =
        (1..10).map { "module_$it" to lce }

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
