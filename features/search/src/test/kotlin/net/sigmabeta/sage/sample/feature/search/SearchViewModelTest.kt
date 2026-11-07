package net.sigmabeta.sage.sample.feature.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
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

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeSearchRepository()
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
    )
}
