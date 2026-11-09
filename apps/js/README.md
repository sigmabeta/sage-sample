# `:apps:js`

The Kotlin/JS browser client. It mounts the shared `SampleApp` shell in a `ComposeViewport`, binds
the HTTP repositories from `:core:repository:remote`, and persists settings in `localStorage`.

```sh
./gradlew :apps:js:jsBrowserDevelopmentExecutableDistribution -Psage.js=true
```

Requires Node 22 + Yarn 1.x on `PATH`; the root build disables Kotlin's own Node/Yarn download
because `FAIL_ON_PROJECT_REPOS` rejects its project repositories. The module only exists when
`-Psage.js=true` is set.

## Layout

| File | Role |
|---|---|
| `src/jsMain/kotlin/…/JsMain.kt` | Web graph, Escape/arrow-key handling, splash removal, `SampleApp` mount. |
| `…/di/WebSampleAppGraph.kt` | `@DependencyGraph(AppScope::class) : ViewModelGraph`. |
| `…/di/WebSampleBindings.kt` | Strings, logger, analytics, `AppInfo`, localStorage storage, dispatchers, Ktor client, browser navigation. |
| `…/storage/LocalStorageStorage.kt` | SAGE `Storage` over `window.localStorage` (cross-tab live). |
| `…/navigation/BrowserHistoryNavigation.kt` | `pushState`/`popstate` ↔ `SampleRoute`; Back delegates to `history.back()` when an app entry exists. |
| `…/ui/WebPlatformBack.kt` | Escape (outside text fields) → the shared Back policy. |
| `src/jsMain/resources/index.html` | SPA shell with `<base href="/">` and the launch splash. |
| `webpack.config.d/okio-node-polyfills.js` | `os`/`path`/`fs` fallbacks okio's JS bundle needs. |

`generateWebBuildInfo` writes `WebBuildInfo.kt` (version/branch/commit/time) for Settings > About.

## Tests

No JS test target. The URL codec is tested in `:core:nav`, the HTTP client in
`:core:repository:remote` (Ktor MockEngine), and the server stack in `:apps:server`.
