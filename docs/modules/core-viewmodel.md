# `:core:viewmodel`

The base class every feature screen's view model extends. It is the sample's replacement for
SAGE's Dagger-era `ListViewModelBrain` / `BrainProvider` shell.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)   // pure JVM, Kotlin 17, detekt, ktlint
    alias(libs.plugins.sage.di)    // contributes the shared MetroViewModelFactory binding
}
```

## Why pure JVM

The base class depends only on `androidx.lifecycle`'s multiplatform `ViewModel` and SAGE's common
modules, so features stay free of Android types and both apps can share them. The module also owns
`SampleMetroViewModelFactory`, the one `MetroViewModelFactory` binding both app graphs resolve, so
neither app keeps its own copy.

## Public API

`SampleListViewModel<StateType : ListState>` (`SampleListViewModel.kt`) is abstract and implements
SAGE's `ActionSink`. `SampleMetroViewModelFactory` (`SampleMetroViewModelFactory.kt`) is the
`@ContributesBinding(AppScope::class)` factory behind `metroViewModel<T>()`; Metro fills its
`viewModelProviders` map from the graph's ViewModel multibindings.

Implementors supply:

| Member | Purpose |
|---|---|
| `stringProvider`, `analytics`, `dispatchers`, `delayManager` | Overridden abstract properties, injected into each feature view model |
| `screenIdentifier` | `AnalyticsScreen` used for screen-view/action logging |
| `initialState()` | The first state |
| `handleAction(action)` | Reduces one `SageAction` |

The base provides:

| Member | Purpose |
|---|---|
| `uiStateActual: StateFlow<ListStateActual>` | `internalUiState` mapped through `ListState.toActual(stringProvider)`; what the app renders |
| `internalUiState` | Mutable `StateFlow<StateType>` for subclasses |
| `updateState { … }` | Applies a state transform on the main dispatcher |
| `scheduler: SageScheduler` | SAGE's coroutine seam, backed by `viewModelScope` (not an app-wide scope) |
| `Flow.runInBackground(dispatcher)` | The single asynchronous seam: optional simulated delay, then `flowOn(disk)`, collected in `viewModelScope` |
| `uiEvents: SharedFlow<SageEvent>` / `emitEvent` | One-shot effects (`replay = 0`), e.g. navigation and snackbars |
| `sendAction(action)` | Coalesces the foreground pair, logs analytics, then dispatches `handleAction` |

### Foreground coalescing

The shared screen sends `SageAction.Resume` on entering composition and `SageAction.Pause` on
leaving it. SAGE's `ListScreen`/`GridScreen` additionally emit `Resume` whenever the title changes.
`sendAction` accepts only a false→true `Resume` and a true→false `Pause`, so feature side effects
run once per real foreground entry, no matter how many title-change resumes arrive.

### One-shot events

`uiEvents` is a `MutableSharedFlow` with `replay = 0`. A screen re-pushed or recomposed cannot replay
an old navigation event; a collector only sees events emitted while it is subscribed.

### Lifecycle

The instance lives in the Voyager screen's `ViewModelStore` (Voyager supplies one per screen on
Android; `:apps:jvm` installs `WithPerScreenViewModelStore` for the desktop). It is retained while
the screen stays in the back stack and cleared when popped, which cancels `viewModelScope` — and
therefore every `runInBackground` collector and the `scheduler`.

## Dependencies

| Dependency | Why |
|---|---|
| `androidx.lifecycle:viewmodel` (`api`) | `ViewModel`, `viewModelScope` |
| `metrox-viewmodel` (`api`) | `MetroViewModelFactory`, the shared factory's supertype |
| `:core:settings` (`api`) | `SampleDelayManager`, exposed as the base's delay seam |
| `sage.common.list` (`api`) | `ListState`, `ListStateActual`, `SageScheduler`, `DelayManager` |
| `sage.common.appcomm` (`api`) | `SageAction`, `SageEvent`, `ActionSink` |
| `sage.common.analytics` (`api`) | `Analytics`, `AnalyticsScreenId`, `isInitAction` |
| `sage.common.coroutines` (`api`) | `SageDispatchers` |
| `sage.common.ui.strings` (`api`) | `StringProvider` |

`api` rather than `implementation` because every feature sees these types through the base class's
public surface.

## Tests

`src/test/.../SampleListViewModelTest.kt` covers state mapping, action dispatch, foreground
coalescing, `runInBackground` delay/collection, and the non-replaying event flow. Test-only deps:
JUnit 4, `kotlinx-coroutines-test`, and `sage.common.ui.components` (its `ListModel` types are needed
by `ListState`; SAGE keeps `ui:components` internal).
