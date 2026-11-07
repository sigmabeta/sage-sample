package net.sigmabeta.sage.sample.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Search's view model.
 *
 * Query entry uses SAGE's shared [SageAction.SearchQueryEntered] / [SageAction.SearchClearClicked]
 * actions, like the other SAGE apps. Typing updates [SearchState.query] immediately, then
 * [observeQuery] waits [SEARCH_DEBOUNCE_MILLIS] and runs *two* repository queries: one for the
 * row-shaped results and one for the grid images. Results are live data from the sample
 * generator and are keyed by the query they were started for, so a slow load cannot overwrite a
 * newer query's rows.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class SearchViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: SearchRepository,
) : SampleListViewModel<SearchState>() {
    override val screenIdentifier = AnalyticsScreen.SEARCH

    /**
     * The live text in the app-owned search field.
     *
     * [uiStateActual] carries the rendered rows, which is enough for an ordinary list screen; the
     * search field also needs the current query, so this mirrors it out of the state machine.
     */
    val query: StateFlow<String> by lazy {
        internalUiState
            .map { it.query }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = initialState().query,
            )
    }

    override fun initialState(): SearchState = SearchState()

    init {
        sendAction(SageAction.InitNoArgs)
        observeQuery()
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.SearchQueryEntered -> onQueryEntered(action.query)

            SageAction.SearchClearClicked -> updateState { SearchState() }

            is SearchAction.ResultClicked -> emitEvent(
                SageEvent.ShowSnackbar(
                    message = stringProvider.getStringOneArg(
                        SearchStringId.SEARCH_RESULT_CLICKED,
                        action.result.name,
                    ),
                    withDismissAction = false,
                    source = "SearchViewModel",
                ),
            )

            else -> Unit
        }
    }

    private fun onQueryEntered(rawQuery: String) {
        val trimmedQuery = rawQuery.trim()

        updateState { state ->
            when {
                trimmedQuery.length < MIN_QUERY_LENGTH -> state.copy(
                    query = rawQuery,
                    listResults = LCE.Uninitialized,
                    gridResults = LCE.Uninitialized,
                )

                trimmedQuery == state.query.trim() -> state.copy(query = rawQuery)

                else -> state.copy(
                    query = rawQuery,
                    listResults = LCE.Loading(SEARCH_LIST_OPERATION_NAME),
                    gridResults = LCE.Loading(SEARCH_GRID_OPERATION_NAME),
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        internalUiState
            .map { it.query.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .onEach { query ->
                if (query.length >= MIN_QUERY_LENGTH) {
                    search(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun search(query: String) {
        updateState {
            it.copy(
                listResults = LCE.Loading(SEARCH_LIST_OPERATION_NAME),
                gridResults = LCE.Loading(SEARCH_GRID_OPERATION_NAME),
            )
        }

        repository.searchList(query)
            .onEach { results ->
                updateState { state ->
                    // The query may have moved on while this result was loading; only the
                    // current query's result set may replace the rows.
                    if (state.query.trim() == query) {
                        state.copy(listResults = results)
                    } else {
                        state
                    }
                }
            }
            .runInBackground()

        repository.searchGrid(query)
            .onEach { results ->
                updateState { state ->
                    if (state.query.trim() == query) {
                        state.copy(gridResults = results)
                    } else {
                        state
                    }
                }
            }
            .runInBackground()
    }
}
