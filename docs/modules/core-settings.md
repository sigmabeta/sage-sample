# `:core:settings`

The app's persisted preference vocabulary and its `Storage`-backed managers. Shared by both apps so
a setting means the same thing on Android and desktop.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)   // pure JVM
    alias(libs.plugins.sage.di)    // Metro: @Inject constructors and contributed bindings
}
```

## Contents

### Value types

| Type | Values / contract |
|---|---|
| `ThemeMode` | `LIGHT`, `DARK`, `SYSTEM` (default). `isDark(systemDark)` resolves the system case. `fromStorageValue` falls back to the default. |
| `FontChoice` | `STANDARD` (platform family, default), `INTER` (bundled). Compose-free: each app maps it to a `FontFamily`. |
| `SimulateDelay` | `NONE` (default), `ONE_SECOND`, `TWO_SECONDS`, `FIVE_SECONDS`, each with `millis`. |
| `DemoOutcome` | `CONTENT` (default), `EMPTY`, `ERROR`. `select(content, empty, error)` is the shared branch helper; `demoOutcomeFailure()` is the one exception every generated datasource throws for `ERROR`. |

### `SampleSettingsManager`

A `StateFlow`-based interface for `themeMode`, `fontChoice`, `simulateDelay`,
`homeShowsSameContent`, and `demoOutcome`, plus a setter for each. `StateFlow` because the
composition roots read the current value without collecting, while screens still react to changes.

`StorageSampleSettingsManager` (`@SingleIn(AppScope)`, `@ContributesBinding`) implements it over
SAGE's key-value `Storage`, one string key per preference (`setting.appearance.theme`,
`setting.debug.demo_outcome`, …). Missing or unrecognised values fall back to each preference's
default instead of failing a screen.

### Delay seam

`SampleDelayManager : DelayManager` adds `delayMillis()`. `SettingsDelayManager` reads the
persisted `SimulateDelay` **when a load starts**, so a change affects the next load, not one in
flight. `:core:viewmodel`'s `runInBackground` is the only consumer of the duration.

### `SearchHistoryManager`

Observable recent searches (`StateFlow<List<String>>`, newest first) with `record`, `remove`,
`clear`. `StorageSearchHistoryManager` keeps the list in memory, serializes read-modify-write
mutations behind a mutex, and persists one entry per line under `search.history`; it escapes
backslashes/newlines and drops malformed or duplicate entries on read. Capped at 20 entries.

### `DemoDataResetter`

`reset()` clears recent searches and restores `DemoOutcome.DEFAULT` plus
`DEFAULT_HOME_SHOWS_SAME_CONTENT`. `DefaultDemoDataResetter` sequences the app-scoped managers;
running it off the main thread is the caller's job (Settings does this).

## Dependencies

| Dependency | Why |
|---|---|
| `sage.common.storage.common` (`api`) | The key-value `Storage` seam |
| `sage.common.list` (`api`) | `DelayManager`, which `SampleDelayManager` extends |
| `sage.common.di` (`api`) | `AppScope` for the scoped bindings |
| `kotlinx-coroutines-core` | Flows, scopes, mutex |

## Runtime wiring

The concrete managers are `@SingleIn(AppScope::class)` + `@ContributesBinding(AppScope::class)`, so
Metro binds each interface to its implementation without a module listing them. Each app supplies
the `Storage` and `CoroutineScope`: Android uses Preferences DataStore
(`PreferencesStorage`), desktop a properties file (`JvmStorage`).

## Tests

- `SettingsModelsTest` — enum defaults, `fromStorageValue`, `isDark`, `DemoOutcome.select`.
- `StorageSampleSettingsManagerTest` — persistence, defaults, fallback parsing.
- `StorageSearchHistoryManagerTest` — ordering, dedupe, cap, escaping, persistence round-trip.
- `DemoDataResetterTest` — reset sequencing and defaults.
