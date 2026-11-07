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
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.EmptyStateListModel
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
    private lateinit var pagedRepository: ControlledGridOfImagesRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeGridOfImagesRepository()
        pagedRepository = ControlledGridOfImagesRepository()
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

    @Test
    fun `the initial load requests only the first page`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()

        assertEquals(
            listOf(PageRequest(0, GRID_OF_IMAGES_PAGE_SIZE)),
            pagedRepository.requests,
        )

        pagedRepository.emitPage(0, LCE.Content(pagedItems(1..20)))
        advanceUntilIdle()

        assertEquals((1L..20L).toList(), cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `load more appends the next page and drops the boundary loader`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()

        assertEquals(PageRequest(20, 20), pagedRepository.requests.last())
        assertTrue(
            loaderOperations(viewModel).contains("$GRID_OF_IMAGES_OPERATION_NAME.load_more"),
        )

        pagedRepository.emitLatest(LCE.Content(pagedItems(21..40)))
        advanceUntilIdle()

        assertEquals((1L..40L).toList(), cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `appending a page with overlapping ids keeps each cell once`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(pagedItems(11..30)))
        advanceUntilIdle()

        assertEquals((1L..30L).toList(), cellIds(viewModel))
        assertEquals(30, cellIds(viewModel).distinct().size)
    }

    @Test
    fun `a burst of load more requests starts one page request`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()

        assertEquals(2, pagedRepository.requests.size)
        assertEquals(PageRequest(20, 20), pagedRepository.requests.last())
    }

    @Test
    fun `a failed page keeps existing cells and drops the loader`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        assertTrue(loaderOperations(viewModel).isNotEmpty())

        pagedRepository.emitLatest(
            LCE.Error(GRID_OF_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
        )
        advanceUntilIdle()

        assertEquals((1L..20L).toList(), cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
        assertTrue(viewModel.uiStateActual.value.listItems.none { it is ErrorStateListModel })
    }

    @Test
    fun `an empty next page keeps cells and stops paging`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(emptyList()))
        advanceUntilIdle()

        assertEquals((1L..20L).toList(), cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        assertEquals(2, pagedRepository.requests.size)
    }

    @Test
    fun `a short final page still appends but stops further requests`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))

        viewModel.sendAction(SageAction.LoadMoreRequested)
        advanceUntilIdle()
        pagedRepository.emitLatest(LCE.Content(pagedItems(21..30)))
        advanceUntilIdle()

        assertEquals((1L..30L).toList(), cellIds(viewModel))

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
        assertTrue(cellIds(viewModel).isEmpty())
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `an initial error page shows the error state and never pages`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(
            0,
            LCE.Error(GRID_OF_IMAGES_OPERATION_NAME, IllegalStateException("boom")),
        )
        advanceUntilIdle()

        val rows = viewModel.uiStateActual.value.listItems
        assertEquals(
            GRID_OF_IMAGES_OPERATION_NAME,
            (rows.single() as ErrorStateListModel).failedOperationName,
        )
        assertTrue(cellIds(viewModel).isEmpty())
    }

    @Test
    fun `prepending at offset zero is a no-op`() = runTest {
        val viewModel = loadedViewModel(pagedItems(1..20))
        val before = cellIds(viewModel)

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()

        assertEquals(1, pagedRepository.requests.size)
        assertEquals(before, cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())
    }

    @Test
    fun `a non-zero window prepends earlier cells`() = runTest {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(0, LCE.Content(pagedItems(1..20)))
        advanceUntilIdle()

        viewModel.sendAction(SageAction.InitWithPageNumber(0L, 1L))
        advanceUntilIdle()
        assertEquals(PageRequest(20, 20), pagedRepository.requests.last())
        pagedRepository.emitLatest(LCE.Content(pagedItems(21..40)))
        advanceUntilIdle()

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()
        assertEquals(PageRequest(0, 20), pagedRepository.requests.last())
        assertTrue(
            loaderOperations(viewModel).contains("$GRID_OF_IMAGES_OPERATION_NAME.load_previous"),
        )

        pagedRepository.emitLatest(LCE.Content(pagedItems(1..20)))
        advanceUntilIdle()

        assertEquals((1L..40L).toList(), cellIds(viewModel))
        assertTrue(loaderOperations(viewModel).isEmpty())

        viewModel.sendAction(SageAction.LoadPreviousRequested)
        advanceUntilIdle()
        assertEquals(3, pagedRepository.requests.size)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loadedViewModel(
        items: List<SampleItem>,
    ): GridOfImagesViewModel {
        val viewModel = createViewModel(pagedRepository)
        advanceUntilIdle()
        pagedRepository.emitPage(0, LCE.Content(items))
        advanceUntilIdle()
        return viewModel
    }

    private fun cellIds(viewModel: GridOfImagesViewModel): List<Long> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<GridImageListModel>()
            .map { it.dataId }

    private fun loaderOperations(viewModel: GridOfImagesViewModel): List<String> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<LoadingItemListModel>()
            .map { it.loadOperationName }

    private fun createViewModel(
        repository: GridOfImagesRepository = this.repository,
    ) = GridOfImagesViewModel(
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
