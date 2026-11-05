package net.sigmabeta.sage.sample.feature.search

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
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Search's view model. Returns the placeholder state until search is built; when it is, query
 * handling belongs here (`SageAction.SearchQueryEntered`) rather than in the app.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class SearchViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
) : SampleListViewModel<SearchState>() {
    override val screenIdentifier = AnalyticsScreen.SEARCH

    override fun initialState(): SearchState = SearchState

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) = Unit
}
