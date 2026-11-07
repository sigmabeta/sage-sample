package net.sigmabeta.sage.sample.feature.search

import java.util.Random
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import net.sigmabeta.sage.ui.StringGenerator

/** The two independent result sets a query returns. */
internal enum class SearchResultKind(val routeSegment: String) {
    LIST("list"),
    GRID("grid"),
}

/**
 * Builds the sample's fake results for one repository query.
 *
 * The query and [SearchResultKind] together are the seed: trim the query, lowercase it, and feed
 * the pair's hash into [Random]. Each kind therefore gets its own random count and contents,
 * while a given query + kind always reproduces the same rows. [SearchState] uses the same seed
 * for the `generated://` artwork URLs.
 */
internal fun searchResultsFor(query: String, kind: SearchResultKind): List<SearchResult> {
    val normalized = normalizeQuery(query)
    if (normalized.length < MIN_QUERY_LENGTH) {
        return emptyList()
    }

    val random = Random(searchSeedFor(normalized, kind).toLong())
    val generator = StringGenerator(random)
    val resultCount = MIN_RESULT_COUNT + random.nextInt(MAX_RESULT_COUNT - MIN_RESULT_COUNT + 1)

    return List(resultCount) { index ->
        SearchResult(
            id = index + 1L,
            name = generator.generateTitle(),
            caption = generator.generateName(),
        )
    }
}

/** Stable seed for one query + result-set pair; case and surrounding whitespace are ignored. */
internal fun searchSeedFor(query: String, kind: SearchResultKind): Int =
    "${kind.name}:${normalizeQuery(query)}".hashCode()

/**
 * The stable content ID for result [resultId] of one query + result set.
 *
 * The list and grid sections each number their results from 1, so the query + kind seed is part
 * of the namespace: equal numeric IDs in the two sections still route apart, and the same query
 * reproduces the same route on every search.
 */
internal fun searchContentId(
    query: String,
    kind: SearchResultKind,
    resultId: Long,
): ContentId = ContentId(
    "search:${kind.routeSegment}:${searchSeedFor(query, kind)}:$resultId",
)

private fun normalizeQuery(query: String): String = query.trim().lowercase()

/** Queries shorter than this show the prompt instead of running a search. */
internal const val MIN_QUERY_LENGTH = 3

/** How long the search box waits after typing before submitting a query. */
internal const val SEARCH_DEBOUNCE_MILLIS = 300L

/** LCE operation name used by the list-result query's loading and error rows. */
internal const val SEARCH_LIST_OPERATION_NAME = "search.list"

/** LCE operation name used by the grid-result query's loading and error rows. */
internal const val SEARCH_GRID_OPERATION_NAME = "search.grid"

/** Each repository query returns a random number of results in this range. */
internal const val MIN_RESULT_COUNT = 1

/** The maximum number of results one repository query may return. */
internal const val MAX_RESULT_COUNT = 20
