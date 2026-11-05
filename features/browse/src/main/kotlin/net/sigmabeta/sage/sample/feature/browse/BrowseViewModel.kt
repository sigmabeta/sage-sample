package net.sigmabeta.sage.sample.feature.browse

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's view model. Nothing to act on yet: the screen is a placeholder, but it already goes
 * through the same state/analytics path every real SAGE feature will.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class BrowseViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: DelayManager,
) : SampleListViewModel<BrowseState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): BrowseState = BrowseState

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) = Unit
}
