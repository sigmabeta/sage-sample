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
| [`:core:bitmaps`](core-bitmaps.md) | KMP Compose library | Deterministic placeholder art: generator, `@Preview` composable, `generated://` Coil fetcher |
| [`:core:models:api`](core-models-api.md) | Pure-JVM library | Shared domain models + the Content detail route codec |
| [`:core:nav`](core-nav.md) | Pure-JVM library | Shared destinations, route/Back policy, layout and top-bar derivation, screen keys |
| [`:core:settings`](core-settings.md) | Pure-JVM library | Persisted preferences, demo outcome, simulated delay, recent-search history |
| [`:core:strings:api`](core-strings-api.md) | KMP library | One `SampleStringId` vocabulary for every user-visible string |
| [`:core:strings:real`](core-strings-real.md) | KMP library | The single id → English table (`SampleStringProvider`) |
| [`:core:ui:components:api`](core-ui-components-api.md) | KMP Compose library | Shared renderer suite: SAGE `ListModel` → composables, search chrome |
| [`:core:ui:previews`](core-ui-previews.md) | Android test-support library | VM-free `ListState` rendering, device matrix and `FakeModelGenerator` |
| [`:core:ui:shell`](core-ui-shell.md) | KMP Compose library | Shared composition root and Voyager navigation host |
| [`:core:ui:theme:api`](core-ui-theme-api.md) | KMP Compose library | `SampleTheme` + the shared type scale |
| [`:core:viewmodel`](core-viewmodel.md) | Pure-JVM library | `SampleListViewModel`: the base every feature screen builds on |
| [`:features:browse`](features-browse.md) | Pure-JVM feature | Static destination list that opens the demo screens |
| [`:features:browse:screenshot`](features-browse-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Browse |
| [`:features:contentdetail`](features-contentdetail.md) | Pure-JVM feature | Parameterized detail screen over seeded generated content |
| [`:features:contentdetail:screenshot`](features-contentdetail-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Content detail |
| [`:features:home`](features-home.md) | Pure-JVM feature | Module-driven Home with a pluggable row contract and a stable/random plan |
| [`:features:home:screenshot`](features-home-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Home |
| [`:features:listitems`](features-listitems.md) | Pure-JVM feature | Three list/grid screens, two of them paginated |
| [`:features:listitems:screenshot`](features-listitems-screenshot.md) | Android screenshot leaf | Paparazzi goldens for List Items, List Items with Images and Grid of Images |
| [`:features:search`](features-search.md) | Pure-JVM feature | Debounced search, two result sets, persisted recent searches |
| [`:features:search:screenshot`](features-search-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Search |
| [`:features:settings`](features-settings.md) | Pure-JVM feature | Preferences, demo controls, reset action, About rows |
| [`:features:settings:screenshot`](features-settings-screenshot.md) | Android screenshot leaf | Paparazzi goldens for Settings |

The root project (`build.gradle.kts`) has no sources. It declares the plugins SAGE's convention
plugins apply by id with `apply false`, so their runtime is on the shared build classpath, and pins
the single app `version` used by both apps.

## Dependency shape

```
apps/android ─┐
apps/jvm ─────┴─ core/ui/shell ─┬─ features/* ── core/viewmodel ── core/settings
                                │        │
                                │        ├─ core/models
                                │        ├─ core/strings:api
                                │        └─ core/ui/components:api (renderers)
                                ├─ core/nav         (shared shell policy)
                                ├─ core/strings:real (text)
                                ├─ core/ui/theme:api (SampleTheme)
                                └─ core/bitmaps      (generated:// URLs)

apps/* also depend on core/strings:real, core/settings, core/ui/components:api,
core/bitmaps and core:ui:shell; everything else reaches them through the shell.

features/*/screenshot ── features/* + core/ui/previews ── core/ui/components:api
                                                        └─ core/models + core/strings:real + core/ui/theme:api
```

Rules that hold across the graph:

- **Features are pure JVM.** They use SAGE's `common` (multiplatform) modules,
  `:core:models:api`, `:core:strings:api` and `:core:viewmodel`; no Android types and no Compose.
  That is what lets both apps share them.
- **`:core:models:api`, `:core:nav`, `:core:settings` and `:core:viewmodel` are pure JVM.**
  Only `:core:bitmaps`, `:core:strings:*` and the `:core:ui:*` libraries are multiplatform.
- **The shared shell owns navigation hosts and rendering.** Features emit `SageEvent`s and
  `ListState`s; `:core:ui:shell` translates and renders them for both apps. The *policy* (routes,
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
| `sage.jvm` | features, `:core:models:api`, `:core:nav`, `:core:settings`, `:core:viewmodel`, `:apps:jvm` | Kotlin/JVM 17, detekt, ktlint |
| `sage.di` | features, `:core:settings`, `:core:viewmodel`, `:apps:jvm` | Metro compiler plugin |
| `sage.kmp` | `:core:bitmaps`, `:core:strings:api`, `:core:strings:real`, `:core:ui:components:api`, `:core:ui:shell`, `:core:ui:theme:api` | Kotlin Multiplatform (Android + JVM targets) |
| `sage.compose.kmp` | `:core:bitmaps`, `:core:ui:components:api`, `:core:ui:shell`, `:core:ui:theme:api` | Compose Multiplatform for the KMP targets |
| `sagesample.screenshot` | `:features:*:screenshot` | `sage.android` + `sage.compose.android` + Paparazzi; derives the namespace from the module path; adds `:core:ui:previews`, `:core:strings:real`, SAGE `appcomm`/`list`; disables JUnit HTML/XML reports (Paparazzi alpha's reporter is not Gradle-9 compatible) |

`:apps:android` applies `android.application`, Compose, Metro, detekt and ktlint directly, because
SAGE's convention plugins target library modules. Its ktlint engine is pinned to the catalog's
`ktlintTool` version so it formats like every other module.

`detekt-config.yml` and `.editorconfig` at the repo root are required by the SAGE base plugins.

## Verify

```sh
./gradlew :apps:android:assembleDebug
./gradlew :apps:jvm:run
./gradlew ktlintCheck detekt          # both gate commit/push
./gradlew verifyPaparazziDebug        # check the committed screenshot goldens
scripts/ci/verify.sh                  # CI mirror; must report OVERALL: PASS before push
```

Unit tests live beside each module (`src/test/kotlin`), except `:core:bitmaps`,
`:core:strings:real` and `:core:ui:shell`, which use the KMP `jvmTest` source set. `:core:nav`
owns the pure shell policy tests, `:core:ui:shell` owns the tab/chrome tests, and the apps keep
only host-local tests. The screenshot modules are Android unit tests (`src/test/java`) that Paparazzi
runs; they are excluded from the generic `test` task so only `verifyPaparazziDebug` owns the LFS
goldens. There is no emulator, device suite or native target.
