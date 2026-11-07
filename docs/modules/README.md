# Module reference

One page per Gradle module in this repository. Each page covers the module's job, how it is built,
what it depends on and why, the key types it exposes, and what its tests pin down. For the
end-to-end architecture (navigation, screens, DI, layouts) see
[`README.md`](../../README.md); for review-time rules see
[`CODING_STANDARDS.md`](../../CODING_STANDARDS.md).

The build is a **Gradle composite build**: `settings.gradle.kts` includes `sage/` and
`sage/sage-build-logic`, and reuses SAGE's own version catalog (`libs`). There are no
`dependencySubstitution` rules — SAGE's published coordinates resolve to the included projects
automatically.

## Module map

| Module | Kind | One-line role |
|---|---|---|
| [`:apps:android`](apps-android.md) | Android application | Activity, Metro app graph, Android platform seams, navigation shell |
| [`:apps:jvm`](apps-jvm.md) | Compose Desktop application | Window entry point, desktop graph, JVM platform seams, same navigation shell |
| [`:core:bitmaps`](core-bitmaps.md) | KMP Compose library | Deterministic placeholder art: generator, `@Preview` composable, `generated://` Coil fetcher |
| [`:core:settings`](core-settings.md) | Pure-JVM library | Persisted preferences, demo outcome, simulated delay, recent-search history |
| [`:core:ui:components:api`](core-ui-components-api.md) | KMP Compose library | Shared renderer suite: SAGE `ListModel` → composables, search chrome, typography |
| [`:core:viewmodel`](core-viewmodel.md) | Pure-JVM library | `SampleListViewModel`: the base every feature screen builds on |
| [`:features:browse`](features-browse.md) | Pure-JVM feature | Static destination list that opens the demo screens |
| [`:features:contentdetail`](features-contentdetail.md) | Pure-JVM feature | Parameterized detail screen over seeded generated content |
| [`:features:home`](features-home.md) | Pure-JVM feature | Module-driven Home with a pluggable row contract and a stable/random plan |
| [`:features:listitems`](features-listitems.md) | Pure-JVM feature | Three list/grid screens, two of them paginated |
| [`:features:search`](features-search.md) | Pure-JVM feature | Debounced search, two result sets, persisted recent searches |
| [`:features:settings`](features-settings.md) | Pure-JVM feature | Preferences, demo controls, reset action, About rows |

The root project (`build.gradle.kts`) has no sources. It declares the plugins SAGE's convention
plugins apply by id with `apply false`, so their runtime is on the shared build classpath, and pins
the single app `version` used by both apps.

## Dependency shape

```
apps/android ─┬─ features/* ── core/viewmodel ── core/settings
apps/jvm ─────┘        │
                       ├─ features/contentdetail (route codec)
                       ├─ core/bitmaps          (generated:// URLs)
                       └─ core/ui/components:api (renderers, SAGE ListModel re-export)

apps/* also depend on core/ui/components:api, core/bitmaps, core/settings
```

Rules that hold across the graph:

- **Features are pure JVM.** They use SAGE's `common` (multiplatform) modules and
  `:core:viewmodel`; no Android types and no Compose. That is what lets both apps share them.
- **`:core:settings` and `:core:viewmodel` are pure JVM too.** Only `:core:bitmaps` and
  `:core:ui:components:api` are multiplatform, because both apps render their Compose code.
- **The app owns navigation and rendering.** Features emit `SageEvent`s and `ListState`s; the app
  translates and renders them.
- **Text never touches `R.string` outside an app module.** Every UI-bearing module emits a
  `SageStringId` enum; the two apps own the id → text maps.
- **DI is Metro.** Feature view models are contributed with
  `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`; they are
  built by `metroViewModel<...>()` and live in the screen's Voyager store, not the app graph.

## Build conventions

| Convention | Applied by | Effect |
|---|---|---|
| `sage.jvm` | features, `:core:settings`, `:core:viewmodel`, `:apps:jvm` | Kotlin/JVM 17, detekt, ktlint |
| `sage.di` | features, `:core:settings`, `:apps:jvm` | Metro compiler plugin |
| `sage.kmp` | `:core:bitmaps`, `:core:ui:components:api` | Kotlin Multiplatform (Android + JVM targets) |
| `sage.compose.kmp` | `:core:bitmaps`, `:core:ui:components:api` | Compose Multiplatform for the KMP targets |

`:apps:android` applies `android.application`, Compose, Metro, detekt and ktlint directly, because
SAGE's convention plugins target library modules. Its ktlint engine is pinned to the catalog's
`ktlintTool` version so it formats like every other module.

`detekt-config.yml` and `.editorconfig` at the repo root are required by the SAGE base plugins.

## Verify

```sh
./gradlew :apps:android:assembleDebug
./gradlew :apps:jvm:run
./gradlew ktlintCheck detekt          # both gate commit/push
scripts/ci/verify.sh                  # CI mirror; must report OVERALL: PASS before push
```

Unit tests live beside each module (`src/test/kotlin`), except `:core:bitmaps`, which tests the KMP
`jvmTest` source set. There is no emulator, device suite, Paparazzi or native target.
