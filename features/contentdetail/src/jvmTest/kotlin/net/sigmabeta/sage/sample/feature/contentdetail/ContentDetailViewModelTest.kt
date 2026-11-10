package net.sigmabeta.sage.sample.feature.contentdetail
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.CONTENT_DETAIL_OPERATION_NAME

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
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
import net.sigmabeta.sage.components.SectionListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentDetailViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repository: FakeContentDetailRepository

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        repository = FakeContentDetailRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init no args does not load anything`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(repository.requestedIds.isEmpty())
        assertTrue(viewModel.uiStateActual.value.listItems.isNotEmpty())
    }

    @Test
    fun `init with string loads the addressed content`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()
        val id = ContentId("list-items:7")

        viewModel.sendAction(SageAction.InitWithString(id.value))
        advanceUntilIdle()

        assertEquals(listOf(id), repository.requestedIds)

        repository.emit(id, LCE.Content(CONTENT_DETAIL_FIXTURE.copy(id = id)))
        advanceUntilIdle()

        val sections = state.value.listItems.filterIsInstance<SectionListModel>()
        assertEquals(5, sections.size)
        assertEquals(CONTENT_DETAIL_FIXTURE.title, state.value.title.title)
        val items = sections.last().sectionItems
            .filterIsInstance<ImageNameCaptionListModel>()
        assertEquals(listOf("One", "Two"), items.map { it.name })
    }

    @Test
    fun `a blank init string shows the not-found state without a load`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.InitWithString(""))
        advanceUntilIdle()

        assertTrue(repository.requestedIds.isEmpty())
        val empty = state.value.listItems
            .filterIsInstance<EmptyStateListModel>()
            .single()
        assertEquals("str-CONTENT_DETAIL_NOT_FOUND", empty.explanation)
    }

    @Test
    fun `an invalid init string shows the not-found state without a load`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        viewModel.sendAction(SageAction.InitWithString("not a content id"))
        advanceUntilIdle()

        assertTrue(repository.requestedIds.isEmpty())
        assertTrue(
            state.value.listItems
                .filterIsInstance<EmptyStateListModel>()
                .isNotEmpty(),
        )
    }

    @Test
    fun `a second init string does not keep serving the first id's content`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()
        val first = ContentId("list-items:1")
        val second = ContentId("list-items:2")

        viewModel.sendAction(SageAction.InitWithString(first.value))
        advanceUntilIdle()
        repository.emit(first, LCE.Content(CONTENT_DETAIL_FIXTURE.copy(title = "First")))
        advanceUntilIdle()
        assertEquals("First", state.value.title.title)

        viewModel.sendAction(SageAction.InitWithString(second.value))
        advanceUntilIdle()
        assertEquals(listOf(first, second), repository.requestedIds)

        // The first ID's stream is still live in the test; its late emission must not overwrite
        // the second ID's loading state.
        repository.emit(first, LCE.Content(CONTENT_DETAIL_FIXTURE.copy(title = "Stale")))
        advanceUntilIdle()
        assertTrue(state.value.title.title != "Stale")

        repository.emit(second, LCE.Content(CONTENT_DETAIL_FIXTURE.copy(title = "Second")))
        advanceUntilIdle()
        assertEquals("Second", state.value.title.title)
    }

    @Test
    fun `an error publishes the error state`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()
        val id = ContentId("list-items:7")

        viewModel.sendAction(SageAction.InitWithString(id.value))
        advanceUntilIdle()
        repository.emit(id, LCE.Error(CONTENT_DETAIL_OPERATION_NAME, IllegalStateException("boom")))
        advanceUntilIdle()

        val errors = state.value.listItems.filterIsInstance<ErrorStateListModel>()
        assertEquals(1, errors.size)
        assertTrue(errors.all { it.error.message == "boom" })
    }

    @Test
    fun `clicking a part opens the detail for its content id`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.sendAction(
            ContentDetailAction.ItemClicked(
                ContentId("content-detail:item:list-items:1:1"),
            ),
        )
        advanceUntilIdle()

        val navigation = events.filterIsInstance<SageEvent.NavigateTo>().single()
        assertEquals(
            "content-detail/content-detail:item:list-items:1:1",
            navigation.destination,
        )
        assertEquals("ContentDetailViewModel", navigation.source)
        assertTrue(events.none { it is SageEvent.ShowSnackbar })
    }

    @Test
    fun `clicking a related item opens another detail`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.sendAction(
            ContentDetailAction.RelatedClicked(
                ContentId("content-detail:related:list-items:1:1"),
            ),
        )
        advanceUntilIdle()

        val navigation = events.filterIsInstance<SageEvent.NavigateTo>().single()
        assertEquals(
            "content-detail/content-detail:related:list-items:1:1",
            navigation.destination,
        )
        assertEquals("ContentDetailViewModel", navigation.source)
        assertTrue(events.none { it is SageEvent.ShowSnackbar })
    }

    @Test
    fun `clicking a generated cta emits a snackbar with its label`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.sendAction(ContentDetailAction.CtaClicked("First action"))
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-CONTENT_DETAIL_ITEM_CLICKED:First action", snackbar.message)
    }

    @Test
    fun `clicking the hero emits a snackbar with the content title`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        val events = collectEvents(viewModel)
        advanceUntilIdle()
        val id = ContentId("list-items:7")
        viewModel.sendAction(SageAction.InitWithString(id.value))
        advanceUntilIdle()
        repository.emit(id, LCE.Content(CONTENT_DETAIL_FIXTURE))
        advanceUntilIdle()

        viewModel.sendAction(ContentDetailAction.HeroClicked)
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-CONTENT_DETAIL_ITEM_CLICKED:Fugiat lorem ipsum", snackbar.message)
        assertEquals(CONTENT_DETAIL_FIXTURE.title, state.value.title.title)
    }

    private fun TestScope.collectEvents(
        viewModel: ContentDetailViewModel,
    ): MutableList<SageEvent> {
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        return events
    }

    private fun createViewModel() = ContentDetailViewModel(
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
