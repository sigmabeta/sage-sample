# `:core:ui:shell`

The composition root and Compose/Voyager navigation host shared by every app. It owns everything
that would otherwise be duplicated between `:apps:android` and `:apps:jvm`; an app keeps only its
entry point, Metro graph/bindings, storage and platform seams.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.sage.compose.kmp)
}
```

Sources live in `src/commonMain/kotlin` so every target compiles them; the desktop-only
`GeneratedBitmapPreview` is in `src/jvmMain` because the AndroidX `@Preview` artifact is not a JS
dependency. `jvmTest` holds the shared shell tests.

## Public API

| File | Role |
|---|---|
| `SampleApp.kt` | Composition root: selects `SampleTheme(darkTheme, fontFamily)` from the persisted settings, installs the platform seams as composition locals, renders the nav host. |
| `SampleNavHost.kt` | `TabNavigator` shell: tab switching (`popAll` on the outgoing tab), pushed screens, width class, chrome reset, Back application. |
| `SampleScreens.kt` | One Voyager `Screen` per destination, resolving its own view model with `metroViewModel<...>()`; the shared `SampleListScreen` renders title, layout and lifecycle. |
| `SampleTabs.kt` | Home/Browse/Search tabs with retained inner `Navigator`s, the slide/fade between them (`SampleCurrentTab`), active-tab registration, the shared Back local, and `screenFor`. |
| `SampleNavEffects.kt` | `LocalSampleNavEffects`: forwards a feature `SageEvent` into the app. |
| `SampleNavScaffold.kt` / `SampleTopBar.kt` | Bottom bar vs. rail by width class; top bar title/leading affordance from the active screen's SAGE title state. |
| `SampleChrome.kt` | Compose state: `SampleChromeController`, title-bar/width/chrome locals. Policy lives in `:core:nav`. |
| `ScreenLifecycle.kt` | `LocalScreenLifecycleOwner` + `ScreenLifecycleEffect`: exactly one foreground `Resume`/`Pause` pair per visible screen. |
| `SamplePlatformBack.kt` | `SamplePlatformBack` seam + `SampleSystemBack`; the app supplies the platform input. |
| `SampleScreenContainer.kt` | `SampleScreenContainer` seam: wraps each screen with a per-screen ViewModel store where the navigator does not supply one. |
| `PerScreenViewModelStore.kt` | `PerScreenSampleScreenContainer`: the desktop/web store (a Voyager `ScreenModel` owns the `ViewModelStore`). |
| `SampleBrowserNavigation.kt` | Browser-history seam: the shell reports `SampleRoute`s and accepts deep links/`popstate` through `SampleShellNavigation`. Null on Android/desktop. |
| `jvmMain/GeneratedBitmapPreview.kt` | Desktop `@Preview` harness for `:core:bitmaps` placeholder art. |

## Platform seams

`SampleApp` takes platform-specific values, all with preview-safe defaults:

| Parameter | Android | Desktop / Web |
|---|---|---|
| `interFontFamily` | `res/font` OTF family | classpath `fonts/` OTF family (desktop); default family (web) |
| `platformBack` | `AndroidPlatformBack` (`BackHandler`) | `DesktopPlatformBack` (Escape/Backspace + mouse back) / `WebPlatformBack` (Escape) |
| `screenContainer` | passthrough (Voyager supplies stores) | `PerScreenSampleScreenContainer` (store off a `ScreenModel`; desktop + web) |
| `screenLifecycleOwner` | `null` → activity lifecycle | `rememberWindowFocusLifecycleOwner()` (desktop); composition owner on web |
| `browserNavigation` | `null` | `null` (desktop) / `BrowserHistoryNavigation` (web) |

`onOuterBack` is the policy's `Outer` branch: Android supplies `finish()`, desktop leaves it empty.

## Dependencies

Re-exported (`api`), because the apps consume these through the shell: `:core:nav`,
`:core:models:api`, `:core:strings:api`, `:core:viewmodel`, `:core:settings`,
`:core:ui:components:api`, `:core:ui:theme:api`, `:core:bitmaps`, and every `:features:*` module
(so every app graph sees the contributed view models). Internal: SAGE `list`, `appcomm`,
`ui.components`, `ui.iconsReal`, `ui.listScreens`, Metro's Compose resolver, Voyager
navigator/screenmodel/tab-navigator/transitions, lifecycle-runtime-compose and coroutines. The
desktop `jvmMain` source set adds `coil-compose` and the JetBrains `ui-tooling-preview` for the
preview harness; the Android target uses the AndroidX BOM + preview artifact.

## Tests

`jvmTest` holds the shared `SampleChromeTest` (chrome defaults/reset) and `SampleTabsTest` (stable
tab roots, one tab per top-level destination, every destination mapped to a screen). The pure
policy tests stay in `:core:nav`; desktop-only Back-key tests stay in `:apps:jvm`.
