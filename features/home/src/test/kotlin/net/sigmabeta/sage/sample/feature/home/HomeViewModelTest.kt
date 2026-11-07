package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
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
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.ui.Icon
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The module fan-in contract: each module's flow owns one section slot, sections render in
 * priority order, a row tap becomes a one-shot snackbar event, and flipping "Home shows same
 * content" rebuilds the plan without recreating the screen.
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
                fakeDefinition("first", 0, flowOf(LCE.Content(section("First", 1L)))),
                fakeDefinition("second", 1, lateModule),
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
                    fakeDefinition("later", 20, flowOf(LCE.Content(section("Later", 2L)))),
                    fakeDefinition("earlier", 10, flowOf(LCE.Content(section("Earlier", 1L)))),
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

    @Test
    fun `flipping home shows same content rebuilds the plan and cancels old collections`() =
        runTest(dispatcher) {
            val builds = mutableMapOf<String, Int>()
            val cancelledInstances = mutableListOf<String>()

            fun countingDefinition(
                typeId: String,
                priority: Int,
                title: String,
                itemBase: Long,
            ) = FakeHomeModuleDefinition(typeId, priority) { instanceNumber, _ ->
                val build = builds.merge(typeId, 1, Int::plus)!!
                // A replaying flow never completes while Home is collecting it, so its
                // onCompletion runs when the plan rebuild cancels the collector.
                val updates = MutableSharedFlow<LCE<HomeModuleSection>>(replay = 1)
                val stateFlow = updates
                    .onCompletion { cancelledInstances += "${typeId}_$instanceNumber" }
                updates.tryEmit(
                    LCE.Content(section("$title:$build", itemBase + build)),
                )

                    FakeHomeModule(
                        id = "${typeId}_$instanceNumber",
                        priority = instanceNumber - 1,
                        flow = stateFlow,
                    )
                }

            val settings = FakeHomeSettingsManager(initialHomeShowsSameContent = true)
            val viewModel = createViewModel(
                listOf(
                    countingDefinition("hero", 0, "Hero", itemBase = 0L),
                    countingDefinition("gallery", 1, "Gallery", itemBase = 1_000L),
                ),
                settings,
            )
            advanceUntilIdle()

            assertEquals(listOf("Hero:1", "Gallery:1"), renderedHeaders(viewModel))

            settings.setHomeShowsSameContent(false)
            advanceUntilIdle()

            // The stable instances were cancelled, and every rendered header comes from a
            // freshly created plan (build 2 or later).
            assertEquals(2, cancelledInstances.size)
            assertTrue(builds.getValue("hero") >= 2)
            assertTrue(builds.getValue("gallery") >= 2)
            val headers = renderedHeaders(viewModel)
            assertTrue(headers.isNotEmpty())
            assertTrue(headers.none { it.endsWith(":1") })
        }

    // Touch the mapped state before the test advances the scheduler: `uiStateActual` is eager,
    // but its first collection has to be scheduled while time can still advance.
    private fun createViewModel(
        definitions: List<HomeModuleDefinition>,
        settings: SampleSettingsManager = FakeHomeSettingsManager(),
    ) = HomeViewModel(
        moduleFactory = HomeModuleFactory(definitions.toSet()),
        settingsManager = settings,
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
