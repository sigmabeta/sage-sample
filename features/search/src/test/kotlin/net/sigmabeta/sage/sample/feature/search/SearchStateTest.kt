package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SearchHistoryListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchStateTest {
    private val strings = FakeStringProvider()

    private val listResults = listOf(
        SearchResult(id = 11L, name = "Fermentum Pellentesque", caption = "Elias Conrad"),
        SearchResult(id = 22L, name = "Lorem Ipsum Dolor", caption = "Kimiko Crane"),
    )

    private val gridResults = listOf(
        SearchResult(id = 11L, name = "Adipiscing Elit", caption = "Exie Clayton"),
        SearchResult(id = 22L, name = "Iaculis Neque", caption = "Kurt Bolton"),
    )

    @Test
    fun `no submitted query shows the prompt even while text is being edited`() {
        listOf("", "  ", "ab", "chrono").forEach { query ->
            val rows = SearchState(
                query = query,
                listResults = LCE.Content(listResults),
                gridResults = LCE.Content(gridResults),
            ).toListItems(strings)

            val prompt = rows.filterIsInstance<EmptyStateListModel>().single()
            assertEquals("str-SEARCH_PROMPT", prompt.explanation)
            assertTrue(
                rows.none {
                    it is ImageNameCaptionListModel ||
                        it is GridImageListModel ||
                        it is LoadingItemListModel
                },
            )
        }
    }

    @Test
    fun `list and grid results render as two headed sections`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Content(listResults),
            gridResults = LCE.Content(gridResults),
        ).toListItems(strings)

        assertEquals(
            listOf("str-SEARCH_SECTION_LIST", "str-SEARCH_SECTION_GRID"),
            rows.filterIsInstance<SectionHeaderListModel>().map { it.title },
        )

        val listRows = rows.filterIsInstance<ImageNameCaptionListModel>()
        assertEquals(listOf(11L, 22L), listRows.map { it.dataId })
        assertEquals(listOf("Fermentum Pellentesque", "Lorem Ipsum Dolor"), listRows.map { it.name })
        assertEquals(listOf("Elias Conrad", "Kimiko Crane"), listRows.map { it.caption })
        assertEquals(
            listOf(
                listUrl("chrono", 11L),
                listUrl("chrono", 22L),
            ),
            listRows.map { it.sourceInfo.info },
        )

        val gridRows = rows.filterIsInstance<GridImageListModel>()
        assertEquals(listOf("Adipiscing Elit", "Iaculis Neque"), gridRows.map { it.name })
        assertEquals(
            listOf(
                gridUrl("chrono", 11L),
                gridUrl("chrono", 22L),
            ),
            gridRows.map { it.sourceInfo.info },
        )

        val listIds = listRows.map { it.dataId }.toSet()
        val gridIds = gridRows.map { it.dataId }.toSet()
        assertTrue(listIds.intersect(gridIds).isEmpty())
        assertEquals(rows.size, rows.map { it.dataId }.toSet().size)
    }

    @Test
    fun `result clicks carry the originating result`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Content(listResults),
            gridResults = LCE.Content(gridResults),
        ).toListItems(strings)

        val listIds = rows.filterIsInstance<ImageNameCaptionListModel>()
            .map { (it.clickAction as SearchAction.ResultClicked).result.id }
        val gridIds = rows.filterIsInstance<GridImageListModel>()
            .map { (it.clickAction as SearchAction.ResultClicked).result.id }

        assertEquals(listOf(11L, 22L), listIds)
        assertEquals(listOf(11L, 22L), gridIds)
    }

    @Test
    fun `empty list results hide the list section`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Content(emptyList()),
            gridResults = LCE.Content(gridResults),
        ).toListItems(strings)

        assertEquals(
            listOf("str-SEARCH_SECTION_GRID"),
            rows.filterIsInstance<SectionHeaderListModel>().map { it.title },
        )
        assertTrue(rows.none { it is ImageNameCaptionListModel })
        assertTrue(rows.any { it is GridImageListModel })
    }

    @Test
    fun `both empty result sets show the no-results state`() {
        val rows = SearchState(
            query = "nothing",
            submittedQuery = "nothing",
            listResults = LCE.Content(emptyList()),
            gridResults = LCE.Content(emptyList()),
        ).toListItems(strings)

        val empty = rows.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-SEARCH_NO_RESULTS:nothing", empty.explanation)
    }

    @Test
    fun `loading results render headed loading rows for both queries`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Loading(SEARCH_LIST_OPERATION_NAME),
            gridResults = LCE.Loading(SEARCH_GRID_OPERATION_NAME),
        ).toListItems(strings)

        assertEquals(
            listOf("str-SEARCH_SECTION_LIST", "str-SEARCH_SECTION_GRID"),
            rows.filterIsInstance<SectionHeaderListModel>().map { it.title },
        )

        val loadingRows = rows.filterIsInstance<LoadingItemListModel>()
        assertTrue(loadingRows.any { it.loadingType == LoadingType.TEXT_CAPTION_IMAGE })
        assertTrue(loadingRows.any { it.loadingType == LoadingType.SQUARE })
        assertTrue(
            loadingRows
                .filter { it.loadingType == LoadingType.TEXT_CAPTION_IMAGE }
                .all { it.loadOperationName == SEARCH_LIST_OPERATION_NAME },
        )
        assertTrue(
            loadingRows
                .filter { it.loadingType == LoadingType.SQUARE }
                .all { it.loadOperationName == SEARCH_GRID_OPERATION_NAME },
        )
        assertTrue(rows.none { it is ImageNameCaptionListModel || it is GridImageListModel })
    }

    @Test
    fun `an uninitialized submitted query shows loading rather than no results`() {
        val rows = SearchState(submittedQuery = "chrono").toListItems(strings)

        assertTrue(rows.filterIsInstance<LoadingItemListModel>().isNotEmpty())
        assertTrue(rows.none { it is EmptyStateListModel })
    }

    @Test
    fun `an error in one query leaves the other section visible`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Error(SEARCH_LIST_OPERATION_NAME, IllegalStateException("boom")),
            gridResults = LCE.Content(gridResults),
        ).toListItems(strings)

        val error = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(SEARCH_LIST_OPERATION_NAME, error.failedOperationName)
        assertEquals("boom", error.error.message)
        assertEquals(2, rows.filterIsInstance<GridImageListModel>().size)
    }

    @Test
    fun `errors in both queries keep both headers and render one error per section`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Error(SEARCH_LIST_OPERATION_NAME, IllegalStateException("list boom")),
            gridResults = LCE.Error(SEARCH_GRID_OPERATION_NAME, IllegalStateException("grid boom")),
        ).toListItems(strings)

        assertEquals(
            listOf("str-SEARCH_SECTION_LIST", "str-SEARCH_SECTION_GRID"),
            rows.filterIsInstance<SectionHeaderListModel>().map { it.title },
        )
        assertEquals(
            listOf(SEARCH_LIST_OPERATION_NAME, SEARCH_GRID_OPERATION_NAME),
            rows.filterIsInstance<ErrorStateListModel>().map { it.failedOperationName },
        )
        assertTrue(rows.none { it is ImageNameCaptionListModel || it is GridImageListModel })
    }

    @Test
    fun `blank submitted query renders history rows newest first`() {
        val rows = SearchState(history = listOf("chrono", "ff7")).toListItems(strings)

        val historyRows = rows.filterIsInstance<SearchHistoryListModel>()
        assertEquals(listOf("chrono", "ff7"), historyRows.map { it.name })
        assertEquals(
            listOf(
                SearchAction.HistoryClicked("chrono"),
                SearchAction.HistoryClicked("ff7"),
            ),
            historyRows.map { it.clickAction },
        )
        assertEquals(
            listOf(
                SearchAction.HistoryRemoved("chrono"),
                SearchAction.HistoryRemoved("ff7"),
            ),
            historyRows.map { it.removeAction },
        )
    }

    @Test
    fun `fewer than five history rows append the resting prompt`() {
        (0..4).forEach { count ->
            val rows = SearchState(history = List(count) { "query-$it" }).toListItems(strings)

            assertEquals(count, rows.filterIsInstance<SearchHistoryListModel>().size)
            val prompt = rows.filterIsInstance<EmptyStateListModel>().single()
            assertEquals("str-SEARCH_PROMPT", prompt.explanation)
        }
    }

    @Test
    fun `five or more history rows show only the history`() {
        val rows = SearchState(history = List(5) { "query-$it" }).toListItems(strings)

        assertEquals(5, rows.filterIsInstance<SearchHistoryListModel>().size)
        assertTrue(rows.none { it is EmptyStateListModel })
    }

    @Test
    fun `a submitted query hides history rows`() {
        val rows = SearchState(
            query = "chrono",
            submittedQuery = "chrono",
            listResults = LCE.Content(listResults),
            gridResults = LCE.Content(gridResults),
            history = listOf("ff7"),
        ).toListItems(strings)

        assertTrue(rows.none { it is SearchHistoryListModel })
    }

    @Test
    fun `editing the field without submitting keeps history visible`() {
        val rows = SearchState(
            query = "chro",
            history = listOf("ff7"),
        ).toListItems(strings)

        assertEquals(listOf("ff7"), rows.filterIsInstance<SearchHistoryListModel>().map { it.name })
    }

    @Test
    fun `the same submitted query seeds the same artwork for both sections`() {
        fun urls(query: String): List<String> = seededUrls(submittedQuery = query, query = query)

        assertEquals(urls("Chrono"), urls("  chrono  "))
    }

    @Test
    fun `artwork stays seeded by the submitted query while the field is edited`() {
        val settled = seededUrls(submittedQuery = "chrono", query = "chrono")
        val edited = seededUrls(submittedQuery = "chrono", query = "chronologue")

        assertEquals(settled, edited)
    }

    private fun seededUrls(submittedQuery: String, query: String): List<String> = SearchState(
        query = query,
        submittedQuery = submittedQuery,
        listResults = LCE.Content(listResults),
        gridResults = LCE.Content(gridResults),
    ).toListItems(strings)
        .filter { it is ImageNameCaptionListModel || it is GridImageListModel }
        .map {
            when (it) {
                is ImageNameCaptionListModel -> it.sourceInfo.info as String
                is GridImageListModel -> it.sourceInfo.info as String
                else -> error("unreachable")
            }
        }

    private fun listUrl(query: String, id: Long): String =
        "generated://search/${searchSeedFor(query, SearchResultKind.LIST)}/$id?width=128&height=128"

    private fun gridUrl(query: String, id: Long): String =
        "generated://search/${searchSeedFor(query, SearchResultKind.GRID)}/$id?width=256&height=256"
}
