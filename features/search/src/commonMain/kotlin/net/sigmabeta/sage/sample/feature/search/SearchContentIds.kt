package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.searchSeedFor

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

/** How long the search box waits after typing before submitting a query. */
internal const val SEARCH_DEBOUNCE_MILLIS = 300L
