package net.sigmabeta.sage.sample.feature.componentlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryAction
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryStyle
import net.sigmabeta.sage.ui.componentlibrary.reduce

/** The Component Library's view model: forwards the gallery's own actions to SAGE's pure reducer. */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class ComponentLibraryViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
) : SampleListViewModel<ComponentLibraryState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): ComponentLibraryState = ComponentLibraryState()

    /**
     * The live style tweaks, for the screen's `ComponentLibraryHost`. [uiStateActual] is the
     * rendered `ListStateActual` and does not carry the typed config, so the style is exposed
     * separately.
     */
    val style: StateFlow<ComponentLibraryStyle> by lazy {
        internalUiState
            .map { it.config.style }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialState().config.style)
    }

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is ComponentLibraryAction -> updateState { it.copy(config = it.config.reduce(action)) }
            else -> Unit
        }
    }
}
