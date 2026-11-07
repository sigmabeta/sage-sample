package net.sigmabeta.sage.sample.feature.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SearchHistoryListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The view model owns the other SAGE apps' search timing: queries shorter than the minimum never
 * run, and a settled query runs two repository calls once after the debounce. The repository
 * records submissions per query, so these tests assert on what actually reached it rather than
 * on internal timing machinery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repository: FakeSearchRepository
    private lateinit var historyManager: FakeSearchHistoryManager

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeSearchRepository()
        historyManager = FakeSearchHistoryManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `queries below the minimum length never reach either repository query`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            val state = viewModel.uiStateActual
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("ab"))
            advanceUntilIdle()

            assertTrue(repository.listQueries.isEmpty())
            assertTrue(repository.gridQueries.isEmpty())
            assertTrue(state.value.listItems.any { it is EmptyStateListModel })
        }

    @Test
    fun `a settled query runs one list and one grid query after the debounce`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            val state = viewModel.uiStateActual
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
            assertTrue(repository.listQueries.isEmpty())
            assertTrue(repository.gridQueries.isEmpty())

            advanceUntilIdle()

            assertEquals(listOf("abc"), repository.listQueries)
            assertEquals(listOf("abc"), repository.gridQueries)

            val listRows = state.value.listItems.filterIsInstance<ImageNameCaptionListModel>()
            assertEquals(
                searchResultsFor("abc", SearchResultKind.LIST).map { it.name },
                listRows.map { it.name },
            )

            val gridRows = state.value.listItems.filterIsInstance<GridImageListModel>()
            assertEquals(
                searchResultsFor("abc", SearchResultKind.GRID).map { it.name },
                gridRows.map { it.name },
            )
        }

    @Test
    fun `results stay shown while the next query is still being typed`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        val query = viewModel.query
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceUntilIdle()
        val settledListNames = state.value.listItems.listNames()
        val settledGridNames = state.value.listItems.gridNames()
        assertEquals(searchResultsFor("abc", SearchResultKind.LIST).map { it.name }, settledListNames)
        assertEquals(searchResultsFor("abc", SearchResultKind.GRID).map { it.name }, settledGridNames)

        viewModel.sendAction(SageAction.SearchQueryEntered("abcd"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)

        assertEquals("abcd", query.value)
        assertEquals(listOf("abc"), repository.listQueries)
        assertEquals(settledListNames, state.value.listItems.listNames())
        assertEquals(settledGridNames, state.value.listItems.gridNames())
        assertTrue(state.value.listItems.none { it is LoadingItemListModel })
    }

    @Test
    fun `a short non-blank query neither submits nor replaces valid results`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        val query = viewModel.query
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceUntilIdle()
        val settledListNames = state.value.listItems.listNames()
        val settledGridNames = state.value.listItems.gridNames()

        viewModel.sendAction(SageAction.SearchQueryEntered("ab"))
        advanceUntilIdle()

        assertEquals("ab", query.value)
        assertEquals(listOf("abc"), repository.listQueries)
        assertEquals(listOf("abc"), repository.gridQueries)
        assertEquals(settledListNames, state.value.listItems.listNames())
        assertEquals(settledGridNames, state.value.listItems.gridNames())
    }

    @Test
    fun `clearing the field by deleting the text returns to the prompt immediately`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            val state = viewModel.uiStateActual
            val query = viewModel.query
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceUntilIdle()
            assertTrue(state.value.listItems.any { it is ImageNameCaptionListModel })

            viewModel.sendAction(SageAction.SearchQueryEntered(""))
            advanceUntilIdle()

            assertEquals("", query.value)
            val prompt = state.value.listItems.filterIsInstance<EmptyStateListModel>().single()
            assertEquals("str-SEARCH_PROMPT", prompt.explanation)
            assertTrue(
                state.value.listItems.none {
                    it is ImageNameCaptionListModel ||
                        it is GridImageListModel ||
                        it is LoadingItemListModel
                },
            )
            assertEquals(listOf("abc"), repository.listQueries)
            assertEquals(listOf("abc"), repository.gridQueries)
        }

    @Test
    fun `a whitespace-only query also returns to the prompt`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceUntilIdle()
        viewModel.sendAction(SageAction.SearchQueryEntered("   "))
        advanceUntilIdle()

        assertTrue(state.value.listItems.any { it is EmptyStateListModel })
        assertTrue(state.value.listItems.none { it is ImageNameCaptionListModel })
    }

    @Test
    fun `a superseded search is cancelled and cannot publish stale results`() =
        runTest(dispatcher) {
            val cancelled = mutableListOf<String>()
            val stale = SearchResult(id = 1L, name = "Stale Result", caption = "Old Query")
            repository = FakeSearchRepository(
                listFlow = { submitted ->
                    if (submitted == "abc") {
                        flow {
                            try {
                                delay(STALE_RESULT_DELAY_MILLIS)
                                emit(LCE.Content(listOf(stale)))
                            } finally {
                                cancelled += submitted
                            }
                        }
                    } else {
                        flow { emit(LCE.Content(searchResultsFor(submitted, SearchResultKind.LIST))) }
                    }
                },
                gridFlow = { submitted ->
                    if (submitted == "abc") {
                        flow {
                            try {
                                delay(STALE_RESULT_DELAY_MILLIS)
                                emit(LCE.Content(searchResultsFor(submitted, SearchResultKind.GRID)))
                            } finally {
                                cancelled += "grid:$submitted"
                            }
                        }
                    } else {
                        flow { emit(LCE.Content(searchResultsFor(submitted, SearchResultKind.GRID))) }
                    }
                },
            )
            val viewModel = createViewModel()
            val state = viewModel.uiStateActual
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            runCurrent()
            // "abc" is still in flight when the user settles a new query.
            viewModel.sendAction(SageAction.SearchQueryEntered("abcd"))
            advanceUntilIdle()

            assertTrue("the superseded list search should be cancelled", "abc" in cancelled)
            assertTrue("the superseded grid search should be cancelled", "grid:abc" in cancelled)
            assertEquals(listOf("abc", "abcd"), repository.listQueries)
            assertEquals(listOf("abc", "abcd"), repository.gridQueries)
            assertEquals(
                searchResultsFor("abcd", SearchResultKind.LIST).map { it.name },
                state.value.listItems.listNames(),
            )
            assertTrue(state.value.listItems.listNames().none { it == stale.name })
        }

    @Test
    fun `one section can still be loading while the other shows results`() = runTest(dispatcher) {
        repository = FakeSearchRepository(
            listFlow = { submitted ->
                flow {
                    delay(LIST_RESULT_DELAY_MILLIS)
                    emit(LCE.Content(searchResultsFor(submitted, SearchResultKind.LIST)))
                }
            },
            gridFlow = { submitted ->
                flow { emit(LCE.Content(searchResultsFor(submitted, SearchResultKind.GRID))) }
            },
        )
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()

        assertTrue(state.value.listItems.any { it is GridImageListModel })
        val loadingRows = state.value.listItems.filterIsInstance<LoadingItemListModel>()
        assertTrue(loadingRows.isNotEmpty())
        assertTrue(loadingRows.all { it.loadOperationName == SEARCH_LIST_OPERATION_NAME })
        assertTrue(state.value.listItems.none { it is ImageNameCaptionListModel })

        advanceUntilIdle()
        assertEquals(
            searchResultsFor("abc", SearchResultKind.LIST).map { it.name },
            state.value.listItems.listNames(),
        )
        assertTrue(state.value.listItems.none { it is LoadingItemListModel })
    }

    @Test
    fun `a settled query is trimmed before it reaches the repositories`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val query = viewModel.query
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("  abc  "))
        advanceUntilIdle()

        assertEquals("  abc  ", query.value)
        assertEquals(listOf("abc"), repository.listQueries)
        assertEquals(listOf("abc"), repository.gridQueries)
    }

    @Test
    fun `rapid typing only submits the settled query to both repositories`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            viewModel.uiStateActual
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceTimeBy(100)
            viewModel.sendAction(SageAction.SearchQueryEntered("abcd"))
            advanceTimeBy(100)
            viewModel.sendAction(SageAction.SearchQueryEntered("abcde"))
            advanceUntilIdle()

            assertEquals(listOf("abcde"), repository.listQueries)
            assertEquals(listOf("abcde"), repository.gridQueries)
        }

    @Test
    fun `clearing resets the query and both result sections`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        val query = viewModel.query
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceUntilIdle()
        assertTrue(state.value.listItems.any { it is ImageNameCaptionListModel })
        assertTrue(state.value.listItems.any { it is GridImageListModel })

        viewModel.sendAction(SageAction.SearchClearClicked)
        advanceUntilIdle()

        assertEquals("", query.value)
        assertTrue(state.value.listItems.any { it is EmptyStateListModel })
        assertEquals(listOf("abc"), repository.listQueries)
        assertEquals(listOf("abc"), repository.gridQueries)
    }

    @Test
    fun `an error from one query keeps the other section visible`() = runTest(dispatcher) {
        repository = FakeSearchRepository(
            listResponse = {
                LCE.Error(SEARCH_LIST_OPERATION_NAME, IllegalStateException("boom"))
            },
            gridResponse = {
                LCE.Content(searchResultsFor(it, SearchResultKind.GRID))
            },
        )
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceUntilIdle()

        val error = state.value.listItems
            .filterIsInstance<ErrorStateListModel>()
            .single()
        assertEquals(SEARCH_LIST_OPERATION_NAME, error.failedOperationName)
        assertEquals("boom", error.error.message)
        assertTrue(state.value.listItems.any { it is GridImageListModel })
    }

    @Test
    fun `clicking a result emits a snackbar naming it`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }

        viewModel.sendAction(
            SearchAction.ResultClicked(
                SearchResult(id = 7L, name = "Fermentum Pellentesque", caption = "Elias Conrad"),
            ),
        )
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-SEARCH_RESULT_CLICKED:Fermentum Pellentesque", snackbar.message)
        assertEquals("SearchViewModel", snackbar.source)
    }

    @Test
    fun `history from the manager appears in the rendered state`() = runTest(dispatcher) {
        historyManager = FakeSearchHistoryManager(listOf("chrono", "ff7"))
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        assertEquals(
            listOf("chrono", "ff7"),
            state.value.listItems.filterIsInstance<SearchHistoryListModel>().map { it.name },
        )
    }

    @Test
    fun `clicking a history row refills the field and submits after the debounce`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            val query = viewModel.query
            advanceUntilIdle()

            viewModel.sendAction(SearchAction.HistoryClicked("ff7"))
            runCurrent()

            assertEquals("ff7", query.value)
            assertTrue(repository.listQueries.isEmpty())

            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            runCurrent()

            assertEquals(listOf("ff7"), repository.listQueries)
            assertEquals(listOf("ff7"), repository.gridQueries)
        }

    @Test
    fun `removing a history row updates the manager and the rendered list`() =
        runTest(dispatcher) {
            historyManager = FakeSearchHistoryManager(listOf("chrono", "ff7"))
            val viewModel = createViewModel()
            val state = viewModel.uiStateActual
            advanceUntilIdle()

            viewModel.sendAction(SearchAction.HistoryRemoved("chrono"))
            advanceUntilIdle()

            assertEquals(listOf("chrono"), historyManager.removed)
            assertEquals(
                listOf("ff7"),
                state.value.listItems.filterIsInstance<SearchHistoryListModel>().map { it.name },
            )
        }

    @Test
    fun `clearing the field preserves the rendered history`() = runTest(dispatcher) {
        historyManager = FakeSearchHistoryManager(listOf("chrono"))
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()
        viewModel.sendAction(SageAction.SearchClearClicked)
        advanceUntilIdle()

        assertEquals(
            listOf("chrono"),
            state.value.listItems.filterIsInstance<SearchHistoryListModel>().map { it.name },
        )
    }

    @Test
    fun `a settled query with results is recorded once after the linger window`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            runCurrent()

            assertTrue("history must wait for the linger window", historyManager.recorded.isEmpty())

            advanceTimeBy(SEARCH_HISTORY_RECORD_DELAY_MILLIS + 1)
            runCurrent()
            assertEquals(listOf("abc"), historyManager.recorded)

            advanceTimeBy(SEARCH_HISTORY_RECORD_DELAY_MILLIS * 2)
            runCurrent()
            assertEquals("the record must happen exactly once", listOf("abc"), historyManager.recorded)
        }

    @Test
    fun `a settled query with no results is not recorded`() = runTest(dispatcher) {
        repository = FakeSearchRepository(
            listResponse = { LCE.Content(emptyList()) },
            gridResponse = { LCE.Content(emptyList()) },
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("nothing-matches"))
        advanceUntilIdle()

        assertTrue(historyManager.recorded.isEmpty())
    }

    @Test
    fun `a new settled query cancels the pending record for the previous query`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            runCurrent()
            advanceTimeBy(1_500)

            viewModel.sendAction(SageAction.SearchQueryEntered("abcd"))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            runCurrent()
            advanceTimeBy(SEARCH_HISTORY_RECORD_DELAY_MILLIS + 1)
            runCurrent()

            assertEquals(listOf("abcd"), historyManager.recorded)
        }

    @Test
    fun `clearing the field cancels the pending record`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendAction(SageAction.SearchQueryEntered("abc"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        runCurrent()
        advanceTimeBy(1_500)

        viewModel.sendAction(SageAction.SearchClearClicked)
        advanceTimeBy(SEARCH_HISTORY_RECORD_DELAY_MILLIS * 2)
        runCurrent()

        assertTrue(historyManager.recorded.isEmpty())
    }

    private fun List<ListModel>.listNames(): List<String> =
        filterIsInstance<ImageNameCaptionListModel>().map { it.name }

    private fun List<ListModel>.gridNames(): List<String> =
        filterIsInstance<GridImageListModel>().map { it.name }

    private fun createViewModel() = SearchViewModel(
        stringProvider = FakeStringProvider(),
        analytics = FakeAnalytics(),
        dispatchers = SageDispatchers(
            computation = dispatcher,
            disk = dispatcher,
            network = dispatcher,
            main = dispatcher,
        ),
        delayManager = object : SampleDelayManager {
            override fun delayMillis() = 0L
        },
        repository = repository,
        historyManager = historyManager,
    )

    private companion object {
        const val STALE_RESULT_DELAY_MILLIS = 1_000L
        const val LIST_RESULT_DELAY_MILLIS = 1_000L
    }
}
