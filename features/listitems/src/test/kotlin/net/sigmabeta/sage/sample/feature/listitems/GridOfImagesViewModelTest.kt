package net.sigmabeta.sage.sample.feature.listitems

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
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GridOfImagesViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repository: FakeGridOfImagesRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeGridOfImagesRepository()
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
                    SampleItem(
                        id = 1L,
                        name = "Sleve McDichael",
                        caption = "Fermentum pellentesque iaculis",
                    ),
                    SampleItem(
                        id = 2L,
                        name = "Exie Clayton",
                        caption = "Lorem ipsum dolor",
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        val rows = viewModel.uiStateActual.value.listItems
            .filterIsInstance<GridImageListModel>()

        assertEquals(listOf(1L, 2L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf(
                "generated://grid-of-images/1?width=256&height=256",
                "generated://grid-of-images/2?width=256&height=256",
            ),
            rows.map { it.sourceInfo.info },
        )
    }

    @Test
    fun `an error publishes the error state`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        repository.emit(
            LCE.Error(
                GRID_OF_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        )
        advanceUntilIdle()

        val errorState = state.value.listItems.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(GRID_OF_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }

    @Test
    fun `clicking an image emits a snackbar naming it`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        val item = SampleItem(
            id = 7L,
            name = "Sleve McDichael",
            caption = "Fermentum pellentesque iaculis",
        )

        viewModel.sendAction(GridOfImagesAction.ItemClicked(item))
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-GRID_OF_IMAGES_ITEM_CLICKED:Sleve McDichael", snackbar.message)
        assertEquals("GridOfImagesViewModel", snackbar.source)
    }

    private fun createViewModel() = GridOfImagesViewModel(
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
