package net.sigmabeta.sage.sample.feature.contentdetail

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
    fun `init loads content through the action and state path`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        repository.emit(LCE.Content(CONTENT_DETAIL_FIXTURE))
        advanceUntilIdle()

        val rows = state.value.listItems
        val sections = rows.filterIsInstance<SectionListModel>()
        assertEquals(5, sections.size)

        val items = sections.last().sectionItems
            .filterIsInstance<ImageNameCaptionListModel>()
        assertEquals(listOf("One", "Two"), items.map { it.name })
    }

    @Test
    fun `an error publishes the error state`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiStateActual
        advanceUntilIdle()

        repository.emit(
            LCE.Error(
                CONTENT_DETAIL_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        )
        advanceUntilIdle()

        val errors = state.value.listItems
            .filterIsInstance<ErrorStateListModel>()
        assertEquals(1, errors.size)
        assertTrue(errors.all { it.error.message == "boom" })
    }

    @Test
    fun `clicking a part emits a snackbar naming it`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        advanceUntilIdle()

        viewModel.sendAction(ContentDetailAction.ItemClicked(CONTENT_DETAIL_FIXTURE.items[0]))
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-CONTENT_DETAIL_ITEM_CLICKED:One", snackbar.message)
        assertEquals("ContentDetailViewModel", snackbar.source)
    }

    @Test
    fun `clicking a related item emits a snackbar naming it`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        advanceUntilIdle()

        viewModel.sendAction(ContentDetailAction.RelatedClicked(CONTENT_DETAIL_FIXTURE.related[0]))
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-CONTENT_DETAIL_ITEM_CLICKED:Alpha", snackbar.message)
    }

    @Test
    fun `clicking a generated cta emits a snackbar with its label`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
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
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        advanceUntilIdle()
        repository.emit(LCE.Content(CONTENT_DETAIL_FIXTURE))
        advanceUntilIdle()

        viewModel.sendAction(ContentDetailAction.HeroClicked)
        advanceUntilIdle()

        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals("str-CONTENT_DETAIL_ITEM_CLICKED:Fugiat lorem ipsum", snackbar.message)
        assertEquals(CONTENT_DETAIL_FIXTURE.title, state.value.title.title)
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
