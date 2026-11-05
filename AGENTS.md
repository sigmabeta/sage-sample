# AGENTS.md — SAGE Sample orientation

Orientation for coding agents. Keep this file lean; update it when a load-bearing
fact below stops being true.

## What this repo is

The canonical reference for building an app on [SAGE](sage/): one Android app
module (`:app`), one pure-JVM feature module (`:features:welcome`), and SAGE itself
as a Git submodule consumed through a Gradle composite build. It is deliberately
small — patterns added here are meant to be copied by real SAGE apps, so prefer the
conventional SAGE way over a local shortcut.

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
  (`@BindingContainer` + `@ContributesTo(AppScope::class)`). `WelcomeViewModelBrain`
  is `@Inject`; the app requests it directly from the graph. Entry points read the
  graph off `Application` — there is no `@AndroidEntryPoint` member injection.
- **Navigation is not wired in this sample.** `MainActivity` hosts `SampleApp`,
  which renders one SAGE `ListScreen`. Voyager is available through SAGE's catalog,
  but only add navigation if the task is explicitly about navigation.
- **Screen split:** a feature owns the state machine and the app owns rendering.
  `WelcomeState : ListState` declares the content; `WelcomeViewModelBrain :
  ListViewModelBrain` owns actions, events, and analytics; `SampleApp` observes
  `uiStateActual` and passes it to SAGE's `ListScreen`; `ListItemContent` renders
  each `ListModel`. Unknown model types fall through to a spacer.
- **Text never touches `R.string` in a feature.** Features expose a
  `SageStringId` enum (`WelcomeStringId`) and read text through `StringProvider`.
  `SampleBindings` is the only place that maps ids to resources — adding a string
  means adding the enum constant **and** its `R.string` mapping.
- **Module conventions:** `:features:*` applies `sage.jvm` + `sage.di`. `:app`
  applies `android.application`, Compose, `metro`, `detekt`, and `ktlint` directly,
  because SAGE's convention plugins target library modules, not applications. That
  is why the app pins the ktlint engine from the catalog itself.
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
  wired, so adding `src/test` sources needs no build changes. The feature module is
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
  di/SampleAppGraph.kt      @DependencyGraph(AppScope::class)
  di/SampleBindings.kt      app-side implementations of SAGE seams
  di/SchedulerImpl.kt       app's SageScheduler implementation
  MainActivity.kt           reads the graph, hosts the Compose UI
  ui/SampleApp.kt           Scaffold + SAGE ListScreen
  ui/ListItemContent.kt     renders each SAGE ListModel
features/welcome/           pure-JVM feature module (no Android dependencies)
  WelcomeState.kt           ListState: what the screen contains
  WelcomeViewModelBrain.kt  ListViewModelBrain: state, actions, events, analytics
  WelcomeStringId.kt        SageStringId keys; the app maps them to R.string
.github/workflows/ci.yml    CI: static analysis, unit tests, Android build
scripts/ci/verify.sh        local mirror of CI + artifact collation
detekt-config.yml           ruleset read by the SAGE base plugins
.editorconfig               ktlint ruleset, mirrors SAGE
sage/                       SAGE submodule (composite build)
```
