package net.sigmabeta.sage.sample.feature.browse

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's view model.
 *
 * Initial load and the demo rows all go through [handleAction], which pulls from
 * [BrowseRepository] and publishes each [LCE] through [updateState]. Collection runs in
 * `viewModelScope` via [runInBackground], so it stops when the screen's ViewModel is cleared.
 * Starting a new load cancels the previous collection, so a slow earlier outcome can never
 * overwrite the outcome the user asked for last.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class BrowseViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: DelayManager,
    private val repository: BrowseRepository,
) : SampleListViewModel<BrowseState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): BrowseState = BrowseState()

    /** The in-flight load; a new load cancels it so only the latest outcome can publish. */
    private var loadJob: Job? = null

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs, is BrowseAction.LoadContent -> load(BrowseOutcome.CONTENT)
            is BrowseAction.LoadEmpty -> load(BrowseOutcome.EMPTY)
            is BrowseAction.LoadError -> load(BrowseOutcome.ERROR)
            else -> Unit
        }
    }

    private fun load(outcome: BrowseOutcome) {
        loadJob?.cancel()
        updateState { BrowseState(LCE.Loading(BROWSE_OPERATION_NAME)) }

        loadJob = repository.load(outcome)
            .onEach { content ->
                updateState { BrowseState(content) }
            }
            .runInBackground()
    }
}
