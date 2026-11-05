package net.sigmabeta.sage.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
import net.sigmabeta.sage.appcomm.SageEvent
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
 * - the feature module contributes its factory into Metro's ViewModel map (see
 *   `@ContributesIntoMap` + `@ViewModelKey` on each view model), and `metroViewModel<...>()` uses
 *   it to build an instance;
 * - that instance lives in the Voyager screen's own `ViewModelStore`: it is retained while the
 *   screen stays in the back stack and cleared when the screen is popped or removed, at which
 *   point `viewModelScope` — which replaces the app-wide scheduler scope — is cancelled. The
 *   app-scope contribution never makes the instance app-scoped, and popping + re-pushing builds a
 *   fresh one;
 * - there is no `BrainProvider` shell and no graph accessor per screen.
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

    // --- One-shot UI events ----------------------------------------------------------------
    //
    // Navigation (and other transient effects) is an intent, not screen state: it must fire once
    // and never be replayed when a screen is re-pushed. `replay = 0` is the load-bearing part —
    // a collector only sees events emitted while it is subscribed.

    private val internalUiEvents = MutableSharedFlow<SageEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Transient effects for the screen to render/forward; see [emitEvent]. */
    val uiEvents: SharedFlow<SageEvent> = internalUiEvents

    /** Emits a one-shot effect on the view model's scope, on the main dispatcher. */
    protected fun emitEvent(event: SageEvent) {
        viewModelScope.launch(scheduler.dispatchers.main) {
            internalUiEvents.tryEmit(event)
        }
    }
}
