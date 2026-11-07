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
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListItemsWithImagesViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repository: FakeSampleItemRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeSampleItemRepository()
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
            .filterIsInstance<ImageNameCaptionListModel>()

        assertEquals(listOf(1L, 2L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf("Fermentum pellentesque iaculis", "Lorem ipsum dolor"),
            rows.map { it.caption },
        )
        assertEquals(
            listOf(
                "generated://list-items-with-images/1?width=128&height=128",
                "generated://list-items-with-images/2?width=128&height=128",
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
                LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        )
        advanceUntilIdle()

        val errorState = state.value.listItems.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }

    @Test
    fun `clicking an item opens the detail for its own stable id`() = runTest {
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

        viewModel.sendAction(ListItemsWithImagesAction.ItemClicked(item))
        advanceUntilIdle()

        val navigation = events.filterIsInstance<SageEvent.NavigateTo>().single()
        assertEquals("content-detail/list-items-with-images:7", navigation.destination)
        assertEquals("ListItemsWithImagesViewModel", navigation.source)
        assertTrue(events.none { it is SageEvent.ShowSnackbar })
    }

    private fun createViewModel() = ListItemsWithImagesViewModel(
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
