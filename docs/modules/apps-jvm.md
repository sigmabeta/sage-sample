# `:apps:jvm`

The Compose Desktop application. It reuses the feature modules, the shared renderer suite and SAGE's
common UI, and renders the same `:core:ui:shell` navigation host with desktop-specific platform
seams.

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
| `Main.kt` | Creates `JvmSampleAppGraph`, registers `generatedImageLoader` as Coil's singleton, and opens the window at the size it was closed with. Window-level Escape/Backspace handling emits into a `MutableSharedFlow` and runs the shared Back policy once per physical press (`HeldBackKeys`). |
| `di/JvmSampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`; exposes `settingsManager`, `stringProvider` and `storage`. |
| `di/JvmSampleBindings.kt` | `@BindingContainer @ContributesTo(AppScope)`: the shared `SampleStringProvider`, `BluntHatchet`, `NoopAnalytics`, `AppInfo`, `JvmStorage`, `SageDispatchers` (main = `Dispatchers.Main`, backed by the Swing artifact), and an app-lifetime `CoroutineScope`. |
| `storage/JvmStorage.kt` | `Storage` over a properties file (`~/.sage-sample/settings.properties`, overridable with `-Dsage.sample.settingsFile`). Type-namespaced keys, reactive `MutableStateFlow`s, atomic temp-file + move writes. |
| `ui/PersistedWindowState.kt` | Window size restored from that `Storage` before the platform window is created, and saved back (debounced) while the window is floating. The position is left to the platform. |
| `JvmBuildInfo.kt` | Reads the generated properties into version/build-time constants. |

The shared `MetroViewModelFactory` binding lives in `:core:viewmodel`, so both graphs resolve it
without a per-app copy.

## Desktop rendering differences

- **Per-screen view models.** Voyager's per-screen `ViewModelStoreOwner` plumbing is Android-only. On
  desktop every screen would share the window's store. The wrapper now lives in `:core:ui:shell`
  (`PerScreenViewModelStore.kt`): it hangs a `ViewModelStore` off a Voyager `ScreenModel` (disposed
  when the screen leaves the stack) and provides it as `LocalViewModelStoreOwner`.
  `PerScreenSampleScreenContainer` supplies it to the shared shell, and `:apps:js` reuses the same
  object.
- **Window focus lifecycle.** `ui/WindowFocusLifecycle.kt` builds a `LifecycleOwner` from window
  focus and passes it as `screenLifecycleOwner`, so the foreground `Resume`/`Pause` pair follows
  focus exactly as Android follows the activity.
- **Back keys.** `ui/DesktopPlatformBack.kt`, `ui/navigation/PlatformBackKeys.kt` and
  `ui/navigation/BackMouseButton.kt` feed Escape/Backspace and mouse Back into the same
  `SamplePlatformBack` seam as Android's `BackHandler`.
- **Chrome and layout.** `:core:ui:shell` hosts the Compose/Voyager shell; its policy comes from the
  shared `:core:nav` (`SampleDestination`, `navActionFor`, `backActionFor`, `topBarState`,
  `screenLayoutFor`, route/screen keys); top bar vs. rail and list vs. grid use the same width
  classes.
- **Theme/font.** The shared `SampleApp` uses `:core:ui:theme:api`'s `SampleTheme`; this app passes
  Inter loaded from the classpath (`resources/fonts/`) rather than `res/font`.

## Dependencies

`:core:ui:shell` (which re-exports the features and shared core modules), the in-process data
source `:core:repository:demo` (its Metro bindings supply every `*Repository`), and the desktop
seams: `:core:strings:real`, `:core:settings`, `:core:ui:components:api`, `:core:bitmaps`,
JetBrains Compose runtime/ui + `compose.desktop.currentOs`, `androidx.lifecycle`
viewmodel/viewModelCompose/runtimeCompose, `kotlinx-coroutines-swing` (provides `Dispatchers.Main`
on the AWT event queue), Voyager navigator/screenmodel (for the per-screen store), Coil core, and
JUnit 4.

## Tests

JVM tests beside the module:

- `JvmSampleAppGraphTest` — the graph resolves its entry points and ViewModel factory.
- `InterFontResourcesTest` — bundled font resources are present.
- `HeldBackKeysTest` / `SampleBackKeyTest` — one Back per press, auto-repeat and focus-loss handling.
- `PersistedWindowStateTest` — restoring a stored size, falling back on an unusable one, and the
  placement cases that are not saved.

The shared chrome/tab tests live in `:core:ui:shell`'s `jvmTest`; the shared provider completeness
test and the shell policy tests live in `:core:strings:real` and `:core:nav`.

## Run

```sh
./gradlew :apps:jvm:run
./gradlew :apps:jvm:assemble
```
