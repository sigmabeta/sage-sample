# SAGE Sample

The canonical reference for building an app on [SAGE](../sage). It is deliberately small: one
Android app module, four pure-JVM feature modules (Welcome, plus Browse/Search/Settings
placeholders over a shared `:core:viewmodel` base), and SAGE itself as a Git submodule consumed
through a Gradle composite build.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17+ (CI uses Temurin 21) |
| Android SDK | platform 36 |
| Gradle | 9.5 (wrapper) |

`local.properties` must point at an Android SDK (`sdk.dir=...`). `settings.gradle.kts` copies it
into the `sage/` submodule so both builds resolve the SDK.

## Getting started

```bash
git clone --recurse-submodules <this repo>
cd sage-sample

./gradlew build             # compile, detekt, ktlint, lint, debug + release APK
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

A plain clone needs the submodule initialised first:

```bash
git submodule update --init --recursive
```

## Verification

CI is `.github/workflows/ci.yml`. Three jobs run on every push, pull request, and manual dispatch:

| Job | Command |
|-----|---------|
| `static-analysis` | `./gradlew ktlintCheck detekt --continue` |
| `unit-test` | `./gradlew test --continue` |
| `android` | `./gradlew :app:lintRelease :app:assembleDebug :app:assembleRelease` |

The two compile-heavy jobs wait for `static-analysis`, so a style/rule break fails fast.
Reports, JUnit XML, and the APKs are uploaded as workflow artifacts.

Locally, `scripts/ci/verify.sh` runs the same tasks sequentially, keeps going after a failure so
one run shows everything, and writes a summary with all reports to `build/verification/`:

```bash
scripts/ci/verify.sh                 # everything
scripts/ci/verify.sh --skip-apps     # ktlint/detekt + unit tests only (fast inner loop)
scripts/ci/verify.sh static-analysis # just the named task(s)
scripts/ci/verify.sh --list          # task names
```

The release APK is unsigned: the sample has no release signing config, so `assembleRelease`
produces an unsigned artifact that must be signed before installing.

## Updating SAGE

SAGE is pinned by the submodule commit, so updates are explicit:

```bash
git -C sage fetch origin main
git -C sage checkout origin/main
./gradlew build
git add sage
git commit -m "Bump SAGE"
```

If SAGE renames a module, update the `libs.sage.*` alias used by the module that depends on it
(the version catalog is SAGE's own `gradle/libs.versions.toml`).

## Layout

```
app/                        Android application
  SampleApplication.kt      owns the Metro AppScope graph
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class) : ViewModelGraph
  di/SampleMetroViewModelFactory.kt  the factory behind metroViewModel()
  di/SampleBindings.kt      app-side implementations of SAGE seams
  MainActivity.kt           installs LocalMetroViewModelFactory, hosts the Compose UI
  ui/SampleApp.kt           theme + navigation host
  ui/ListItemContent.kt     renders each SAGE ListModel
  ui/navigation/
    SampleDestination.kt    routes + tab metadata (SAGE RouteDescriptor)
    SampleScreens.kt        one Voyager Screen per destination
    SampleNavHost.kt        the back stack and top-level switch semantics
    SampleNavScaffold.kt    top bar + bottom bar/rail
    SampleTopBar.kt         hamburger on top level, back arrow elsewhere
core/viewmodel/             pure-JVM ViewModel base shared by every feature
  SampleListViewModel.kt    ListState -> ListStateActual, ActionSink, scheduler
features/welcome/           pure-JVM feature module (no Android dependencies)
  WelcomeState.kt           ListState: what the screen contains
  WelcomeViewModel.kt       Metro-contributed ViewModel: state, actions, analytics
  WelcomeStringId.kt        SageStringId keys; the app maps them to R.string
features/browse/            same shape; placeholder content (TODO)
features/search/            same shape; placeholder content (TODO)
features/settings/          same shape; placeholder content (TODO)
detekt-config.yml           ruleset read by the SAGE base plugins
sage/                       SAGE submodule (composite build)
```

## How the wiring works

### Composite build

`settings.gradle.kts` includes SAGE and its build logic, and reuses SAGE's version catalog:

```kotlin
includeBuild("sage/sage-build-logic")
includeBuild("sage")

