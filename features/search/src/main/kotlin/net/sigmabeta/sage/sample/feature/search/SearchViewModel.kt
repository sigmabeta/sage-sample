package net.sigmabeta.sage.sample.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
 * actions, like the other SAGE apps. Typing only updates [SearchState.query], so the results
 * already on screen stay visible while the next query is being edited.
 *
 * [observeQuery] waits [SEARCH_DEBOUNCE_MILLIS] and then submits a settled query of at least
 * [MIN_QUERY_LENGTH] characters: it records [SearchState.submittedQuery] and runs *two*
 * repository queries, one for the row-shaped results and one for the grid images. The previous
 * pair is cancelled first, and each result is only published while its query is still the
 * submitted one, so a superseded search can neither waste work nor overwrite newer rows.
 *
 * A blank query returns to the resting prompt immediately, without waiting for the debounce.
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

    /** In-flight repository reads; cancelled whenever a newer query supersedes them. */
    private var listJob: Job? = null
    private var gridJob: Job? = null

    override fun initialState(): SearchState = SearchState()

    init {
        sendAction(SageAction.InitNoArgs)
        observeQuery()
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.SearchQueryEntered -> onQueryEntered(action.query)

            SageAction.SearchClearClicked -> clearSearch()

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
        if (rawQuery.isBlank()) {
            // Clearing the box returns to the resting prompt straight away; the debounce never
            // sees a blank query worth submitting.
            clearSearch()
            return
        }

        // Only the field text changes. Whatever is on screen belongs to the last submitted
        // query and stays there until a new query settles (or the field is cleared).
        updateState { it.copy(query = rawQuery) }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        internalUiState
            .map { it.query.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .onEach { query ->
                if (query.length >= MIN_QUERY_LENGTH &&
                    query != internalUiState.value.submittedQuery
                ) {
                    submit(query)
                }
            }
            .launchIn(viewModelScope)
    }

    /** Cancels both repository reads and returns to the empty, resting state. */
    private fun clearSearch() {
        cancelSearchJobs()
        updateState { SearchState() }
    }

    /** Makes [query] the submitted query and runs both repository reads for it. */
    private fun submit(query: String) {
        cancelSearchJobs()

        updateState {
            it.copy(
                submittedQuery = query,
                listResults = LCE.Loading(SEARCH_LIST_OPERATION_NAME),
                gridResults = LCE.Loading(SEARCH_GRID_OPERATION_NAME),
            )
        }

        listJob = repository.searchList(query)
            .onEach { results ->
                updateState { state ->
                    // A superseded search must not publish, even if its result was already
                    // queued when the new query cancelled it.
                    if (state.submittedQuery == query) {
                        state.copy(listResults = results)
                    } else {
                        state
                    }
                }
            }
            .runInBackground()

        gridJob = repository.searchGrid(query)
            .onEach { results ->
                updateState { state ->
                    if (state.submittedQuery == query) {
                        state.copy(gridResults = results)
                    } else {
                        state
                    }
                }
            }
            .runInBackground()
    }

    private fun cancelSearchJobs() {
        listJob?.cancel()
        gridJob?.cancel()
        listJob = null
        gridJob = null
    }
}
