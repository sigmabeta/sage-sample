package net.sigmabeta.sage.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.analytics.isInitAction
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.ListStateActual
import net.sigmabeta.sage.list.SageScheduler
import net.sigmabeta.sage.ui.StringProvider

/**
 * Base class for the sample's list screens.
 *
 * SAGE's [net.sigmabeta.sage.list.ListViewModelBrain] is a plain, graph-resolved class — a leftover
 * from the Dagger era, where a `ViewModel` shell wired the brain to a screen. Metro lets each screen
 * resolve its own `ViewModel`, so the brain *is* the ViewModel:
 *
 * - it is contributed into Metro's ViewModel map by the feature module (see
 *   `@ContributesIntoMap` + `@ViewModelKey` on each view model), so `metroViewModel<...>()` can
 *   build it;
 * - `viewModelScope` replaces the app-wide scheduler scope, so work is cancelled when the screen
 *   goes away (Android owns the lifetime);
 * - there is no `BrainProvider`/factory indirection and no graph accessor per screen.
 *
 * The class stays feature-agnostic: everything screen-specific is [initialState] and
 * [handleAction].
 */
abstract class SampleListViewModel<StateType : ListState> :
    ViewModel(),
    ActionSink {
    protected abstract val stringProvider: StringProvider
    protected abstract val analytics: Analytics
    protected abstract val dispatchers: SageDispatchers
    protected abstract val delayManager: DelayManager

    /** SAGE's coroutine seam, backed by [viewModelScope] instead of an app-wide scope. */
    val scheduler: SageScheduler by lazy {
        object : SageScheduler {
            override val dispatchers = this@SampleListViewModel.dispatchers
            override val coroutineScope = viewModelScope
            override val delayManager = this@SampleListViewModel.delayManager
        }
    }

    abstract val screenIdentifier: AnalyticsScreenId

    abstract fun initialState(): StateType

    protected val internalUiState by lazy { MutableStateFlow(initialState()) }

    /** Rendered state: [ListState] mapped through `toActual` on every change. */
    val uiStateActual: StateFlow<ListStateActual> by lazy {
        internalUiState
            .map { it.toActual(stringProvider) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = initialState().toActual(stringProvider),
            )
    }

    override fun sendAction(action: SageAction) {
        if (action.isInitAction()) {
            analytics.logScreenView(action, screenIdentifier)
        } else {
            analytics.logAction(action, screenIdentifier)
        }

        viewModelScope.launch(scheduler.dispatchers.main) {
            handleAction(action)
        }
    }

    protected abstract fun handleAction(action: SageAction)

    protected fun updateState(updater: (StateType) -> StateType) {
        viewModelScope.launch(scheduler.dispatchers.main) {
            internalUiState.value = updater(internalUiState.value)
        }
    }
}
