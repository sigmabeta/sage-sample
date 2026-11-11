# AGENTS.md — SAGE Sample orientation

Orientation for coding agents. **Architecture lives in [`README.md`](README.md)** — its
"How the wiring works", "Screens", "Navigation" and "Layout" sections. This file is the
agent-only guardrails and pointers; prefer a pointer over repeating the README.

## What this repo is

The canonical reference for building an app on [SAGE](sage/): one Android app (`:apps:android`), one
Compose Desktop app (`:apps:jvm`), one Kotlin/JS browser app (`:apps:js`) with a Ktor server
(`:apps:server`) that serves it, six multiplatform feature modules over `:core:viewmodel`, and SAGE
itself as a Git submodule consumed through a Gradle composite build. Patterns added here are
copied by real SAGE apps, so prefer the conventional SAGE way over a local shortcut.

## Before doing any work

- Refresh SAGE to its current `main` line:
  `git -C sage fetch origin main && git -C sage checkout origin/main`.
  If that won't check out cleanly, stop and ask rather than merging or resetting. The
  submodule is a detached HEAD at a pinned commit — this repo is what pins it.
- On a fresh clone, initialise the submodule: `git submodule update --init --recursive`.
- Read the `README.md` of every SAGE module you depend on (`sage/<module>/README.md`).
- `local.properties` (gitignored) must point at an Android SDK with platform 36;
  `settings.gradle.kts` copies it into `sage/local.properties`. JDK 17+ (CI uses Temurin 21).

## Read before changing behaviour

