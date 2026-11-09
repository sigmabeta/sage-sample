# `:core:ui:shell`

The composition root and Compose/Voyager navigation host shared by both apps. It owns everything
that would otherwise be duplicated between `:apps:android` and `:apps:jvm`; an app keeps only its
entry point, Metro graph/bindings, storage and platform seams.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.compose.kmp)
}
```

Sources live in `src/main/java` — the `sage.kmp` convention maps that to its `jvmSharedMain`
intermediate source set, which compiles once per target (Android and JVM) and can see the pure-JVM
core and feature modules. `jvmTest` holds the shared shell tests.

## Public API

| File | Role |
|---|---|
| `SampleApp.kt` | Composition root: selects `SampleTheme(darkTheme, fontFamily)` from the persisted settings, installs the platform seams as composition locals, renders the nav host. |
| `SampleNavHost.kt` | `TabNavigator` shell: tab switching (`popAll` on the outgoing tab), pushed screens, width class, chrome reset, Back application. |
| `SampleScreens.kt` | One Voyager `Screen` per destination, resolving its own view model with `metroViewModel<...>()`; the shared `SampleListScreen` renders title, layout and lifecycle. |
| `SampleTabs.kt` | Home/Browse/Search tabs with retained inner `Navigator`s, active-tab registration, the shared Back local, and `screenFor`. |
| `SampleNavEffects.kt` | `LocalSampleNavEffects`: forwards a feature `SageEvent` into the app. |
| `SampleNavScaffold.kt` / `SampleTopBar.kt` | Bottom bar vs. rail by width class; top bar title/leading affordance from the active screen's SAGE title state. |
| `SampleChrome.kt` | Compose state: `SampleChromeController`, title-bar/width/chrome locals. Policy lives in `:core:nav`. |
| `ScreenLifecycle.kt` | `LocalScreenLifecycleOwner` + `ScreenLifecycleEffect`: exactly one foreground `Resume`/`Pause` pair per visible screen. |
| `SamplePlatformBack.kt` | `SamplePlatformBack` seam + `SampleSystemBack`; the app supplies the platform input. |
| `SampleScreenContainer.kt` | `SampleScreenContainer` seam: wraps each screen with a per-screen ViewModel store where the navigator does not supply one. |
| `GeneratedBitmapPreview.kt` | `@Preview` harness for `:core:bitmaps` placeholder art. |

## Platform seams

`SampleApp` takes four platform-specific values, all with preview-safe defaults:

| Parameter | Android | Desktop |
|---|---|---|
| `interFontFamily` | `res/font` OTF family | classpath `fonts/` OTF family |
| `platformBack` | `AndroidPlatformBack` (`BackHandler`) | `DesktopPlatformBack` (Escape/Backspace + mouse back) |
| `screenContainer` | passthrough (Voyager supplies stores) | `DesktopSampleScreenContainer` (store off a `ScreenModel`) |
| `screenLifecycleOwner` | `null` → activity lifecycle | `rememberWindowFocusLifecycleOwner()` |

`onOuterBack` is the policy's `Outer` branch: Android supplies `finish()`, desktop leaves it empty.

## Dependencies

Re-exported (`api`), because the apps consume these through the shell: `:core:nav`,
`:core:models:api`, `:core:strings:api`, `:core:viewmodel`, `:core:settings`,
`:core:ui:components:api`, `:core:ui:theme:api`, `:core:bitmaps`, and every `:features:*` module
(so both app graphs see the contributed view models). Internal: SAGE `list`, `appcomm`,
`ui.components`, `ui.iconsReal`, `ui.listScreens`, Metro's Compose resolver, Voyager
navigator/tab-navigator/transitions, lifecycle-runtime-compose and coroutines. The `@Preview`
annotation comes from `androidx` on the Android target and JetBrains Compose on the desktop target.

## Tests

`jvmTest` holds the shared `SampleChromeTest` (chrome defaults/reset) and `SampleTabsTest` (stable
tab roots, one tab per top-level destination, every destination mapped to a screen). The pure
policy tests stay in `:core:nav`; desktop-only Back-key tests stay in `:apps:jvm`.
