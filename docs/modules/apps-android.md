# `:apps:android`

The Android application: entry point, Metro app graph, Android platform seams, and the Voyager
navigation shell that renders every feature.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.metro)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)   // applied directly: SAGE conventions target libraries
}
```

`namespace`/`applicationId` are `net.sigmabeta.sage.sample`; `compileSdk`/`targetSdk` 36, `minSdk`
26, JVM 17. Version comes from the root project (`0.1.0`). `buildConfig` is enabled because Settings'
About rows read `VERSION_NAME`, `VERSION_CODE` and `BUILD_TIME_MS` (captured at BuildConfig
generation). Both build types are unminified; release is unsigned.

## Entry points

| File | Role |
|---|---|
| `SampleApplication.kt` | Creates the app-scoped `SampleAppGraph` lazily; registers `generatedImageLoader` as Coil's singleton; enables SAGE perf measurement in debug. |
| `MainActivity.kt` | Installs `LocalMetroViewModelFactory` and `LocalComponentStringProvider` above `SampleApp`, and supplies `finish()` as the outer Back action. |
| `di/SampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`. Exposes `settingsManager` and `stringProvider`; provides the application `Context`. |
| `di/SampleBindings.kt` | `@BindingContainer @ContributesTo(AppScope)` — string provider (the only `R.string` map), `Hatchet`, `Analytics`, `Storage` (DataStore), `AppInfo`. |
| `di/SampleMetroViewModelFactory.kt` | `MetroViewModelFactory` backed by the graph's ViewModel map; installed as `LocalMetroViewModelFactory`. |
| `storage/PreferencesStorage.kt` | Android `Storage` over Preferences DataStore (`sage_sample_settings`); disk-dispatched writes, flow reads. |

## UI shell

`ui/SampleApp.kt` is the composition root: it collects `themeMode` and `fontChoice`, wraps Material 3
in the selected color scheme and `sampleTypography(fontFamily)`, then renders `SampleNavHost`.
Inter ships as three OTF weights in `res/font`.

`ui/navigation/` holds the app's whole navigation policy, split into pure functions and Compose
glue:

| File | Role |
|---|---|
| `SampleDestination.kt` | Destinations as SAGE `RouteDescriptor`s; top-level flag, label resource, icon. `fromRoute` resolves feature routes, including every `content-detail/...` route. |
| `SampleScreens.kt` | One Voyager `Screen` per destination, each resolving its own view model with `metroViewModel<...>()` and reporting title/layout/lifecycle through the shared renderer. |
| `SampleTabs.kt` | Home/Browse/Search tabs, each owning a retained inner `Navigator`; active-tab registration; shared Back handler; `screenFor`. |
| `SampleNavHost.kt` | `TabNavigator` shell: tab switching (`popAll` on the outgoing tab), pushed screens, width class, chrome reset, Back application. |
| `SampleNavEffects.kt` | `LocalSampleNavEffects`: forwards a feature `SageEvent` into the app. |
| `SampleNavAction.kt` | Pure `navActionFor`: `ShowTopLevel`, `Push` (with optional `ContentId`), `Pop`. Unknown routes/events are ignored. |
| `SampleBackAction.kt` | Pure `backActionFor(tab, canPop)`: `Pop`, `ShowTab(Home)`, or `Outer`. |
| `SampleSystemBack.kt` | Android `BackHandler` → the shared Back handler. |
| `SampleNavScaffold.kt` / `SampleTopBar.kt` | Bottom bar vs. rail by width class; top bar title and leading affordance from the active screen's SAGE title state. |
| `SampleChrome.kt` | Pure chrome/layout derivation (`topBarState`, `screenLayoutFor`, `widthClassFromDp`, route keys). Search uses it to hide the top bar. |
| `ScreenLifecycle.kt` | Sends exactly one foreground `Resume`/`Pause` pair for the visible, foregrounded screen. |

Navigation facts worth knowing:

- Popping the tab root pops the shell (Android finishes the activity).
- Search hides the top bar; `SampleNavHost` resets chrome on every route change, so Search does not
  restore it on disposal.
- `ContentDetailScreen` keys its Voyager screen by the full `ContentId`, so each ID gets its own view
  model and load.
- Layout is derived: `ColumnType.One` or at most one computed column renders `ListScreen`; more
  columns renders `GridScreen` (staggered when the column type asks).

## Dependencies

All six feature modules plus `:core:viewmodel`, `:core:settings`, `:core:ui:components:api` and
`:core:bitmaps`. Notable platform/SAGE extras: `metrox-viewmodel(-compose)`, SAGE nav/icons/list/
strings/storage/logging/coroutines/perf, AndroidX DataStore Preferences, Compose BOM + material3 +
foundation + activity-compose + lifecycle-runtime-compose, Coil 3, Voyager navigator/tab-navigator/
transitions, `kotlinx-collections-immutable`, JUnit 4.

## Tests

Android unit tests for app-owned logic only (no emulator):

- `SampleDestinationTest` — route mapping, top-level set, Content-detail prefix.
- `SampleNavActionTest` — every reachable route and handled event type.
- `SampleBackActionTest` — each tab/`canPop` branch.
- `SampleChromeTest` / `SampleTopBarStateTest` / `SampleScreenLayoutTest` — derivation tables.
- `SampleTabsTest`, `SampleContentDetailScreenTest`, `ScreenLifecycleTest` — screen keys, active-tab
  registration, foreground pair.

## Verify

```sh
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintRelease :apps:android:assembleRelease
```

Do not boot an AVD; hand device testing to the user. The release APK needs signing before install.
