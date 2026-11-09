package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.models.contentDetailRoute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.settings.SearchHistoryManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.sample.repository.MIN_SEARCH_QUERY_LENGTH
import net.sigmabeta.sage.sample.repository.SEARCH_GRID_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SEARCH_LIST_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.SearchRepository

/** How long a settled query must stay current before it is worth remembering. */
internal const val SEARCH_HISTORY_RECORD_DELAY_MILLIS = 3_000L

/**
 * Search's view model.
 *
 * Query entry uses SAGE's shared [SageAction.SearchQueryEntered] / [SageAction.SearchClearClicked]
 * actions. Typing only updates [SearchState.query], so the results
 * already on screen stay visible while the next query is being edited.
 *
 * [observeQuery] waits [SEARCH_DEBOUNCE_MILLIS] and then submits a settled query of at least
 * [MIN_SEARCH_QUERY_LENGTH] characters: it records [SearchState.submittedQuery] and runs *two*
 * repository queries, one for the row-shaped results and one for the grid images. The previous
 * pair is cancelled first, and each result is only published while its query is still the
 * submitted one, so a superseded search can neither waste work nor overwrite newer rows.
 *
 * A blank query returns to the resting prompt immediately, without waiting for the debounce.
 *
 * Recent searches come from [SearchHistoryManager]. A settled query is recorded only after both
 * [SEARCH_HISTORY_RECORD_DELAY_MILLIS] of lingering *and* that query's two sections reaching a
 * terminal result (the Settings simulate-delay can hold results past the linger window), and only
 * when at least one section has content; a newer settled query or a clear cancels the pending
 * record. Selecting a history row refills [SearchState.query] so the normal debounce submits it,
 * and clearing the field keeps the history rendered.
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
    private val historyManager: SearchHistoryManager,
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

    /**
     * Field-text changes the user did not type, such as selecting a recent search or clearing the
     * box. The app-owned search field keeps its text in local composition state so typing echoes
     * synchronously; it subscribes to this flow only to apply those external refills.
     */
    private val internalFieldUpdates = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val fieldUpdates: SharedFlow<String> = internalFieldUpdates

    /** In-flight repository reads; cancelled whenever a newer query supersedes them. */
    private var listJob: Job? = null
    private var gridJob: Job? = null

    /** The pending linger window; cancelled by a newer settled query or a clear. */
    private var historyJob: Job? = null

    override fun initialState(): SearchState = SearchState()

    init {
        sendAction(SageAction.InitNoArgs)
        observeHistory()
        observeQuery()
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.SearchQueryEntered -> onQueryEntered(action.query)

            SageAction.SearchClearClicked -> {
                clearSearch()
                internalFieldUpdates.tryEmit("")
            }

            is SearchAction.HistoryClicked -> {
                onQueryEntered(action.query)
                internalFieldUpdates.tryEmit(action.query)
            }

            is SearchAction.HistoryRemoved -> historyManager.remove(action.query)

            is SearchAction.ResultClicked -> emitEvent(
                SageEvent.NavigateTo(
                    destination = contentDetailRoute(action.contentId),
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
                // Any newly settled query supersedes the pending record, including one too short
                // to submit: the user has moved on from the query the timer was for.
                historyJob?.cancel()

                if (query.length >= MIN_SEARCH_QUERY_LENGTH &&
                    query != internalUiState.value.submittedQuery
                ) {
                    submit(query)
                }
            }
            .launchIn(viewModelScope)
    }

    /** Mirrors the persisted recent searches into the rendered state. */
    private fun observeHistory() {
        historyManager.history
            .onEach { entries -> updateState { it.copy(history = entries) } }
            .launchIn(viewModelScope)
    }

    /** Cancels both repository reads and returns to the empty, resting state. */
    private fun clearSearch() {
        cancelSearchJobs()
        historyJob?.cancel()
        // Keep the persisted history rendered: clearing the field returns to recent searches.
        updateState { SearchState(history = it.history) }
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

        startHistoryRecording(query)

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

    /**
     * Starts recording [query] once both gates pass: the linger window has elapsed *and* both of
     * that query's result sections are terminal. The second gate matters because the Settings
     * simulate-delay makes results arrive after the linger window; checking only the timer would
     * see `Loading` and never record. A newer query or a clear cancels this job.
     */
    private fun startHistoryRecording(query: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            delay(SEARCH_HISTORY_RECORD_DELAY_MILLIS)

            val settled = internalUiState.first { it.hasSettled(query) }
            if (settled.hasAnyResults()) {
                historyManager.record(query)
            }
        }
    }
}

private fun LCE<List<*>>.hasContent(): Boolean = this is LCE.Content && data.isNotEmpty()

private fun LCE<List<*>>.isTerminal(): Boolean = this is LCE.Content || this is LCE.Error

/** Whether [query] is still submitted and its two result sections have both finished loading. */
private fun SearchState.hasSettled(query: String): Boolean =
    submittedQuery == query && listResults.isTerminal() && gridResults.isTerminal()

/** Whether any result section has at least one row; history is only recorded on results. */
private fun SearchState.hasAnyResults(): Boolean =
    listResults.hasContent() || gridResults.hasContent()
