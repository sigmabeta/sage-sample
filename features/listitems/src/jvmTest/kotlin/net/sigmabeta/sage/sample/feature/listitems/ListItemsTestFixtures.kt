package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.repository.GridOfImagesRepository
import net.sigmabeta.sage.sample.repository.SampleItemRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.settings.DEFAULT_HOME_SHOWS_SAME_CONTENT
import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

internal class FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = "str-$string"

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        "${getString(string)}:$arg"

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        error("unused")

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        error("unused")
}

/**
 * Settings seam used by repository tests that read a preference at call time.
 *
 * Only the demo outcome is mutable here; the rest mirror the manager's defaults so the sample's
 * preferences have one fake vocabulary in this module.
 */
internal class FakeSampleSettingsManager(
    initialDemoOutcome: DemoOutcome = DemoOutcome.DEFAULT,
) : SampleSettingsManager {
    private val demoOutcomeState = MutableStateFlow(initialDemoOutcome)
    private val themeModeState = MutableStateFlow(ThemeMode.DEFAULT)
    private val fontChoiceState = MutableStateFlow(FontChoice.DEFAULT)
    private val simulateDelayState = MutableStateFlow(SimulateDelay.DEFAULT)
    private val animationScaleState = MutableStateFlow(AnimationScale.DEFAULT)
    private val homeShowsSameContentState = MutableStateFlow(DEFAULT_HOME_SHOWS_SAME_CONTENT)

    override val demoOutcome: StateFlow<DemoOutcome> = demoOutcomeState.asStateFlow()
    override val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()
    override val fontChoice: StateFlow<FontChoice> = fontChoiceState.asStateFlow()
    override val simulateDelay: StateFlow<SimulateDelay> = simulateDelayState.asStateFlow()
    override val animationScale: StateFlow<AnimationScale> = animationScaleState.asStateFlow()
    override val homeShowsSameContent: StateFlow<Boolean> = homeShowsSameContentState.asStateFlow()

    override fun setDemoOutcome(outcome: DemoOutcome) {
        demoOutcomeState.value = outcome
    }

    override fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
    }

    override fun setFontChoice(choice: FontChoice) {
        fontChoiceState.value = choice
    }

    override fun setSimulateDelay(delay: SimulateDelay) {
        simulateDelayState.value = delay
    }

    override fun setAnimationScale(scale: AnimationScale) {
        animationScaleState.value = scale
    }

    override fun setHomeShowsSameContent(showSameContent: Boolean) {
        homeShowsSameContentState.value = showSameContent
    }
}

internal class FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit

    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit

    override fun logEvent(event: SageEvent) = Unit

    override fun logAutoRefresh() = Unit

    override fun logError(
        failedOperationName: String,
        errorString: String,
        error: Throwable,
    ) = Unit
}

/**
 * Repository seam used by the view model tests.
 *
 * The stream is a replayed [MutableSharedFlow], so the test decides exactly when a value reaches
 * the view model. No real delays, no background threads.
 */
internal class FakeSampleItemRepository : SampleItemRepository {
    private val stream = MutableSharedFlow<LCE<List<SampleItem>>>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> = stream

    suspend fun emit(value: LCE<List<SampleItem>>) {
        stream.emit(value)
    }
}

/** Grid of Images' repository seam, with the same replay-and-emit shape as the list fake. */
internal class FakeGridOfImagesRepository : GridOfImagesRepository {
    private val stream = MutableSharedFlow<LCE<List<SampleItem>>>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> = stream

    suspend fun emit(value: LCE<List<SampleItem>>) {
        stream.emit(value)
    }
}

/** One repository window request, recorded so paging tests can assert offsets and limits. */
internal data class PageRequest(val offset: Int, val limit: Int)

/**
 * A repository whose every [load] call returns its own controllable flow.
 *
 * Paging is sequential in production, so each call's flow is independent: the test starts a page,
 * waits for the view model to subscribe, then emits that page's result. [requests] records the
 * order of windows so tests can prove one-at-a-time coalescing and end-of-content stops.
 */
internal class ControlledSampleItemRepository : SampleItemRepository {
    private val pages = mutableListOf<MutableSharedFlow<LCE<List<SampleItem>>>>()

    val requests = mutableListOf<PageRequest>()

    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> {
        requests += PageRequest(offset, limit)
        return MutableSharedFlow<LCE<List<SampleItem>>>(replay = 1).also { pages += it }
    }

    suspend fun emitPage(index: Int, value: LCE<List<SampleItem>>) {
        pages[index].emit(value)
    }

    suspend fun emitLatest(value: LCE<List<SampleItem>>) {
        pages.last().emit(value)
    }
}

/** Grid of Images' controllable paging seam, mirroring [ControlledSampleItemRepository]. */
internal class ControlledGridOfImagesRepository : GridOfImagesRepository {
    private val pages = mutableListOf<MutableSharedFlow<LCE<List<SampleItem>>>>()

    val requests = mutableListOf<PageRequest>()

    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> {
        requests += PageRequest(offset, limit)
        return MutableSharedFlow<LCE<List<SampleItem>>>(replay = 1).also { pages += it }
    }

    suspend fun emitPage(index: Int, value: LCE<List<SampleItem>>) {
        pages[index].emit(value)
    }

    suspend fun emitLatest(value: LCE<List<SampleItem>>) {
        pages.last().emit(value)
    }
}

/** Generated-looking rows with stable ids in [range], for paging assertions. */
internal fun pagedItems(range: IntRange): List<SampleItem> = range.map { index ->
    SampleItem(
        id = index.toLong(),
        name = "Name $index",
        caption = "Caption $index",
    )
}
