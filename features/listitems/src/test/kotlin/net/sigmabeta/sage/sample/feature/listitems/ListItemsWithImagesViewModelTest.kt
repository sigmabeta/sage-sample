package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

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
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.EmptyStateListModel
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
    private lateinit var pagedRepository: ControlledSampleItemRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeSampleItemRepository()
        pagedRepository = ControlledSampleItemRepository()
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

    @Test
    fun `the initial load requests only the first page`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()

        assertEquals(
            listOf(PageRequest(0, LIST_ITEMS_WITH_IMAGES_PAGE_SIZE)),
            pagedRepository.requests,
        )

        pagedRepository.emitPage(0, LCE.Content(pagedItems(1..10)))
        advanceUntilIdle()

        assertEquals((1L..10L).toList(), imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `load more appends the next page and drops the boundary loader`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()

        assertEquals(PageRequest(10, 10), pagedRepository.requests.last())
        assertTrue(
            loaderOperations(viewModel).contains(
                "$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_more",
            ),
        )

        pagedRepository.emitLatest(LCE.Content(pagedItems(11..20)))
        advanceUntilIdle()

        assertEquals((1L..20L).toList(), imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `appending a page with overlapping ids keeps each row once`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(pagedItems(6..15)))
        advanceUntilIdle()

        assertEquals((1L..15L).toList(), imageIds(viewModel))
        assertEquals(15, imageIds(viewModel).distinct().size)
    }

    @Test
    fun `a burst of load more requests starts one page request`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()

        assertEquals(2, pagedRepository.requests.size)
        assertEquals(PageRequest(10, 10), pagedRepository.requests.last())
    }

    @Test
    fun `a failed page keeps existing rows and drops the loader`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        assertTrue(loaderOperations(viewModel).isNotEmpty())

        pagedRepository.emitLatest(
            LCE.Error(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
        )
        advanceUntilIdle()

        assertEquals((1L..10L).toList(), imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
        assertTrue(viewModel.uiStateActual.value.listItems.none { it is ErrorStateListModel })
    }

    @Test
    fun `an empty next page keeps rows and stops paging`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(emptyList()))
        advanceUntilIdle()

        assertEquals((1L..10L).toList(), imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        assertEquals(2, pagedRepository.requests.size)
    }

    @Test
    fun `a short final page still appends but stops further requests`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(pagedItems(11..15)))
        advanceUntilIdle()

        assertEquals((1L..15L).toList(), imageIds(viewModel))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        assertEquals(2, pagedRepository.requests.size)
    }

    @Test
    fun `an initial empty page shows the empty state and never pages`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(0, LCE.Content(emptyList()))
        advanceUntilIdle()

        val rows = viewModel.uiStateActual.value.listItems
        assertTrue(rows.single() is EmptyStateListModel)
        assertTrue(imageIds(viewModel).isEmpty())
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `an initial error page shows the error state and never pages`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(
            0,
            LCE.Error(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
        )
        advanceUntilIdle()

        val rows = viewModel.uiStateActual.value.listItems
        assertEquals(
            LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
            (rows.single() as ErrorStateListModel).failedOperationName,
        )
        assertTrue(imageIds(viewModel).isEmpty())
    }

    @Test
    fun `prepending at offset zero is a no-op`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..10))
        val before = imageIds(viewModel)

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()

        assertEquals(1, pagedRepository.requests.size)
        assertEquals(before, imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `a non-zero window prepends earlier rows`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(0, LCE.Content(pagedItems(1..10)))
        advanceUntilIdle()

        viewModel.sendAction(SageAction.InitWithPageNumber(0L, 1L))
        advanceUntilIdle()
        assertEquals(PageRequest(10, 10), pagedRepository.requests.last())
        pagedRepository.emitLatest(LCE.Content(pagedItems(11..20)))
        advanceUntilIdle()

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()
        assertEquals(PageRequest(0, 10), pagedRepository.requests.last())
        assertTrue(
            loaderOperations(viewModel).contains(
                "$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_previous",
            ),
        )

        pagedRepository.emitLatest(LCE.Content(pagedItems(1..10)))
        advanceUntilIdle()

        assertEquals((1L..20L).toList(), imageIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()
        assertEquals(3, pagedRepository.requests.size)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loadedViewModel(
        items: List<SampleItem>,
    ): ListItemsWithImagesViewModel {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(0, LCE.Content(items))
        advanceUntilIdle()
        return viewModel
    }

    private fun imageIds(viewModel: ListItemsWithImagesViewModel): List<Long> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<ImageNameCaptionListModel>()
            .map { it.dataId }

    private fun loaderOperations(viewModel: ListItemsWithImagesViewModel): List<String> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<LoadingItemListModel>()
            .map { it.loadOperationName }

    private fun createViewModel(
        repository: SampleItemRepository = this.repository,
    ) = ListItemsWithImagesViewModel(
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
    ).also { it.uiStateActual }
}
