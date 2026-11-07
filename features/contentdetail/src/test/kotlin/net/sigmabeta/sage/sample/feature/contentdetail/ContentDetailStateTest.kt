package net.sigmabeta.sage.sample.feature.contentdetail

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.CtaListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.HeroImageListModel
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SectionListModel
import net.sigmabeta.sage.components.WideItemListModel
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentDetailStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `loading content maps every section to its loading shape`() {
        val sections = ContentDetailState().toListItems(strings).sections()

        assertEquals(5, sections.size)

        val hero = sections[0].sectionItems.single() as LoadingItemListModel
        assertEquals(LoadingType.BIG_IMAGE, hero.loadingType)

        assertTrue(sections[1].sectionItems.isEmpty())
        assertTrue(sections[2].sectionItems.isEmpty())

        val related = sections[3].sectionItems
        val relatedScroller = related.filterIsInstance<HorizontalScrollerListModel>().single()
        val relatedLoading = relatedScroller.scrollingItems
            .filterIsInstance<LoadingItemListModel>()
        assertTrue(relatedLoading.isNotEmpty())
        assertTrue(relatedLoading.all { it.loadingType == LoadingType.WIDE_ITEM })

        val items = sections[4].sectionItems
        assertEquals(
            LoadingType.SECTION_HEADER,
            items.filterIsInstance<LoadingItemListModel>().first().loadingType,
        )
        assertEquals(
            8,
            items.filterIsInstance<LoadingItemListModel>()
                .count { it.loadingType == LoadingType.TEXT_CAPTION_IMAGE },
        )
    }

    @Test
    fun `the title falls back to the destination name until content loads`() {
        val loading = ContentDetailState().title(strings)

        assertEquals("str-CONTENT_DETAIL_TITLE", loading.title)
        assertTrue(loading.shouldShowBack)
    }

    @Test
    fun `loaded content titles the screen with the item`() {
        val loaded = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE)).title(strings)

        assertEquals(CONTENT_DETAIL_FIXTURE.title, loaded.title)
        assertTrue(loaded.shouldShowBack)
    }

    @Test
    fun `the hero section uses the deterministic cover request`() {
        val hero = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
            .toListItems(strings)
            .sections()[0]
            .sectionItems
            .single() as HeroImageListModel

        assertEquals(
            "generated://content-detail/hero/1?width=528&height=704",
            hero.sourceInfo.info,
        )
        assertEquals(CONTENT_DETAIL_FIXTURE.title, hero.contentDescription)
        assertEquals(Icon.Album, hero.imagePlaceholder)
        assertEquals(ContentDetailAction.HeroClicked, hero.clickAction)
    }

    @Test
    fun `the cta section renders the generated actions`() {
        val cta = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
            .toListItems(strings)
            .sections()[1]
            .sectionItems
            .filterIsInstance<CtaListModel>()

        assertEquals(listOf("First action", "Second action"), cta.map { it.name })
        assertEquals(listOf(Icon.Calendar, Icon.Tag), cta.map { it.icon })
        assertEquals(
            listOf(
                ContentDetailAction.CtaClicked("First action"),
                ContentDetailAction.CtaClicked("Second action"),
            ),
            cta.map { it.clickAction },
        )
    }

    @Test
    fun `the details section renders generated label value rows`() {
        val details = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
            .toListItems(strings)
            .sections()[2]
            .sectionItems

        val header = details.first() as SectionHeaderListModel
        assertEquals("str-CONTENT_DETAIL_SECTION_DETAILS", header.title)

        val rows = details.filterIsInstance<LabelValueListModel>()
        assertEquals(listOf("Alpha label", "Beta label"), rows.map { it.label })
        assertEquals(listOf("One", "Two"), rows.map { it.value })
    }

    @Test
    fun `blank generated fields are omitted`() {
        val details = ContentDetailState(
            LCE.Content(
                CONTENT_DETAIL_FIXTURE.copy(
                    details = listOf(
                        ContentDetailField(label = "Kept", value = "Value"),
                        ContentDetailField(label = "", value = "No label"),
                        ContentDetailField(label = "No value", value = " "),
                    ),
                ),
            ),
        )
            .toListItems(strings)
            .sections()[2]
            .sectionItems
            .filterIsInstance<LabelValueListModel>()

        assertEquals(listOf("Kept"), details.map { it.label })
    }

    @Test
    fun `the related section scrolls deterministic wide items`() {
        val related = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
            .toListItems(strings)
            .sections()[3]
            .sectionItems

        assertEquals(
            "str-CONTENT_DETAIL_SECTION_RELATED",
            (related.first() as SectionHeaderListModel).title,
        )

        val scroller = related.filterIsInstance<HorizontalScrollerListModel>().single()
        val items = scroller.scrollingItems.filterIsInstance<WideItemListModel>()
        assertEquals(listOf("Alpha", "Beta"), items.map { it.name })
        assertEquals(
            listOf(
                "generated://content-detail/related/1?width=128&height=128",
                "generated://content-detail/related/2?width=128&height=128",
            ),
            items.map { it.sourceInfo.info },
        )
        assertEquals(
            listOf(
                ContentDetailAction.RelatedClicked(CONTENT_DETAIL_FIXTURE.related[0]),
                ContentDetailAction.RelatedClicked(CONTENT_DETAIL_FIXTURE.related[1]),
            ),
            items.map { it.clickAction },
        )
    }

    @Test
    fun `the items section maps generated items to image name caption rows`() {
        val items = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
            .toListItems(strings)
            .sections()[4]
            .sectionItems

        assertEquals(
            "str-CONTENT_DETAIL_SECTION_ITEMS",
            (items.first() as SectionHeaderListModel).title,
        )

        val rows = items.filterIsInstance<ImageNameCaptionListModel>()
        assertEquals(listOf("One", "Two"), rows.map { it.name })
        assertEquals(listOf("Ann", "Bob"), rows.map { it.caption })
        assertEquals(
            listOf(
                "generated://content-detail/item/1?width=128&height=128",
                "generated://content-detail/item/2?width=128&height=128",
            ),
            rows.map { it.sourceInfo.info },
        )
        rows.forEach { row -> assertEquals(Icon.Person, row.imagePlaceholder) }
        assertEquals(
            listOf(
                ContentDetailAction.ItemClicked(CONTENT_DETAIL_FIXTURE.items[0]),
                ContentDetailAction.ItemClicked(CONTENT_DETAIL_FIXTURE.items[1]),
            ),
            rows.map { it.clickAction },
        )
    }

    @Test
    fun `error content maps to an error state`() {
        val rows = ContentDetailState(
            LCE.Error(
                CONTENT_DETAIL_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        )
            .toListItems(strings)

        val error = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(CONTENT_DETAIL_OPERATION_NAME, error.failedOperationName)
        assertEquals("boom", error.error.message)
    }

    @Test
    fun `the rendered list has no duplicate top-level ids`() {
        val state = ContentDetailState(LCE.Content(CONTENT_DETAIL_FIXTURE))
        val actual = state.toActual(strings)

        val ids = actual.listItems.map { it.dataId }
        assertEquals(ids.size, ids.distinct().size)
        assertFalse(actual.listItems.any { it is ErrorStateListModel })
    }

    /** Unwraps every top-level section; the screen renders exactly one section per feature block. */
    private fun List<ListModel>.sections(): List<SectionListModel> =
        filterIsInstance<SectionListModel>().also { sections ->
            assertEquals(size, sections.size)
        }
}
