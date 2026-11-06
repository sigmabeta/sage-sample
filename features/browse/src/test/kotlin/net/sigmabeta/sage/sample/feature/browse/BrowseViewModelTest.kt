package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repository: FakeBrowseRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeBrowseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads content through the action and state path`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiStateActual.value.listItems.any { it is LoadingItemListModel })

        repository.emit(
            LCE.Content(
                listOf(
                    BrowseItem(id = 1L, name = "Alpha"),
                    BrowseItem(id = 2L, name = "Beta"),
                ),
            ),
        )
        advanceUntilIdle()

        val contentRows = viewModel.uiStateActual.value.listItems
            .filterIsInstance<SingleTextListModel>()
            .filter {
                it.dataId != BrowseState.OPEN_LIST_ITEMS_DATA_ID &&
                    it.dataId != BrowseState.OPEN_LIST_ITEMS_WITH_IMAGES_DATA_ID &&
                    it.dataId != BrowseState.OPEN_GRID_OF_IMAGES_DATA_ID
            }

        assertEquals(listOf(1L, 2L), contentRows.map { it.dataId })
        assertEquals(listOf("Alpha", "Beta"), contentRows.map { it.name })
    }

    @Test
    fun `empty content publishes the empty state`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        repository.emit(LCE.Content(emptyList()))
        advanceUntilIdle()

        assertTrue(state.value.listItems.any { it is EmptyStateListModel })
    }

    @Test
    fun `opening the grid emits its navigation route`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        advanceUntilIdle()

        viewModel.sendAction(BrowseAction.OpenGridOfImages)
        advanceUntilIdle()

        val navigation = events.filterIsInstance<SageEvent.NavigateTo>().single()
        assertEquals("grid-of-images", navigation.destination)
        assertEquals("BrowseViewModel", navigation.source)
    }

    @Test
    fun `an error publishes the error state`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        repository.emit(LCE.Error("browse", IllegalStateException("boom")))
        advanceUntilIdle()

        val errorState = state.value.listItems.filterIsInstance<ErrorStateListModel>().single()
        assertEquals("browse", errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }

    private fun createViewModel() = BrowseViewModel(
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
