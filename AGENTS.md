# AGENTS.md — SAGE Sample orientation

Orientation for coding agents. Keep this file lean; update it when a load-bearing
fact below stops being true.

## What this repo is

The canonical reference for building an app on [SAGE](sage/): one Android app
module (`:app`), four pure-JVM feature modules (`:features:welcome` plus
browse/search/settings placeholders) over a shared ViewModel base (`:core:viewmodel`),
and SAGE itself as a Git submodule consumed through a Gradle composite build. It is
deliberately small — patterns added here are meant to be copied by real SAGE apps, so
prefer the conventional SAGE way over a local shortcut.

## Before doing any work

- Refresh SAGE to its current `main` line:
  `git -C sage fetch origin main && git -C sage checkout origin/main`.
  If that won't check out cleanly, stop and ask rather than merging or resetting.
  The submodule is a detached HEAD at a pinned commit — that is expected; this repo
  is what pins it.
- On a fresh clone, initialise the submodule first:
  `git submodule update --init --recursive`.
- `sage/README.md` documents SAGE's module inventory and build conventions. Every
  SAGE module has its own `README.md`; read the one you're about to depend on.
- `local.properties` (gitignored) must point at an Android SDK with platform 36.
  `settings.gradle.kts` copies it into `sage/local.properties` for the included build.
- JDK 17 or newer is required (CI uses Temurin 21).

## Load-bearing facts (verified 2026-11-05)

- **DI is Metro** (`dev.zacsweers.metro`), not Hilt/Dagger/KSP. The app graph is
  `SampleAppGraph` (`@DependencyGraph(AppScope::class)`), created once in
  `SampleApplication` via `createGraphFactory<SampleAppGraph.Factory>().create(this)`.
  App-side bindings live in `SampleBindings`
  (`@BindingContainer` + `@ContributesTo(AppScope::class)`).
- **Screens are ViewModels, not graph-resolved brains.** Each feature's
  `<Name>ViewModel` is an `androidx.lifecycle.ViewModel` contributed into Metro's
  ViewModel map (`@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())`
  + `@ViewModelKey`) and resolved by its screen with `metroViewModel<...>()`.
  `SampleAppGraph : ViewModelGraph` carries the map, and a contributed
  `SampleMetroViewModelFactory` supplies `metroViewModelFactory`, which `MainActivity`
  installs via `LocalMetroViewModelFactory`. Do not add per-screen graph accessors and
  do not go back to SAGE's `ListViewModelBrain`: that plain-class shell is the
  Dagger-era path this structure replaces.
- **Navigation is Voyager** (`cafe.adriel.voyager`), owned by `SampleNavHost`'s single
  `Navigator`. Home/Browse/Search are top-level; Settings is pushed from the hamburger.
  The top-bar icon is the hamburger on top-level destinations and a back arrow
  (pop) elsewhere. Selecting a top-level destination resets the stack to
  `[Home, target]`, so back from Browse/Search returns Home. The nav suite is a
  `NavigationBar` below 600 dp and a `NavigationRail` above it — hand-rolled because
  the `material3-adaptive-navigation-suite` artifact pins material3 older than the BOM.
- **ViewModels are activity-scoped**, not app-scoped: they come from the Compose
  `ViewModelStoreOwner`, so popping and re-pushing a screen keeps its state until the
  activity goes away. `viewModelScope` replaces the app-wide coroutine scope (each VM
  builds its own `SageScheduler` in `:core:viewmodel`).
- **Screen split:** a feature owns the state machine and the app owns rendering.
  `WelcomeState : ListState` declares the content; `WelcomeViewModel :
  SampleListViewModel<WelcomeState>` owns actions and analytics; `SampleScreens`
  resolves it with `metroViewModel`, observes `uiStateActual` and passes it to SAGE's
  `ListScreen`; `ListItemContent` renders each `ListModel`. Unknown model types fall
  through to a spacer.
- **Text never touches `R.string` in a feature.** Features expose a
  `SageStringId` enum (`WelcomeStringId`) and read text through `StringProvider`.
  `SampleBindings` is the only place that maps ids to resources — adding a string
  means adding the enum constant **and** its `R.string` mapping. Navigation labels
  are app-owned, so `SampleDestination` holds `@StringRes` ids directly.
- **Module conventions:** `:features:*` and `:core:viewmodel` apply `sage.jvm` (features
  also apply `sage.di`). `:app` applies `android.application`, Compose, `metro`, `detekt`,
  and `ktlint` directly, because SAGE's convention plugins target library modules, not
  applications. That is why the app pins the ktlint engine from the catalog itself.
- **Build wiring:** `settings.gradle.kts` includes `sage/sage-build-logic` and
  `sage` as composite builds and reuses SAGE's version catalog as `libs`. There are
  no `dependencySubstitution` rules — SAGE project coordinates
  (`net.sigmabeta.sage:*`) resolve automatically. Add modules with
  `projects.features.<name>`; add dependencies through `libs.sage.*` aliases, never
  with a local version.
