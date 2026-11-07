package net.sigmabeta.sage.sample.feature.search

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.demoOutcomeFailure
import net.sigmabeta.sage.sample.settings.select

/**
 * The sample's generated search datasource.
 *
 * Each method rebuilds the same seeded result set through [searchResultsFor], so a query is
 * reproducible across visits and app runs. The list and grid calls use different seeds, so they
 * independently choose how many results to return. The artificial latency comes from the shared
 * `runInBackground` seam, which applies the duration selected on the Settings screen; this
 * repository does not add a delay of its own.
 *
 * The Settings demo outcome is read when a query is requested, not cached at construction:
 * [DemoOutcome.CONTENT] returns the generated results, [DemoOutcome.EMPTY] returns a successful
 * empty result set, and [DemoOutcome.ERROR] fails the load. `EMPTY`/`ERROR` apply to both the list
 * and grid queries, so Search's no-results state and its per-section errors are reachable without
 * source changes. Because the value is read per call, changing it affects the next search with no
 * app restart.
 *
 * Metro binds it to [SearchRepository] once for the application graph; the sample has no real
 * backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSearchRepository(
    private val settingsManager: SampleSettingsManager,
) : SearchRepository {
    override fun searchList(query: String): Flow<LCE<List<SearchResult>>> =
        resultsFor(query, SearchResultKind.LIST, SEARCH_LIST_OPERATION_NAME)

    override fun searchGrid(query: String): Flow<LCE<List<SearchResult>>> =
        resultsFor(query, SearchResultKind.GRID, SEARCH_GRID_OPERATION_NAME)

    private fun resultsFor(
        query: String,
        kind: SearchResultKind,
        operationName: String,
    ): Flow<LCE<List<SearchResult>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                outcome.select(
                    content = { LCE.Content(searchResultsFor(query, kind)) },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = operationName,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}
