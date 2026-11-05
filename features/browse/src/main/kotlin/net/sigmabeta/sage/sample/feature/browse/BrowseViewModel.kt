package net.sigmabeta.sage.sample.feature.browse

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.onEach
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's view model.
 *
 * The initial load pulls from [BrowseRepository] and publishes each [LCE] through [updateState].
 * Collection runs in `viewModelScope` via [runInBackground], so it stops when the screen's
 * ViewModel is cleared and waits for the duration selected on the Settings screen.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class BrowseViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: BrowseRepository,
) : SampleListViewModel<BrowseState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): BrowseState = BrowseState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs -> load()

            is BrowseAction.OpenListItems -> emitEvent(
                SageEvent.NavigateTo(
                    // Route contract with the app: this must stay in sync with
                    // SampleDestination.LIST_ITEMS.destName. SampleDestinationTest guards the
                    // mapping, so changing either side alone fails a test.
                    destination = "list-items",
                    source = "BrowseViewModel",
                ),
            )

            else -> Unit
        }
    }

    private fun load() {
        updateState { BrowseState(LCE.Loading(BROWSE_OPERATION_NAME)) }

        repository.load()
            .onEach { content ->
                updateState { BrowseState(content) }
            }
            .runInBackground()
    }
}
