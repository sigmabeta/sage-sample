# `:apps:jvm`

The Compose Desktop application. It reuses the feature modules, the shared renderer suite and SAGE's
common UI, and mirrors `:apps:android`'s navigation shell with desktop-specific platform seams.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)          // Kotlin/JVM 17, detekt, ktlint
    alias(libs.plugins.sage.di)           // Metro
    alias(libs.plugins.compose.compiler)  // Compose codegen
    alias(libs.plugins.compose.multiplatform) // desktop application DSL
}
```

`compose.desktop.application` sets `mainClass = net.sigmabeta.sage.sample.jvm.MainKt`, package name
`sage-sample`, version from the root project, and targets deb/rpm/msi/dmg with
`includeAllModules = true` (Compose Desktop uses AWT/Swing, and module inference can miss
`java.desktop`).

## Build-time resource

`generateBuildInfo` rewrites `build/generated/build-info/sample-build-info.properties` on every build
(`outputs.upToDateWhen { false }`) with `versionName` and `buildTimeMs`. `processResources` depends on
it, and `JvmBuildInfo` reads it into `AppInfo` for Settings' About rows. This keeps the timestamp the
build's, not a stale cached one.

## Entry point and seams

| File | Role |
|---|---|
| `Main.kt` | Creates `JvmSampleAppGraph`, registers `generatedImageLoader` as Coil's singleton, opens a 1000×800 window. Window-level Escape/Backspace handling emits into a `MutableSharedFlow` and runs the shared Back policy once per physical press (`HeldBackKeys`). |
| `di/JvmSampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`; exposes `settingsManager` and `stringProvider`. |
| `di/JvmSampleBindings.kt` | `@BindingContainer @ContributesTo(AppScope)`: `SampleStringProvider`, `BluntHatchet`, `NoopAnalytics`, `AppInfo`, `JvmStorage`, `SageDispatchers` (main = `Dispatchers.Main`, backed by the Swing artifact), and an app-lifetime `CoroutineScope`. |
| `di/JvmMetroViewModelFactory.kt` | Desktop `MetroViewModelFactory`, installed as `LocalMetroViewModelFactory`. |
| `SampleStrings.kt` / `SampleStringProvider.kt` | Desktop text table mirroring Android's `strings.xml`; maps every feature `SageStringId` and `ComponentStringId`. Unmapped IDs fail loudly. |
| `storage/JvmStorage.kt` | `Storage` over a properties file (`~/.sage-sample/settings.properties`, overridable with `-Dsage.sample.settingsFile`). Type-namespaced keys, reactive `MutableStateFlow`s, atomic temp-file + move writes. |
| `JvmBuildInfo.kt` | Reads the generated properties into version/build-time constants. |

## Desktop rendering differences

- **Per-screen view models.** Voyager's per-screen `ViewModelStoreOwner` plumbing is Android-only. On
  desktop every screen would share the window's store, so `ui/PerScreenViewModelStore.kt` hangs a
  `ViewModelStore` off a Voyager `ScreenModel` (disposed when the screen leaves the stack) and
  provides it as `LocalViewModelStoreOwner`. `metroViewModel<...>()` then behaves as on Android:
  retained while in the back stack, cleared on pop.
- **Window focus lifecycle.** `ScreenLifecycle.kt` uses `LocalLifecycleOwner`/`LifecycleResumeEffect`
  so the foreground `Resume`/`Pause` pair follows window focus.
- **Back keys.** `PlatformBackKeys.kt` / `BackMouseButton.kt` feed Escape/Backspace and mouse Back
  into the same `SampleSystemBack` seam as Android's `BackHandler`.
- **Chrome and layout.** `ui/navigation/` mirrors the Android shell (`SampleDestination`,
  `SampleTabs`, `SampleNavHost`, `SampleNavAction`, `SampleChrome`, …); top bar vs. rail and
  list vs. grid derive from the same width classes.
- **Theme/font.** `ui/SampleApp.kt` applies `ThemeMode` and `FontChoice` exactly as Android does, but
  loads Inter from the classpath (`resources/fonts/`) rather than `res/font`.

## Dependencies

Same feature/core set as Android. Desktop-specific: JetBrains Compose runtime/foundation/material3/ui
plus `compose.desktop.currentOs`, `androidx.lifecycle` viewmodel/viewModelCompose/runtimeCompose,
`kotlinx-coroutines-swing` (provides `Dispatchers.Main` on the AWT event queue), Voyager
navigator/screenmodel/tab-navigator/transitions, Coil 3, JUnit 4.

## Tests

Pure JVM tests beside the module:

- `JvmSampleAppGraphTest` — the graph resolves its entry points and ViewModel factory.
- `SampleStringProviderTest` — every feature/component ID has desktop text.
- `InterFontResourcesTest` — bundled font resources are present.
- Navigation twins of the Android tests: destination, nav action, back action, chrome, top bar,
  layout, tabs, content-detail screen key, screen lifecycle.
- `HeldBackKeysTest` / `SampleBackKeyTest` — one Back per press, auto-repeat and focus-loss handling.

## Run

```sh
./gradlew :apps:jvm:run
./gradlew :apps:jvm:assemble
```
