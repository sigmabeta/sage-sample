# SAGE Sample

The canonical reference for building an app on [SAGE](../sage). It is deliberately small:
one Android app module, one pure-JVM feature module, and SAGE itself as a Git submodule consumed
through a Gradle composite build.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
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
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class)
  di/SampleBindings.kt      app-side implementations of SAGE seams
  MainActivity.kt           reads the graph, hosts the Compose UI
  ui/SampleApp.kt           Scaffold + SAGE ListScreen
  ui/ListItemContent.kt     renders each SAGE ListModel
features/welcome/           pure-JVM feature module (no Android dependencies)
  WelcomeState.kt           ListState: what the screen contains
  WelcomeViewModelBrain.kt  ListViewModelBrain: state, actions, events, analytics
  WelcomeStringId.kt        SageStringId keys; the app maps them to R.string
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

- `@DependencyGraph(AppScope::class)` on `SampleAppGraph` declares the graph.
- `@SingleIn(AppScope::class)` marks app-lifetime bindings.
- `@BindingContainer` + `@ContributesTo(AppScope::class)` contributes a module from any module.
- `@Inject` (from `dev.zacsweers.metro`) marks injectable constructors.
- `createGraphFactory<SampleAppGraph.Factory>().create(application)` builds it; Activity/Service
  code reads bindings from the graph rather than receiving member injection.

The feature module applies `sage.di`, so its brain's `@Inject` constructor is available to the
app graph without a hand-written provider.

### Screens

SAGE splits a screen in two:

1. The feature owns the state machine. `WelcomeState : ListState` declares a title and a list of
   `ListModel`s; `WelcomeViewModelBrain : ListViewModelBrain` owns actions, events, and analytics.
2. The app owns rendering. `SampleApp` observes `brain.uiStateActual` and passes it to
   `ListScreen`, supplying `ListItemContent` to draw each `ListModel`. Unknown model types fall
   through to a spacer, so features can add their own models without breaking the app.

Text goes through `StringProvider` and a feature-owned `SageStringId` enum, so feature modules stay
platform-agnostic; `SampleBindings` is the only place that knows about `R.string`.

## Adding a feature

1. Create `features/<name>` with the `sage.jvm` + `sage.di` convention, and depend on the SAGE
   modules it needs.
2. Add `:features:<name>` to `settings.gradle.kts` and `implementation(projects.features.<name>)`
   in `app/build.gradle.kts`.
3. Write a `ListState` + `ListViewModelBrain`, using a `<Name>StringId : SageStringId` enum for
   text.
4. Expose the brain from `SampleAppGraph` (if an entry point needs it) and map the string ids in
   `SampleBindings`.
