package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.SearchResult

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SearchHistoryListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.bitmaps.GeneratedImageFetcher
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.sample.repository.SEARCH_GRID_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_LIST_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.searchSeedFor

/**
 * Search's content, declared as a SAGE [ListState].
 *
 * [query] is the live text in the app-owned search field. It is kept here (rather than only in
 * the field's composition state) so the view model can debounce it. [submittedQuery] is the
 * last query that actually reached the repository; it drives everything rendered, so editing
 * the field never blanks or replaces results before the new query settles.
 *
 * A submitted query feeds two independent [LCE] slots: [listResults] renders as
 * [ImageNameCaptionListModel] rows, and [gridResults] renders as [GridImageListModel] cells.
 * Each section owns its loading and error rows, so one can fail while the other still shows
 * content. The app's search content draws both in one adaptive grid; row items and headers span
 * the full width, while grid cells keep one column each.
 *
 * With no submitted query the screen shows its resting prompt. Queries shorter than
 * [MIN_SEARCH_QUERY_LENGTH] never reach the repository, so they never become a submitted query.
 *
 * [history] is the persisted recent-search list from the previous runs. It is rendered only while
 * [submittedQuery] is blank; with fewer than [HISTORY_PROMPT_THRESHOLD] entries the resting prompt
 * follows the rows, matching Chipbox's sparse-history rule.
 */
data class SearchState(
    val query: String = "",
    val submittedQuery: String = "",
    val listResults: LCE<List<SearchResult>> = LCE.Uninitialized,
    val gridResults: LCE<List<SearchResult>> = LCE.Uninitialized,
    val history: List<String> = emptyList(),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SampleStringId.SEARCH_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> {
        val submitted = submittedQuery.trim()
        if (submitted.isEmpty()) {
            return restingItems(stringProvider)
        }

        val rows = listSection(stringProvider, submitted) +
            gridSection(stringProvider, submitted)

        return rows.ifEmpty {
            listOf(
                EmptyStateListModel(
                    icon = Icon.Search,
                    explanation = stringProvider.getStringOneArg(
                        SampleStringId.SEARCH_NO_RESULTS,
                        submitted,
                    ),
                    showCrossOut = false,
                ),
            )
        }
    }

    /**
     * The blank-field screen: recent searches, plus the resting prompt while history is sparse.
     * At [HISTORY_PROMPT_THRESHOLD] rows the history is long enough to stand on its own.
     */
    private fun restingItems(stringProvider: StringProvider): List<ListModel> {
        val historyRows = history.map { query ->
            SearchHistoryListModel(
                dataId = historyDataId(query),
                name = query,
                clickAction = SearchAction.HistoryClicked(query),
                removeAction = SearchAction.HistoryRemoved(query),
            )
        }

        return if (historyRows.size < HISTORY_PROMPT_THRESHOLD) {
            historyRows + prompt(stringProvider)
        } else {
            historyRows
        }
    }

    private fun listSection(
        stringProvider: StringProvider,
        query: String,
    ): List<ListModel> = section(
        results = listResults,
        header = stringProvider.getString(SampleStringId.SEARCH_SECTION_LIST),
        operationName = SEARCH_LIST_OPERATION_NAME,
        loadingType = LoadingType.TEXT_CAPTION_IMAGE,
    ) { result ->
        ImageNameCaptionListModel(
            dataId = result.id,
            name = result.name,
            caption = result.caption,
            sourceInfo = imageSourceFor(result, query, SearchResultKind.LIST, LIST_IMAGE_SIZE),
            imagePlaceholder = Icon.Search,
            clickAction = SearchAction.ResultClicked(
                searchContentId(query, SearchResultKind.LIST, result.id),
            ),
        )
    }

    private fun gridSection(
        stringProvider: StringProvider,
        query: String,
    ): List<ListModel> = section(
        results = gridResults,
        header = stringProvider.getString(SampleStringId.SEARCH_SECTION_GRID),
        operationName = SEARCH_GRID_OPERATION_NAME,
        loadingType = LoadingType.SQUARE,
    ) { result ->
        GridImageListModel(
            dataId = result.id + GRID_DATA_ID_OFFSET,
            name = result.name,
            sourceInfo = imageSourceFor(result, query, SearchResultKind.GRID, GRID_IMAGE_SIZE),
            imagePlaceholder = Icon.Album,
            clickAction = SearchAction.ResultClicked(
                searchContentId(query, SearchResultKind.GRID, result.id),
            ),
        )
    }

    /**
     * Maps one query's [LCE] to a headed section. An uninitialized slot is treated as loading:
     * once the query is long enough, "not started yet" and "in flight" should look the same.
     */
    private fun section(
        results: LCE<List<SearchResult>>,
        header: String,
        operationName: String,
        loadingType: LoadingType,
        rowContent: (SearchResult) -> ListModel,
    ): List<ListModel> = when (results) {
        LCE.Uninitialized,
        is LCE.Loading -> listOf(SectionHeaderListModel(header)) + loading(
            operationName = operationName,
            loadingType = loadingType,
            loadingItemCount = LOADING_ITEM_COUNT,
        )

        is LCE.Error -> listOf(SectionHeaderListModel(header)) +
            error(operationName, results.error)

        is LCE.Content -> if (results.data.isEmpty()) {
            emptyList()
        } else {
            listOf(SectionHeaderListModel(header)) + results.data.map(rowContent)
        }
    }

    private fun imageSourceFor(
        result: SearchResult,
        query: String,
        kind: SearchResultKind,
        imageSize: Int,
    ): SourceInfo = SourceInfo(
        GeneratedImageFetcher.urlFor(
            seed = "$SEARCH_IMAGE_SEED_PREFIX/${searchSeedFor(query, kind)}/${result.id}",
            width = imageSize,
            height = imageSize,
        ),
    )

    private fun prompt(stringProvider: StringProvider): ListModel =
        EmptyStateListModel(
            icon = Icon.Search,
            explanation = stringProvider.getString(SampleStringId.SEARCH_PROMPT),
            showCrossOut = false,
        )

    /**
     * A stable list id for a history row. Queries are deduped, so the hash only has to keep
     * different queries apart for focus restoration.
     */
    private fun historyDataId(query: String): Long = query.hashCode().toLong()

    private companion object {
        const val LOADING_ITEM_COUNT = 5
        const val SEARCH_IMAGE_SEED_PREFIX = "search"

        /** Below this many history rows the resting prompt is appended. */
        const val HISTORY_PROMPT_THRESHOLD = 5

        /** Keeps grid cells' list ids disjoint from the row results' ids. */
        const val GRID_DATA_ID_OFFSET = 1_000_000L

        /** Larger than the row thumbnail, so generated artwork stays sharp on dense screens. */
        const val LIST_IMAGE_SIZE = 128

        /** Larger than a 160 dp grid cell, so generated artwork stays sharp on dense screens. */
        const val GRID_IMAGE_SIZE = 256
    }
}