- [`README.md § How the wiring works`](README.md#how-the-wiring-works) — Metro DI, screens as
  ViewModels, Voyager navigation, the composite build, the version catalog.
- [`README.md § Screens`](README.md#screens) — feature-owns-state / app-owns-rendering, text
  ids, list-versus-grid selection.
- [`CODING_STANDARDS.md`](CODING_STANDARDS.md) — the review-time rules for closed-set tests,
  intent mapping, and text ownership.

## Guardrails

- **Code is never duplicated between apps unless absolutely necessary.** Anything more than one
  app needs belongs in a shared module — Compose/Voyager host code in
  `:core:ui:shell`, policy in `:core:nav`, text in `:core:strings:real`, and so on. An app keeps
  only what is genuinely platform-specific (entry point, graph/bindings, storage backend, Back
  input, window lifecycle, bundled font loading, the HTTP/local storage seams). If you find the
  same logic in more than one app, move it out rather than letting the copies drift.
- **DI is Metro** (`dev.zacsweers.metro`), not Hilt/Dagger. A screen contributes
  `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey` and
  resolves with `metroViewModel<...>()`. Do not add per-screen graph accessors and do not go
  back to SAGE's `ListViewModelBrain`/`BrainProvider`.
- **A screen ViewModel lives in its Voyager screen's store**, not the app graph: no `@SingleIn`
  on a screen VM. Popping and re-pushing builds a fresh one; a tab root survives tab switches
  because each tab owns a retained inner `Navigator` (`TabNavigator`).
- **Features depend on repository interfaces, never on a concrete data source.**
  `:core:repository:api` holds the seams, wire DTOs and API route constants;
  `:core:repository:demo` holds the deterministic in-process sources (Android/desktop and
  `apps:server`); `:core:repository:remote` is the Ktor client the browser binds. A feature module
  must not import the demo or remote implementations.
- **Web is server-backed; local demo data is the non-web default.** The browser app must not depend
  on `:core:repository:demo` (it would drag the JVM-only `StringGenerator` into JS). All apps share
  `SampleRoute`/`SampleRouteUrl`, so URLs are derived from one pure codec.
- **Features emit `SageEvent`s; the app owns navigation.** The pure policy lives in `:core:nav`:
  `navActionFor` decides `ShowTopLevel` (select that tab),
  `Push` (onto the active tab), or `Pop` (the shared Back policy). Don't navigate from a feature
  module.
- **Back is one policy:** `backActionFor(tab, canPop)` is shared by the top-bar arrow, platform
  Back, and `SageEvent.NavigateBack`. A pushed screen pops inside the active tab; Search root
  returns Home; Home/Browse roots fall through to the host.
- **Chrome comes from the active screen's `ListStateActual.title`** (`title` +
  `shouldShowBack`), with the destination as the pre-report fallback. Navigation-tab labels and
  icons stay app-owned.
- **Layout is derived:** `screenLayoutFor(columnType, widthClass)` keeps `ListScreen` at one or
  zero columns and uses `GridScreen` (staggered when the column type asks) above that.
- **One foreground pair:** the shared screen sends `Resume`/`Pause`; SAGE's list screens also
  send `Resume` on title change, coalesced in `SampleListViewModel.sendAction`.
- **Text lives in `:core:strings:real`.** Every UI-bearing module emits a `SampleStringId`
  (`:core:strings:api`); `SampleStringProvider` is the single id → text table. Demo/repository
  content included. The apps bind it into their graph and install it as
  `LocalSampleStringProvider`; they own no text table (Android keeps only `app_name` for the
  manifest). The renderer suite's accessibility labels are `SampleStringId`s in the same enum.
- **Renderers are shared, not per-app.** `:core:ui:components:api` is the sample's KMP Compose
  suite: `ListModel.Content` dispatches every SAGE model it knows to a renderer, and it re-exports
  `libs.sage.common.ui.components`. Both apps supply
  `ListModel.Content` to `ListScreen`/`GridScreen`; don't reintroduce app-local `ListItemContent`.
- **The composition root and Compose/Voyager shell live in `:core:ui:shell`.** That module owns
  `SampleApp`, the tabs, screens, nav host/scaffold/top bar, chrome and screen lifecycle. An app
  keeps only its entry point, Metro graph/bindings, storage, and platform seams: a
  `SamplePlatformBack`, a `SampleScreenContainer` when its navigator lacks per-screen stores, a
  screen lifecycle owner, and its Inter family. Don't re-add a navigation host or a
  destination-to-screen mapping to an app.
- **Module plugins:** `:features:*`, `:core:models:api`, `:core:nav`, `:core:viewmodel`,
  `:core:settings`, `:core:repository:api` and `:core:repository:remote` apply `sage.kmp` +
  `sage.kmp.js` (the ones with Metro bindings also apply `metro` directly — `sage.di`'s
  top-level `implementation` does not fit KMP source sets). `:core:bitmaps`,
  `:core:strings:api`, `:core:strings:real`, `:core:ui:components:api`, `:core:ui:shell` and
  `:core:ui:theme:api` apply `sage.kmp` + `sage.kmp.js` (the UI/renderer modules also
  `sage.compose.kmp`). `:core:repository:demo` is JVM-only (`sage.jvm` + `metro`) because SAGE's
  `StringGenerator` lives in a JVM source set. `:features:*:screenshot` applies the sample's
  `sagesample.screenshot` convention (from `build-logic/`), which layers `sage.android` +
  `sage.compose.android` + Paparazzi; `:apps:jvm` applies `sage.jvm` + `sage.di` + Compose Desktop
  (the Compose compiler and JetBrains Compose plugins); `:apps:android` applies
  `android.application`, Compose, `metro`, `detekt`, and `ktlint` directly; `:apps:js` applies the
  raw KMP plugin + Compose + Metro + serialization + ktlint/detekt (single JS target);
  `:apps:server` applies `sage.jvm` + serialization + `application`. Add SAGE dependencies
  through `libs.sage.*`, never a local version.
- **Lint config is required:** `detekt-config.yml` and `.editorconfig` are read by SAGE's
  convention plugins — don't delete either.
- **Tests live beside their module:** converted KMP modules use `src/jvmTest` (JUnit 4, no JS
  test target); `:apps:server` and `:apps:jvm` use `src/test`; `:core:bitmaps` tests the seeded
  `specFor` value and fetcher routing in `src/jvmTest` (no Skia);
  `:core:nav` owns the pure shell policy tests and `:core:ui:shell` owns the tab/chrome tests in
  its `jvmTest` source set; the apps keep only host-local tests; `:features:*:screenshot` uses
  Paparazzi over committed LFS goldens. No emulator, device suite, or native code.
- **Screenshots snapshot hand-built state.** A `:features:<x>:screenshot` fixture is a `ListState`
  built from public model constructors (usually `FakeModelGenerator`) and rendered through
  `:core:ui:previews` — never collected from a ViewModel or repository, so the golden cannot drift
  with time, DI, or the demo delay. Text and theme come from the shared modules, not a per-module
  map. Add a state by adding a `@DevicePreviews` composable plus a matching `@Test`; verify with
  `./gradlew verifyPaparazziDebug`, re-record deliberately with `recordPaparazziDebug`, and review
  the diff with `scripts/dev/paparazzi-diff.sh`.
- **Generated bitmaps are placeholder art:** `:core:bitmaps` serves `@Preview`s and
  `generated://` Coil requests (seeded by the URL's scheme/authority/path; `?width=&height=`
  picks the size without changing the art). Don't treat them as real image content; add a
  network fetcher/decoder before pointing a request at an arbitrary URL.
- **Benchmarks measure, the scripts gate.** `:benchmark` (Android test APK) writes one
  `results-<axis>.json` per axis through `:testing:perfbench-core`; the shell scripts diff it
  against a local, never-committed baseline. UI scenarios must start through the app's deep link
  (`sagesample://<route>`) rather than clicking through navigation, and there is no audio axis or
  media corpus. `TraceSection` is real on Android (Perfetto) and no-op on desktop/JS.
- **Search:** use `git grep`/`rg`; when using `find`/`grep`, prune `build/` and `.gradle/`.
  Generated Android lint/dependency XML is enormous.

## Worktrees

- Create a build-ready worktree with `scripts/dev/worktree-add.sh <name> [base-ref]` (copies
  `local.properties`, reference-clones SAGE); remove it with
  `scripts/dev/worktree-rm.sh <dir>`.
- Never `git stash` in a multi-worktree run: the stash is shared across linked worktrees.
- Check a worktree before hand-off with `scripts/dev/check-worktree-hygiene.sh [dir]`.

## Build / verify

```sh
./gradlew :apps:android:assembleDebug     # Android build
./gradlew :apps:android:installDebug      # install on an attached device
./gradlew :apps:jvm:run                   # run the Compose Desktop app
./gradlew :apps:server:run -Psage.js=true # build the browser bundle and serve it on :8080
./gradlew -Psage.js=true :apps:js:jsBrowserDistribution :apps:server:installDist
./gradlew ktlintCheck detekt              # lint — run BOTH, they gate commit/push
./gradlew ktlintFormat                    # auto-fix ktlint findings
./gradlew verifyPaparazziDebug            # check the committed screenshot goldens
./gradlew recordPaparazziDebug            # re-record goldens (intentional UI change only)
scripts/dev/paparazzi-diff.sh             # rank changed goldens + write review montages
./gradlew :benchmark:compileBenchmarkKotlin          # compile the device benchmark APK
./gradlew :benchmark:connectedBenchmarkAndroidTest   # run both axes (physical device)
scripts/benchmark/device-ui.sh [--record]            # gate UI scroll against a local baseline
scripts/benchmark/device-startup.sh [--record]       # gate startup against a local baseline
scripts/ci/verify.sh                      # full CI-mirroring suite (web is opt-in)
scripts/ci/verify.sh --web                # ...plus the browser bundle + server distribution
```

- **Always run `scripts/ci/verify.sh` before any `git push`**, and only push when it reports
  `OVERALL: PASS`. Add `--web` when the change touches the browser app, the server or the
  repository split (the web suite is opt-in locally and runs as its own CI job). It mirrors
  `.github/workflows/ci.yml`, keeps going after a failure, and collates logs/reports/JUnit/APKs
  into `build/verification/` with a `summary.txt`.
  Scope it with `--list`, `--skip-apps`, `--rerun`, or task names.
- Heed its `WARNING:` lines: uncommitted changes or a SAGE commit that is not on a remote
  branch mean CI may not see what you tested.
- **CI mirrors the script; change them together.** CI has `static-analysis`, `unit-test`,
  `screenshot` (Paparazzi, LFS checkout), `android` (lint + debug/release APK), `jvm-app`
  (desktop jar), and `web` (Node 22 + Yarn 1.x, JS lint/bundle, server distribution) jobs, on
  Temurin 21 and SDK platform 36. Kotlin/JS uses the system Node/Yarn (`-Psage.js=true`); the root
  build disables the plugin's own download because `FAIL_ON_PROJECT_REPOS` rejects its project
  repositories.
- Gradle prints thousands of task lines: redirect a long run to a file and grep it
  (`./gradlew … > /tmp/build.log 2>&1`) rather than pulling them into context.
- The release APK is unsigned. Don't boot an AVD; stop after build + lint and hand device
  testing to the user.

## SAGE changes

`sage/` is a separate repository (`git@github.com:sigmabeta/sage.git`). Treat changes there as
needing explicit sign-off, and keep the two commits in order:

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

If a SAGE module is renamed, update the `libs.sage.*` alias used by the module that depends on
it (the catalog is SAGE's own `gradle/libs.versions.toml`).

## Push gate

`git push` requires all of the following — no exceptions for "tiny changes":

1. `scripts/ci/verify.sh` reports `OVERALL: PASS`.
2. The human has explicitly approved the push.
3. If the `sage` gitlink changed, the pinned commit is already on SAGE's remote.
   Verify with `git -C sage branch -r --contains "$(git rev-parse :sage)"`;
   otherwise CI's submodule checkout fails with `upload-pack: not our ref`.

Don't `git commit` or `git push` unless explicitly asked.

## Layout

See [`README.md § Layout`](README.md#layout) for the file-by-file map. Top level: `apps/android/`
(Android application), `apps/jvm/` (Compose Desktop application), `benchmark/` (Android device
macrobenchmarks), `core/bitmaps/` (KMP Compose preview art), `core/ui/components/api/` (KMP Compose
renderer suite), `core/ui/shell/` (KMP Compose composition root + navigation host), `core/settings/`
and `core/viewmodel/`, `features/*` (pure JVM), `testing/perfbench-core/` (result contract) and
`testing/perfbench/` (diff CLI), `scripts/benchmark/` (per-axis device gates), `scripts/ci/verify.sh`
(CI mirror), `scripts/dev/` (agent tooling), `docs/agents/` (domain docs), `sage/` (submodule).

## Agent skills

### Domain docs

Single-context: one `GLOSSARY.md` plus `docs/adr/` at the repo root. See
`docs/agents/domain.md`.