- **Lint config is required:** `detekt-config.yml` at the repo root is read by
  SAGE's convention plugins. `.editorconfig` mirrors SAGE's ktlint ruleset,
  including the `@Composable` PascalCase exemption — don't delete either.
- **No tests exist yet.** `./gradlew test` and CI's `unit-test` job are already
  wired, so adding `src/test` sources needs no build changes. The feature modules are
  pure JVM (plain JUnit); the app would use Android unit tests.
- There is no Paparazzi/screenshot setup, no device test suite, no native code, and
  no desktop/JS target. Don't boot an emulator to verify changes.

## Build / verify

```sh
./gradlew :app:assembleDebug              # Android build
./gradlew :app:installDebug               # install on an attached device
./gradlew ktlintCheck detekt              # lint — run BOTH, they gate commit/push
./gradlew ktlintFormat                    # auto-fix ktlint findings
scripts/ci/verify.sh                      # full CI-mirroring suite
```

- **Always run `scripts/ci/verify.sh` before any `git push`**, and only push when it
  reports `OVERALL: PASS`. It runs the same Gradle tasks as
  `.github/workflows/ci.yml` sequentially, keeps going after a failure so one run
  shows everything, and collates logs, reports, JUnit XML, and APKs into
  `build/verification/` with a `summary.txt`.
  `scripts/ci/verify.sh --list` / `--skip-apps` / `--rerun` / `<task-name>…` scope it.
- Heed its `WARNING:` lines too: uncommitted changes or a SAGE commit that isn't on
  a remote branch mean CI may not see what you tested.
- **CI mirrors the script; change them together.** `.github/workflows/ci.yml` has
  three jobs: `static-analysis` (ktlint + detekt, the gate), then `unit-test` and
  `android` (lint + debug/release APK). It runs on every push, PR, and manual
  dispatch with Temurin 21 and SDK platform 36. Reports and APKs are uploaded as
  artifacts.
- The release build type has no signing config, so `assembleRelease` produces an
  unsigned APK.
- Gradle prints thousands of task lines: redirect a long run to a file and grep it
  (`./gradlew … > /tmp/build.log 2>&1`) rather than pulling them into context.
- Don't boot an AVD. Stop after build + lint and hand device testing to the user.

## SAGE changes

`sage/` is a separate repository (`git@github.com:sigmabeta/sage.git`). Treat
changes there as needing explicit sign-off, and keep the two commits in order:

1. Land the SAGE change on `sage`'s `main` line through its normal PR flow.
2. Bump the gitlink here, then run `scripts/ci/verify.sh`.
3. Commit the bump in this repo (`git add sage && git commit -m "Bump SAGE"`).

Normal SAGE updates follow the same flow:

```sh
git -C sage fetch origin main
git -C sage checkout origin/main
scripts/ci/verify.sh
git add sage
git commit -m "Bump SAGE"
```

If a SAGE module is renamed, update the `libs.sage.*` alias used by the module that
depends on it (the catalog is SAGE's own `gradle/libs.versions.toml`).

## Push gate

`git push` requires all of the following — no exceptions for "tiny changes":

1. `scripts/ci/verify.sh` reports `OVERALL: PASS`.
2. The human has explicitly approved the push.
3. If the `sage` gitlink changed, the pinned commit is already on SAGE's remote.
   Verify with `git -C sage branch -r --contains "$(git rev-parse :sage)"`;
   otherwise CI's submodule checkout fails with `upload-pack: not our ref`.

Don't `git commit` or `git push` unless explicitly asked.

## Layout

```
app/                        Android application
  SampleApplication.kt      owns the Metro AppScope graph
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class) : ViewModelGraph
  di/SampleBindings.kt      app-side implementations of SAGE seams
  di/SampleMetroViewModelFactory.kt  the factory behind metroViewModel()
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
  SampleListViewModel.kt    ListState -> ListStateActual, ActionSink, viewModelScope scheduler
features/welcome/           pure-JVM feature module (no Android dependencies)
  WelcomeState.kt           ListState: what the screen contains
  WelcomeViewModel.kt       Metro-contributed ViewModel: state, actions, analytics
  WelcomeStringId.kt        SageStringId keys; the app maps them to R.string
features/browse/            same shape; placeholder content (TODO)
features/search/            same shape; placeholder content (TODO)
features/settings/          same shape; placeholder content (TODO)
.github/workflows/ci.yml    CI: static analysis, unit tests, Android build
scripts/ci/verify.sh        local mirror of CI + artifact collation
detekt-config.yml           ruleset read by the SAGE base plugins
.editorconfig               ktlint ruleset, mirrors SAGE
sage/                       SAGE submodule (composite build)
```

## Agent skills

### Issue tracker

Issues and specs live as markdown files under `.scratch/<feature-slug>/`; triage state is a `Status:` line. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, default strings: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` plus `docs/adr/` at the repo root. See `docs/agents/domain.md`.
