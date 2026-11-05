package net.sigmabeta.sage.sample.feature.settings

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
 * Settings' view model. The placeholder state has no actions yet; real preference toggles belong
 * here, not in the app module.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class SettingsViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: DelayManager,
) : SampleListViewModel<SettingsState>() {
    override val screenIdentifier = AnalyticsScreen.SETTINGS

    override fun initialState(): SettingsState = SettingsState

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) = Unit
}
