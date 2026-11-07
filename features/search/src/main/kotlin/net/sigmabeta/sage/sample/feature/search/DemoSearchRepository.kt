package net.sigmabeta.sage.sample.feature.search

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope

/**
 * The sample's generated search datasource.
 *
 * Each method rebuilds the same seeded result set through [searchResultsFor], so a query is
 * reproducible across visits and app runs. The list and grid calls use different seeds, so they
 * independently choose how many results to return. The artificial latency comes from the shared
 * `runInBackground` seam, which applies the duration selected on the Settings screen; this
 * repository does not add a delay of its own.
 *
 * Metro binds it to [SearchRepository] once for the application graph; the sample has no real
 * backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSearchRepository : SearchRepository {
    override fun searchList(query: String): Flow<LCE<List<SearchResult>>> = flow {
        emit(LCE.Content(searchResultsFor(query, SearchResultKind.LIST)))
    }

    override fun searchGrid(query: String): Flow<LCE<List<SearchResult>>> = flow {
        emit(LCE.Content(searchResultsFor(query, SearchResultKind.GRID)))
    }
}
