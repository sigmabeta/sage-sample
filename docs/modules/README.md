# Module reference

One page per Gradle module in this repository. Each page covers the module's job, how it is built,
what it depends on and why, the key types it exposes, and what its tests pin down. For the
end-to-end architecture (navigation, screens, DI, layouts) see
[`README.md`](../../README.md); for review-time rules see
[`CODING_STANDARDS.md`](../../CODING_STANDARDS.md).

The build is a **Gradle composite build**: `settings.gradle.kts` includes `sage/`,
`sage/sage-build-logic`, and the sample's own `build-logic/` (the screenshot convention), and reuses
SAGE's own version catalog (`libs`). There are no `dependencySubstitution` rules — SAGE's published
coordinates resolve to the included projects automatically.

## Module map

| Module | Kind | One-line role |
|---|---|---|
| [`:apps:android`](apps-android.md) | Android application | Activity, Metro app graph, Android platform seams, navigation shell |
| [`:apps:jvm`](apps-jvm.md) | Compose Desktop application | Window entry point, desktop graph, JVM platform seams, same navigation shell |
| [`:apps:js`](apps-js.md) | Kotlin/JS application | Browser entry point, web graph, localStorage/HTTP seams, same navigation shell |
| [`:apps:server`](apps-server.md) | JVM application | Ktor JSON API over `DemoContent`; serves the browser bundle with an SPA fallback |
| [`:core:bitmaps`](core-bitmaps.md) | KMP Compose library | Deterministic placeholder art: generator, `@Preview` composable, `generated://` Coil fetcher |
| [`:core:models:api`](core-models-api.md) | KMP library | Shared domain models + the Content detail route codec |
| [`:core:nav`](core-nav.md) | KMP library | Shared destinations, route/Back policy, layout and top-bar derivation, URL codec, screen keys |
| [`:core:repository:api`](core-repository-api.md) | KMP library | Repository seams, JSON wire DTOs + mappings, API route/parameter constants |
| [`:core:repository:demo`](core-repository-demo.md) | JVM library | Deterministic in-process sources and generators; bound by Android/desktop and used by the server |
| [`:core:repository:remote`](core-repository-remote.md) | KMP library | Ktor-client implementations of the seams; bound by the browser |
| [`:core:settings`](core-settings.md) | KMP library | Persisted preferences, demo outcome, simulated delay, recent-search history |
| [`:core:strings:api`](core-strings-api.md) | KMP library | One `SampleStringId` vocabulary for every user-visible string |
| [`:core:strings:real`](core-strings-real.md) | KMP library | The single id → English table (`SampleStringProvider`) |
| [`:core:ui:components:api`](core-ui-components-api.md) | KMP Compose library | Shared renderer suite: SAGE `ListModel` → composables, search chrome |
| [`:core:ui:previews`](core-ui-previews.md) | Android test-support library | VM-free `ListState` rendering, device matrix and `FakeModelGenerator` |
| [`:core:ui:shell`](core-ui-shell.md) | KMP Compose library | Shared composition root and Voyager navigation host |
| [`:core:ui:theme:api`](core-ui-theme-api.md) | KMP Compose library | `SampleTheme` + the shared type scale |
| [`:core:viewmodel`](core-viewmodel.md) | KMP library | `SampleListViewModel`: the base every feature screen builds on |
| [`:features:browse`](features-browse.md) | KMP feature | Static destination list that opens the demo screens |
| [`:features:browse:screenshot`](features-browse-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Browse |
| [`:features:contentdetail`](features-contentdetail.md) | KMP feature | Parameterized detail screen over seeded generated content |
| [`:features:contentdetail:screenshot`](features-contentdetail-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Content detail |
| [`:features:home`](features-home.md) | KMP feature | Module-driven Home with a pluggable row contract and a stable/random plan |
| [`:features:home:screenshot`](features-home-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Home |
| [`:features:listitems`](features-listitems.md) | KMP feature | Three list/grid screens, two of them paginated |
| [`:features:listitems:screenshot`](features-listitems-screenshot.md) | Android screenshot leaf | Paparazzi goldens for List Items, List Items with Images and Grid of Images |
| [`:features:search`](features-search.md) | KMP feature | Debounced search, two result sets, persisted recent searches |
| [`:features:search:screenshot`](features-search-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Search |
| [`:features:settings`](features-settings.md) | KMP feature | Preferences, demo controls, reset action, About rows |
| [`:features:settings:screenshot`](features-settings-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Settings |

The root project (`build.gradle.kts`) has no sources. It declares the plugins SAGE's convention
plugins apply by id with `apply false`, so their runtime is on the shared build classpath, and pins
the single app `version` used by every app.

## Dependency shape

```
apps/android ─┐
apps/jvm ─────┤ (demo repositories)
apps/js ──────┤ (remote repositories)
apps/server ──┘ (DemoContent)
        │
        └─ core/ui/shell ─┬─ features/* ── core/repository:api ── core/models
                          │        │
                          │        ├─ core/strings:api
                          │        └─ core/ui/components:api (renderers)
                          ├─ core/nav         (shared shell policy + URL codec)
                          ├─ core/strings:real (text)
                          ├─ core/ui/theme:api (SampleTheme)
                          └─ core/bitmaps      (generated:// URLs)

core/repository:demo  ── core/repository:api + core/settings + SAGE ui.strings (StringGenerator)
core/repository:remote ── core/repository:api + core/settings + Ktor client
apps/server ── core/repository:demo + core/repository:api

features/*/screenshot ── features/* + core/ui/previews ── core/ui/components:api
                                                        └─ core/models + core/strings:real + core/ui/theme:api
```

Rules that hold across the graph:

- **Features are multiplatform.** They use SAGE's `common` modules, `:core:models:api`,
  `:core:strings:api`, `:core:viewmodel` and `:core:repository:api`; no Android types and no
  Compose. That is what lets Android, desktop and the browser share them.
- **Every shared non-UI module is multiplatform.** `:core:models:api`, `:core:nav`,
  `:core:settings`, `:core:viewmodel`, the features and the repository API/remote modules compile
  for Android, JVM and JS. `:core:repository:demo` is deliberately JVM-only (SAGE's
  `StringGenerator` lives in a JVM source set).
- **Features never import a concrete repository.** Android/desktop bind
  `:core:repository:demo`; the browser binds `:core:repository:remote`; the server calls
  `DemoContent` directly. The interfaces, wire DTOs and route constants live in
  `:core:repository:api`.
- **The shared shell owns navigation hosts and rendering.** Features emit `SageEvent`s and
  `ListState`s; `:core:ui:shell` translates and renders them for every app. The *policy* (routes,
  Back, layout, chrome, screen keys) is shared in `:core:nav`; the platform-specific seams stay in
  the apps.
- **Text belongs to `:core:strings:real`.** Every UI-bearing module emits a `SampleStringId`; the
  apps install the shared `StringProvider` and keep no id → text table of their own.
- **Screenshot modules are leaves.** Each depends on its feature and `:core:ui:previews`; nothing
  depends on them, so Paparazzi and the preview harness never reach either app.
- **Text never touches `R.string` outside an app module.** Every UI-bearing module emits a
  `SageStringId` enum; the two apps own the id → text maps.
- **DI is Metro.** Feature view models are contributed with
  `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`; they are
  built by `metroViewModel<...>()` and live in the screen's Voyager store, not the app graph.

## Build conventions

| Convention | Applied by | Effect |
|---|---|---|
| `sage.jvm` | `:core:repository:demo`, `:apps:jvm`, `:apps:server` | Kotlin/JVM 17, detekt, ktlint |
| `sage.di` | `:apps:jvm` | Metro compiler plugin (KMP modules apply `metro` directly) |
| `sage.kmp` + `sage.kmp.js` | features, `:core:models:api`, `:core:nav`, `:core:settings`, `:core:viewmodel`, `:core:repository:api`, `:core:repository:remote`, `:core:bitmaps`, `:core:strings:api`, `:core:strings:real`, `:core:ui:components:api`, `:core:ui:shell`, `:core:ui:theme:api` | Kotlin Multiplatform (Android + JVM, plus JS under `-Psage.js`) |
| `sage.compose.kmp` | `:core:bitmaps`, `:core:ui:components:api`, `:core:ui:shell`, `:core:ui:theme:api` | Compose Multiplatform for the KMP targets |
| `sagesample.screenshot` | `:features:*:screenshot` | `sage.android` + `sage.compose.android` + Paparazzi; derives the namespace from the module path; adds `:core:ui:previews`, `:core:strings:real`, SAGE `appcomm`/`list`; disables JUnit HTML/XML reports (Paparazzi alpha's reporter is not Gradle-9 compatible) |

`:apps:android` applies `android.application`, Compose, Metro, detekt and ktlint directly, because
SAGE's convention plugins target library modules. Its ktlint engine is pinned to the catalog's
`ktlintTool` version so it formats like every other module. `:apps:js` also applies the raw KMP
plugin (single browser target) plus Compose, Metro, serialization and both linters; `:apps:server`
applies `sage.jvm`, serialization and `application`.

`detekt-config.yml` and `.editorconfig` at the repo root are required by the SAGE base plugins.

## Verify

```sh
./gradlew :apps:android:assembleDebug
./gradlew :apps:jvm:run
./gradlew :apps:server:run -Psage.js=true   # browser bundle + server (Node 22, Yarn 1.x)
./gradlew ktlintCheck detekt          # both gate commit/push
./gradlew verifyPaparazziDebug        # check the committed screenshot goldens
scripts/ci/verify.sh                  # CI mirror; must report OVERALL: PASS before push
scripts/ci/verify.sh --web            # ...including the browser bundle + server distribution
```

Unit tests live beside each module: JVM-only modules use `src/test/kotlin`; converted KMP modules
use `src/jvmTest/kotlin` (there is no JS test target). `:core:nav` owns the pure shell policy and
URL codec tests, `:core:ui:shell` owns the tab/chrome tests, `:core:repository:remote` covers the
HTTP client against Ktor's MockEngine, `:apps:server` mounts the shipped route stack in
`testApplication`, and the apps keep only host-local tests. The screenshot modules are Android unit tests (`src/test/java`) that Paparazzi
runs; they are excluded from the generic `test` task so only `verifyPaparazziDebug` owns the LFS
goldens. There is no emulator, device suite or native target.
