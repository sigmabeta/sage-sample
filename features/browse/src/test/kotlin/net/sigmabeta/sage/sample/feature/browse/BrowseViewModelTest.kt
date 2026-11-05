package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
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

        assertTrue(viewModel.uiStateActual.value.listItems.first() is LoadingItemListModel)

        repository.emit(
            BrowseOutcome.CONTENT,
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
            .filter { it.clickAction is SageAction.Noop }

        assertEquals(listOf(1L, 2L), contentRows.map { it.dataId })
        assertEquals(listOf("Alpha", "Beta"), contentRows.map { it.name })
    }

    @Test
    fun `load empty publishes the empty state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendAction(BrowseAction.LoadEmpty)
        advanceUntilIdle()

        assertTrue(viewModel.uiStateActual.value.listItems.first() is LoadingItemListModel)

        repository.emit(BrowseOutcome.EMPTY, LCE.Content(emptyList()))
        advanceUntilIdle()

        assertTrue(viewModel.uiStateActual.value.listItems.first() is EmptyStateListModel)
    }

    @Test
    fun `load error publishes the error state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendAction(BrowseAction.LoadError)
        advanceUntilIdle()

        assertTrue(viewModel.uiStateActual.value.listItems.first() is LoadingItemListModel)

        repository.emit(
            BrowseOutcome.ERROR,
            LCE.Error("browse", IllegalStateException("boom")),
        )
        advanceUntilIdle()

        val errorState = viewModel.uiStateActual.value.listItems.first() as ErrorStateListModel
        assertEquals("browse", errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }

    @Test
    fun `a new load cancels the previous collector so stale outcomes cannot publish`() =
        runTest {
            val viewModel = createViewModel()
            // Start the state flow's eager collection before any emission.
            val state = viewModel.uiStateActual
            advanceUntilIdle()

            // The init load (CONTENT) is still collecting. Start a second load; the fix cancels
            // the first collection instead of leaving both alive.
            viewModel.sendAction(BrowseAction.LoadEmpty)
            advanceUntilIdle()

            repository.emit(BrowseOutcome.EMPTY, LCE.Content(emptyList()))
            advanceUntilIdle()
            assertTrue(state.value.listItems.first() is EmptyStateListModel)

            // A late value on the superseded stream must not overwrite the current outcome.
            repository.emit(
                BrowseOutcome.CONTENT,
                LCE.Content(listOf(BrowseItem(id = 1L, name = "Stale"))),
            )
            advanceUntilIdle()

            assertTrue(
                "the superseded CONTENT load must not publish after LoadEmpty",
                state.value.listItems.first() is EmptyStateListModel,
            )
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
