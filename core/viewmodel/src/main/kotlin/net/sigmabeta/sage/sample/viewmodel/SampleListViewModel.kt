package net.sigmabeta.sage.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.analytics.isInitAction
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.ListStateActual
import net.sigmabeta.sage.list.SageScheduler
import net.sigmabeta.sage.sample.settings.SampleDelayManager
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
 *
 * ## Foreground resume / pause
 *
 * There is exactly one foreground pair: the app's shared screen sends [SageAction.Resume] when it
 * enters composition and [SageAction.Pause] when it leaves. SAGE's own `ListScreen` / `GridScreen`
 * additionally emit a `Resume` whenever the rendered title changes, which is not a second
 * foreground entry.
 *
 * [sendAction] coalesces the pair: only a false→true `Resume` and a true→false `Pause` reach
 * [handleAction] (and analytics), so a feature's resume/pause side effects run once per foreground
 * entry no matter how many title changes SAGE reports.
 */
abstract class SampleListViewModel<StateType : ListState> :
    ViewModel(),
    ActionSink {
    protected abstract val stringProvider: StringProvider
    protected abstract val analytics: Analytics
    protected abstract val dispatchers: SageDispatchers
    protected abstract val delayManager: SampleDelayManager

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

    /** Whether the screen is currently between an accepted `Resume` and `Pause`. */
    private var resumed = false

    override fun sendAction(action: SageAction) {
        // Coalesce the foreground pair before analytics or handling: SAGE's list/grid screens also
        // send `Resume` on every title change, and those are not new foreground entries.
        if (!acceptsForegroundTransition(action)) {
            return
        }

        if (action.isInitAction()) {
            analytics.logScreenView(action, screenIdentifier)
        } else {
            analytics.logAction(action, screenIdentifier)
        }

        viewModelScope.launch(scheduler.dispatchers.main) {
            handleAction(action)
        }
    }

    /**
     * Tracks the foreground state and reports whether [action] is a real transition.
     *
     * A repeated `Resume` (or `Pause`) returns `false`; every other action is always accepted.
     */
    private fun acceptsForegroundTransition(action: SageAction): Boolean {
        val target = when (action) {
            SageAction.Resume -> true
            SageAction.Pause -> false
            else -> return true
        }

        if (resumed == target) {
            return false
        }

        resumed = target
        return true
    }

    protected abstract fun handleAction(action: SageAction)

    protected fun updateState(updater: (StateType) -> StateType) {
        viewModelScope.launch(scheduler.dispatchers.main) {
            internalUiState.value = updater(internalUiState.value)
        }
    }

    // --- Background work --------------------------------------------------------------------
    //
    // Collect a feature flow off the main thread, inside the view model's own scope, so it is
    // cancelled when the screen's ViewModel is cleared.

    /**
     * Collects this flow on [dispatcher] within [SageScheduler.coroutineScope] (the
     * `viewModelScope`), after the simulated delay selected on the Settings screen.
     *
     * This is the sample's single asynchronous seam: every feature background operation goes
     * through it, so the selected duration applies uniformly and `None` is the only way to skip
     * the wait.
     */
    protected fun <EmissionType> Flow<EmissionType>.runInBackground(
        dispatcher: CoroutineDispatcher = scheduler.dispatchers.disk,
    ): Job {
        val delayMillis = this@SampleListViewModel.delayManager.delayMillis()
        val possiblyDelayedFlow = if (delayMillis > 0L) this.onStart { delay(delayMillis) } else this
        return possiblyDelayedFlow
            .flowOn(dispatcher)
            .launchIn(scheduler.coroutineScope)
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

    /**
     * Emits a one-shot effect.
     *
     * `MutableSharedFlow.tryEmit` is thread-safe and never suspends, and callers already run on
     * the main dispatcher, so no extra dispatch would add anything.
     */
    protected fun emitEvent(event: SageEvent) {
        internalUiEvents.tryEmit(event)
    }
}
