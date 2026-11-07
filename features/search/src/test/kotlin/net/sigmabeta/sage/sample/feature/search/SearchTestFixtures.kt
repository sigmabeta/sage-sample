package net.sigmabeta.sage.sample.feature.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

internal class FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = "str-$string"

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        "${getString(string)}:$arg"

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        error("unused")

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        error("unused")
}

internal class FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit

    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit

    override fun logEvent(event: SageEvent) = Unit

    override fun logAutoRefresh() = Unit

    override fun logError(
        failedOperationName: String,
        errorString: String,
        error: Throwable,
    ) = Unit
}

/**
 * Search datasource seam used by the view model tests.
 *
 * By default it answers with the real seeded generators, so the view model test also exercises
 * the two-query-to-results path. The separate query lists record what actually reached each
 * repository method, which is how the debounce and minimum-length tests observe submissions.
 */
internal class FakeSearchRepository(
    private val listResponse: (String) -> LCE<List<SearchResult>> = { query ->
        LCE.Content(searchResultsFor(query, SearchResultKind.LIST))
    },
    private val gridResponse: (String) -> LCE<List<SearchResult>> = { query ->
        LCE.Content(searchResultsFor(query, SearchResultKind.GRID))
    },
) : SearchRepository {
    val listQueries = mutableListOf<String>()
    val gridQueries = mutableListOf<String>()

    override fun searchList(query: String): Flow<LCE<List<SearchResult>>> = flow {
        listQueries += query
        emit(listResponse(query))
    }

    override fun searchGrid(query: String): Flow<LCE<List<SearchResult>>> = flow {
        gridQueries += query
        emit(gridResponse(query))
    }
}
