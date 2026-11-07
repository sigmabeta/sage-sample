package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.ui.Icon
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The module fan-in contract: each module's flow owns one section slot, sections render in
 * priority order, and a row tap becomes a one-shot snackbar event.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `each module patches only its own section slot`() = runTest(dispatcher) {
        val lateModule = MutableSharedFlow<LCE<HomeModuleSection>>(extraBufferCapacity = 1)
        val viewModel = createViewModel(
            listOf(
                FakeHomeModule("first", 0, flowOf(LCE.Content(section("First", 1L)))),
                FakeHomeModule("second", 1, lateModule),
            ),
        )

        advanceUntilIdle()
        assertEquals(listOf("First"), renderedHeaders(viewModel))
        assertEquals(listOf("Item 1"), renderedImageNames(viewModel))

        lateModule.emit(LCE.Content(section("Second", 2L)))
        advanceUntilIdle()

        // The late module filled its own slot without disturbing the first module's content.
        assertEquals(listOf("First", "Second"), renderedHeaders(viewModel))
        assertEquals(listOf("Item 1", "Item 2"), renderedImageNames(viewModel))
    }

    @Test
    fun `sections render in module priority order regardless of injection order`() =
        runTest(dispatcher) {
            val viewModel = createViewModel(
                listOf(
                    FakeHomeModule("later", 20, flowOf(LCE.Content(section("Later", 2L)))),
                    FakeHomeModule("earlier", 10, flowOf(LCE.Content(section("Earlier", 1L)))),
                ),
            )

            advanceUntilIdle()

            assertEquals(listOf("Earlier", "Later"), renderedHeaders(viewModel))
        }

    @Test
    fun `an item tap emits a snackbar naming the item`() = runTest(dispatcher) {
        val viewModel = createViewModel(emptyList())
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events += it }
        }
        advanceUntilIdle()

        viewModel.sendAction(HomeAction.ItemClicked("Item 7"))
        advanceUntilIdle()

        assertEquals(
            listOf(
                SageEvent.ShowSnackbar(
                    message = "str-HOME_ITEM_CLICKED:Item 7",
                    withDismissAction = false,
                    source = "HomeViewModel",
                ),
            ),
            events,
        )
    }

    // Touch the mapped state before the test advances the scheduler: `uiStateActual` is eager,
    // but its first collection has to be scheduled while time can still advance.
    private fun createViewModel(modules: List<HomeModule>) = HomeViewModel(
        modules = modules,
        stringProvider = FakeStringProvider(),
        analytics = FakeAnalytics(),
        dispatchers = testDispatchers(dispatcher),
        delayManager = object : SampleDelayManager {
            override fun delayMillis() = 0L
        },
    ).also { it.uiStateActual }

    private fun renderedHeaders(viewModel: HomeViewModel): List<String> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<SectionHeaderListModel>()
            .map { it.title }

    private fun renderedImageNames(viewModel: HomeViewModel): List<String> =
        viewModel.uiStateActual.value.listItems
            .filterIsInstance<GridImageListModel>()
            .map { it.name }

    private fun section(title: String, id: Long) = HomeModuleSection(
        title = title,
        items = persistentListOf(bigImage(id)),
    )

    private fun bigImage(id: Long): ListModel = GridImageListModel(
        dataId = id,
        name = "Item $id",
        sourceInfo = SourceInfo("generated://home/$id"),
        imagePlaceholder = Icon.Album,
        clickAction = HomeAction.ItemClicked("Item $id"),
        aspectRatio = 0.75f,
        maxWidthDp = 400f,
    )
}
