package net.sigmabeta.sage.sample.feature.welcome

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * The Welcome screen's view model. It owns state, actions and analytics; the app only observes
 * [uiStateActual] and forwards user actions.
 *
 * `@ContributesIntoMap` + `@ViewModelKey` contribute its factory to Metro's ViewModel map, so the
 * screen resolves it with `metroViewModel<WelcomeViewModel>()` without a graph accessor. The
 * instance lives in the Voyager screen's `ViewModelStore`: it is retained while the screen stays
 * in the back stack and cleared when the screen is popped or removed. That UI lifetime is why
 * there is no `@SingleIn` here.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class WelcomeViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: DelayManager,
) : SampleListViewModel<WelcomeState>() {
    override val screenIdentifier = AnalyticsScreen.ABOUT

    override fun initialState(): WelcomeState = WelcomeState

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is WelcomeAction.OpenBrowse -> emitEvent(
                SageEvent.NavigateTo(
                    // Route contract with the app: this must stay in sync with
                    // SampleDestination.BROWSE.destName. The app's SampleDestinationTest guards
                    // the mapping, so changing either side alone fails a test.
                    destination = "browse",
                    source = "WelcomeViewModel",
                ),
            )

            else -> Unit
        }
    }
}