versionCatalogs {
    create("libs") { from(files("$settingsDir/sage/gradle/libs.versions.toml")) }
}
```

There are no `dependencySubstitution` rules: Gradle matches SAGE's published coordinates
(`net.sigmabeta.sage:list`, `net.sigmabeta.sage.android:themes`, …) to the included projects
automatically. Add a module by using its catalog alias, e.g. `libs.sage.common.ui.listScreens`.

### Conventions

The root `build.gradle.kts` declares the plugins SAGE's convention plugins apply by id
(`android.library`, `kotlin.jvm`, `detekt`, `ktlint`) with `apply false`, so their runtime is on
the shared classpath. A feature module then only declares its convention:

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.sage.di)
}
```

`detekt-config.yml` at the repo root is required: the SAGE base plugins configure detekt with the
consumer's `rootDir/detekt-config.yml`.

### Dependency injection — Metro, not Hilt

SAGE uses [Metro](https://github.com/ZacSweers/metro). There is no Hilt, KSP, or Dagger in this
app. The entry points:

- `@DependencyGraph(AppScope::class)` on `SampleAppGraph` declares the graph; it extends
  `ViewModelGraph`, which carries Metro's ViewModel multibinding map.
- `@SingleIn(AppScope::class)` marks app-lifetime bindings.
- `@BindingContainer` + `@ContributesTo(AppScope::class)` contributes a module from any module.
- `@Inject` (from `dev.zacsweers.metro`) marks injectable classes/constructors.
- `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey` puts a
  screen's view model in the ViewModel map, where `metroViewModel<...>()` finds it.
- `createGraphFactory<SampleAppGraph.Factory>().create(application)` builds it; Activity/Service
  code reads bindings from the graph rather than receiving member injection.

The feature module applies `sage.di`, so its view model's constructor (and its ViewModel-map entry)
is processed at compile time. `SampleMetroViewModelFactory` is the contributed
`MetroViewModelFactory` that reads that map; `MainActivity` installs it with
`LocalMetroViewModelFactory`, and screens call `metroViewModel<TheirViewModel>()`.

### Screens

SAGE splits a screen in two:

1. The feature owns the state machine. `WelcomeState : ListState` declares a title and a list of
   `ListModel`s; `WelcomeViewModel : SampleListViewModel<WelcomeState>` owns actions and analytics.
   `SampleListViewModel` (`:core:viewmodel`) is an `androidx.lifecycle.ViewModel` base with
   `uiStateActual` mapped from `ListState` — SAGE's plain `ListViewModelBrain` (and the graph
   accessor per screen) is a Dagger-era shell that Metro does not need.
2. The app owns rendering. `SampleScreens` resolves the view model with
   `metroViewModel<WelcomeViewModel>()`, observes `uiStateActual`, and passes it to `ListScreen`,
   supplying `ListItemContent` to draw each `ListModel`. Unknown model types fall through to a
   spacer, so features can add their own models without breaking the app.

Text goes through `StringProvider` and a feature-owned `SageStringId` enum, so feature modules stay
platform-agnostic; `SampleBindings` is the only place that knows about `R.string`.

### Navigation

Navigation uses Voyager (from SAGE's catalog). `SampleNavHost` owns one `Navigator`:

- **Destinations** are in `SampleDestination`, which implements SAGE's `RouteDescriptor`.
- **Home, Browse and Search** are top-level: a `NavigationBar` on compact windows and a
  `NavigationRail` at 600 dp and wider. Selecting one resets the stack to `[home, target]`, so
  back from Browse or Search returns Home.
- **Settings** is pushed from the hamburger. The leading top-bar icon is the hamburger on
  top-level screens and a back arrow on Settings (it pops).
- **Each screen resolves its own view model** with `metroViewModel<...>()`; view models are
  activity-scoped (the Compose `ViewModelStoreOwner`), so a screen that is popped and pushed again
  finds its state until the activity goes away.

## Adding a feature

1. Create `features/<name>` with the `sage.jvm` + `sage.di` convention, and depend on
   `projects.core.viewmodel` plus the SAGE modules it needs.
2. Add `:features:<name>` to `settings.gradle.kts` and `implementation(projects.features.<name>)`
   in `app/build.gradle.kts`.
3. Write a `<Name>State : ListState` and a `<Name>ViewModel : SampleListViewModel<<Name>State>`,
   using a `<Name>StringId : SageStringId` enum for text. Annotate the view model with
   `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey`, and
   send `SageAction.InitNoArgs` from `init`.
4. Map the string ids in `SampleBindings`; no graph accessor is needed for the view model.
5. To make it reachable, add a `SampleDestination`, a `SampleScreen` (resolving it with
   `metroViewModel`), and a `screenFor` branch in `SampleNavHost` — plus a nav label in
   `strings.xml` if it becomes a top-level tab.
