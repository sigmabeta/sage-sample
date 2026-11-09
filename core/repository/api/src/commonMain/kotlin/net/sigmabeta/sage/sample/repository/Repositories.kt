package net.sigmabeta.sage.sample.repository

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.models.SearchResult

// The sample's data seams, split by feature.
//
// Features depend only on these interfaces. Each app decides who implements them: the Android and
// desktop apps bind the in-process `repository.demo` implementations, while the browser binds the
// HTTP-backed `repository.remote` ones against `apps:server`. That keeps the demo generators out
// of the feature modules and gives the web client the same backend-shaped seam a real app uses.

/**
 * The Home feature's datasource seam.
 *
 * Every call takes the module instance's [seed] — its stable id — so two instances of the same
 * module generate different content, while the same instance always reproduces the same items.
 * The two modules ask for different shapes: `BigImageModule` needs a count plus a single indexed
 * pick (a daily, stable hero), while `GridImagesModule` needs a bounded list. Both stay cheap:
 * neither call loads the whole catalog to filter it in memory, which is the rule real Home
 * modules follow. Tests can substitute their own data without touching either module.
 */
interface HomeRepository {
    /** Total number of items available to the daily pick for [seed]. */
    suspend fun itemCount(seed: String): Int

    /** The item at [index] for [seed], or null when that catalog is shorter. */
    suspend fun itemAt(seed: String, index: Int): HomeItem?

    /** The first [limit] items for [seed], streamed with the grid's loading state. */
    fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>>
}

/**
 * The data seam Content detail loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. Tests inject their own fake implementation.
 */
fun interface ContentDetailRepository {
    /**
     * Loads the item named by [contentId].
     *
     * A `null` [ContentDetail] is the repository's explicit not-found result, not an error: the
     * screen renders a localized explanation for it.
     */
    fun load(contentId: ContentId): Flow<LCE<ContentDetail?>>
}

/** The operation name Content detail's LCE values carry through loading and error states. */
const val CONTENT_DETAIL_OPERATION_NAME = "content-detail"

/**
 * The data seam List Items loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. [load] asks for a window of the catalogue: a paginated
 * screen fetches one page at a time with [offset]/[limit], while a non-paginated screen calls
 * [load] without arguments for the whole set. Tests inject their own fake implementation.
 */
interface SampleItemRepository {
    /** Loads the `[offset, offset + limit)` window of the catalogue. */
    fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>>

    /** Loads the whole set, for the non-paginated List Items screen. */
    fun load(): Flow<LCE<List<SampleItem>>> = load(offset = 0, limit = LIST_ITEMS_ITEM_COUNT)
}

/** The operation name List Items' LCE values carry through loading and error states. */
const val LIST_ITEMS_OPERATION_NAME = "list-items"

/** The size of the List Items catalogue, and the non-paginated screen's page limit. */
const val LIST_ITEMS_ITEM_COUNT = 25

/**
 * The data seam Grid of Images loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. [load] asks for a window of the catalogue: the grid
 * fetches one page at a time with [offset]/[limit]. Tests inject their own fake implementation.
 */
interface GridOfImagesRepository {
    /** Loads the `[offset, offset + limit)` window of the catalogue. */
    fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>>

    /** Loads the whole set, for code that does not page. */
    fun load(): Flow<LCE<List<SampleItem>>> = load(offset = 0, limit = GRID_OF_IMAGES_ITEM_COUNT)
}

/** How many generated images the demo grid renders. */
const val GRID_OF_IMAGES_ITEM_COUNT = 50

/** The operation name Grid of Images' LCE values carry through loading and error states. */
const val GRID_OF_IMAGES_OPERATION_NAME = "grid-of-images"

/**
 * The search datasource seam.
 *
 * A query runs two independent repository calls: [searchList] supplies the row-shaped results
 * (rendered as `ImageNameCaptionListModel`), and [searchGrid] supplies the grid-image results
 * (rendered as `GridImageListModel`). Keeping them separate means each can have its own random
 * result count, loading state, and error state.
 *
 * Tests can stand in their own data, and the browser's HTTP implementation replaces the demo
 * generator without touching the screen.
 */
interface SearchRepository {
    fun searchList(query: String): Flow<LCE<List<SearchResult>>>

    fun searchGrid(query: String): Flow<LCE<List<SearchResult>>>
}

/** The two independent result sets a query returns. */
enum class SearchResultKind(val routeSegment: String) {
    LIST("list"),
    GRID("grid"),
}

/**
 * Stable seed for one query + result-set pair; case and surrounding whitespace are ignored.
 *
 * Shared by the demo generator and Search's artwork/content-id derivation so both sides of the
 * repository seam agree on what a query's results are called.
 */
fun searchSeedFor(query: String, kind: SearchResultKind): Int =
    "${kind.name}:${normalizeSearchQuery(query)}".hashCode()

/** Trims and lowercases a query before it is hashed or compared. */
fun normalizeSearchQuery(query: String): String = query.trim().lowercase()

/** Queries shorter than this show the prompt instead of running a search. */
const val MIN_SEARCH_QUERY_LENGTH = 3

/** LCE operation name used by the list-result query's loading and error rows. */
const val SEARCH_LIST_OPERATION_NAME = "search.list"

/** LCE operation name used by the grid-result query's loading and error rows. */
const val SEARCH_GRID_OPERATION_NAME = "search.grid"

/** Each repository query returns a random number of results in this range. */
const val MIN_SEARCH_RESULT_COUNT = 1

/** The maximum number of results one repository query may return. */
const val MAX_SEARCH_RESULT_COUNT = 20
