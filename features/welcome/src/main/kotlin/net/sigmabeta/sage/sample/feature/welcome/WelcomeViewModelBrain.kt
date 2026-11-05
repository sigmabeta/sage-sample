package net.sigmabeta.sage.sample.feature.welcome

import dev.zacsweers.metro.Inject
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.ListViewModelBrain
import net.sigmabeta.sage.list.SageScheduler
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.ui.StringProvider

/**
 * The feature's screen brain. It owns state, actions, events and analytics; the app only
 * observes [uiStateActual] and forwards user actions.
 *
 * Metro's [Inject] annotation is processed by the `sage.di` convention plugin, so the app
 * graph can request this type directly without a hand-written provider.
 */
@Inject
class WelcomeViewModelBrain(
    stringProvider: StringProvider,
    analytics: Analytics,
    hatchet: Hatchet,
    scheduler: SageScheduler,
) : ListViewModelBrain(
        stringProvider,
        analytics,
        hatchet,
        scheduler,
    ) {
    override val screenIdentifier = AnalyticsScreen.ABOUT

    override fun initialState(): ListState = WelcomeState

    override fun handleAction(action: SageAction) = Unit
}
