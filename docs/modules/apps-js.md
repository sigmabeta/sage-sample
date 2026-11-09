# `:apps:js`

The Kotlin/JS browser application — the web twin of `:apps:android` and `:apps:jvm`. It mounts the
same `SampleApp` shell in a `ComposeViewport`, persists settings in `window.localStorage`, and gets
its data from `:apps:server` through `:core:repository:remote`.

The module is gated behind `-Psage.js=true` (see `settings.gradle.kts`), together with the JS
variants of every shared `sage.kmp` module.

## Plugins and target

```kotlin
plugins {
    kotlin("multiplatform")              // single JS target
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}
```

`sage.kmp` / `sage.compose.kmp` are deliberately not applied: they add JVM + Android targets a
single-target executable does not want. `js(IR) { browser() }` + `binaries.executable()` produce
`sage-sample.js`; webpack bundles it into `build/dist/js/<mode>Executable/`.

The Kotlin/JS toolchain uses the system Node 22 + Yarn 1.x (the root build disables the plugin's
download so it cannot add project repositories under `FAIL_ON_PROJECT_REPOS`).

## Entry point and wiring

| File | Role |
|---|---|
| `JsMain.kt` | Builds `WebSampleAppGraph`, wires Escape→Back and arrow-key focus, removes the splash, mounts `SampleApp` in `ComposeViewport`. It provides `LocalForceFakeImages = true`, so generated art renders directly instead of through Coil. |
| `di/WebSampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`; exposes `settingsManager`, `stringProvider` and `browserNavigation`. |
| `di/WebSampleBindings.kt` | `@BindingContainer @ContributesTo(AppScope)`: shared strings, `BasicHatchet`, `NoopAnalytics`, `AppInfo` from generated `WebBuildInfo`, `LocalStorageStorage`, JS dispatchers, app scope, Ktor `HttpClient` + `SampleApiClient`, `BrowserHistoryNavigation`. |
| `storage/LocalStorageStorage.kt` | `Storage` over `window.localStorage` with per-key `StateFlow`s and cross-tab `storage`-event sync. |
| `navigation/BrowserHistoryNavigation.kt` | `SampleBrowserNavigation`: `pushState`/`replaceState`, `popstate` → `SampleShellNavigation.navigateTo`, Back delegation when an app entry exists behind the current URL. |
| `ui/WebPlatformBack.kt` | Escape (outside text fields) → the shared Back policy. |
| `src/jsMain/resources/index.html` | SPA shell; `<base href="/">` keeps assets at the site root for deep links. |

`generateWebBuildInfo` writes `WebBuildInfo.kt` from `git describe` / branch / commit time into
`build/generated/webBuildInfo` for the Settings About rows.

## Browser URLs

`:core:nav` owns the pure `SampleRoute`/`SampleRouteUrl` codec. The bridge only writes history and
applies `popstate`; route decoding, tab ownership and canonicalisation are unit-tested on the JVM.
Short links such as `/settings` land on Home and are canonicalised to `/home/settings`.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:ui:shell` (`api` graph) | Same composition root, screens and policy |
| `:core:repository:remote` | HTTP implementations of every repository seam |
| `:core:repository:api` | Route constants, wire DTOs, search seed contract |
| `:core:bitmaps`, `:core:settings`, `:core:strings:*`, `:core:ui:components:api` | Shared art, preferences, text, renderers |
| Metro (`metrox-viewmodel`, `-compose`) + `androidx.lifecycle.viewmodel` | ViewModel map + per-screen stores |
| Voyager navigator/screenmodel | Same navigation stack as desktop |
| Ktor client core/js/content-negotiation + serialization | JSON API |
| JetBrains Compose runtime/foundation/material3/ui | Compose for Web |

It never depends on `:core:repository:demo`: that module is JVM-only because SAGE's
`StringGenerator` lives in a JVM source set.

## Tests

The module has no JS test target. The pure pieces it depends on are covered on the JVM:
`:core:nav` (URL codec), `:core:repository:remote` (Ktor MockEngine), and `:apps:server`
(the route stack).
