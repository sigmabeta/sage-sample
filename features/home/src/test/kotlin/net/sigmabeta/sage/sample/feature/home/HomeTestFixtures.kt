package net.sigmabeta.sage.sample.feature.home

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
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
        "${getString(string)}:$arg"

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        "${getString(string)}:$first:$second"
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

internal fun testDispatchers(dispatcher: CoroutineDispatcher) = SageDispatchers(
    computation = dispatcher,
    disk = dispatcher,
    network = dispatcher,
    main = dispatcher,
)

/**
 * Home datasource seam used by module and view model tests.
 *
 * [gridResponse] defaults to the first [limit] items; pass a custom response to exercise loading
 * and error shapes, or [gridFailure] / [countFailure] to make the flow throw.
 */
internal class FakeHomeRepository(
    private val items: List<HomeItem> = emptyList(),
    private val gridResponse: (List<HomeItem>, Int) -> LCE<List<HomeItem>> = { all, limit ->
        LCE.Content(all.take(limit))
    },
    private val gridFailure: Throwable? = null,
    private val countFailure: Throwable? = null,
) : HomeRepository {
    /** Every seed passed to [itemCount] or [itemAt], in call order. */
    val itemSeeds = mutableListOf<String>()

    /** Every seed passed to [gridImages], in call order. */
    val gridSeeds = mutableListOf<String>()

    override fun itemCount(seed: String): Int {
        itemSeeds += seed
        countFailure?.let { throw it }
        return items.size
    }

    override fun itemAt(seed: String, index: Int): HomeItem? {
        itemSeeds += seed
        return items.getOrNull(index)
    }

    override fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>> = flow {
        gridSeeds += seed
        gridFailure?.let { throw it }
        emit(gridResponse(items, limit))
    }
}

internal class FakeHomeModule(
    override val id: String,
    override val priority: Int,
    private val flow: Flow<LCE<HomeModuleSection>>,
    override val showHeader: Boolean = true,
) : HomeModule {
    override fun state(): Flow<LCE<HomeModuleSection>> = flow
}

/** A registered row whose created instances reuse one test flow. */
internal class FakeHomeModuleDefinition(
    override val typeId: String,
    override val priority: Int,
    private val moduleFor: (Int, Long) -> HomeModule,
) : HomeModuleDefinition {
    override fun create(instanceNumber: Int, dataIdBase: Long): HomeModule =
        moduleFor(instanceNumber, dataIdBase)
}

/** A definition that reports each instance it creates, keyed by plan position. */
internal fun fakeDefinition(
    typeId: String,
    priority: Int,
    flow: Flow<LCE<HomeModuleSection>>,
): FakeHomeModuleDefinition = FakeHomeModuleDefinition(typeId, priority) { instanceNumber, _ ->
    FakeHomeModule(
        id = "${typeId}_$instanceNumber",
        priority = instanceNumber - 1,
        flow = flow,
    )
}

/** In-memory settings; only the Home-relevant preferences change in Home tests. */
internal class FakeHomeSettingsManager(
    initialHomeShowsSameContent: Boolean = true,
) : SampleSettingsManager {
    private val homeSameContent = MutableStateFlow(initialHomeShowsSameContent)
    private val outcome = MutableStateFlow(DemoOutcome.DEFAULT)

    override val themeMode: StateFlow<ThemeMode> = MutableStateFlow(ThemeMode.DEFAULT).asStateFlow()

    override val fontChoice: StateFlow<FontChoice> =
        MutableStateFlow(FontChoice.DEFAULT).asStateFlow()

    override val simulateDelay: StateFlow<SimulateDelay> =
        MutableStateFlow(SimulateDelay.DEFAULT).asStateFlow()

    override val demoOutcome: StateFlow<DemoOutcome> = outcome.asStateFlow()

    override val homeShowsSameContent: StateFlow<Boolean> = homeSameContent.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) = Unit

    override fun setFontChoice(choice: FontChoice) = Unit

    override fun setSimulateDelay(delay: SimulateDelay) = Unit

    override fun setDemoOutcome(outcome: DemoOutcome) {
        this.outcome.value = outcome
    }

    override fun setHomeShowsSameContent(showSameContent: Boolean) {
        homeSameContent.value = showSameContent
    }
}
