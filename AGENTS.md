# AGENTS.md — SAGE Sample orientation

Orientation for coding agents. **Architecture lives in [`README.md`](README.md)** — its
"How the wiring works", "Screens", "Navigation" and "Layout" sections. This file is the
agent-only guardrails and pointers; prefer a pointer over repeating the README.

## What this repo is

The canonical reference for building an app on [SAGE](sage/): one Android app (`:app`), one
Compose Desktop app (`:apps:jvm`), four pure-JVM feature modules over `:core:viewmodel`, and SAGE
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

- **DI is Metro** (`dev.zacsweers.metro`), not Hilt/Dagger. A screen contributes
  `@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())` + `@ViewModelKey` and
  resolves with `metroViewModel<...>()`. Do not add per-screen graph accessors and do not go
  back to SAGE's `ListViewModelBrain`/`BrainProvider`.
- **A screen ViewModel lives in its Voyager screen's store**, not the app graph: no `@SingleIn`
  on a screen VM. Popping and re-pushing builds a fresh one.
- **Features emit `SageEvent`s; the app owns navigation.** `SampleNavEffects` →
  `navActionFor` decides `ShowTopLevel` (reset to `[Home, target]`), `Push` (Settings), or
  `Pop`. Don't navigate from a feature module.
- **Chrome comes from the active screen's `ListStateActual.title`** (`title` +
  `shouldShowBack`), with the destination as the pre-report fallback. Navigation-tab labels and
  icons stay app-owned.
- **Layout is derived:** `screenLayoutFor(columnType, widthClass)` keeps `ListScreen` at one or
  zero columns and uses `GridScreen` (staggered when the column type asks) above that.
- **One foreground pair:** the shared screen sends `Resume`/`Pause`; SAGE's list screens also
  send `Resume` on title change, coalesced in `SampleListViewModel.sendAction`.
- **Text never touches `R.string` in a feature.** Use a `SageStringId` enum + `StringProvider`;
  `SampleBindings` (Android) and `SampleStringProvider` (desktop) are the only id → text mappings.
  Demo/repository content included.
- **Module plugins:** `:features:*` and `:core:viewmodel` apply `sage.jvm` (features also
  `sage.di`); `:apps:jvm` applies `sage.jvm` + `sage.di` + Compose Desktop (the Compose compiler
  and JetBrains Compose plugins); `:app` applies `android.application`, Compose, `metro`, `detekt`,
  and `ktlint` directly. Add SAGE dependencies through `libs.sage.*`, never a local version.
- **Lint config is required:** `detekt-config.yml` and `.editorconfig` are read by SAGE's
  convention plugins — don't delete either.
- **Tests live beside their module:** feature/core and `:apps:jvm` are pure JVM (JUnit); `:app`
  uses Android unit tests for app-owned logic. No emulator, device suite, Paparazzi, or native
  code.
- **Search:** use `git grep`/`rg`; when using `find`/`grep`, prune `build/` and `.gradle/`.
  Generated Android lint/dependency XML is enormous.

## Worktrees and specs

- Create a build-ready worktree with `scripts/dev/worktree-add.sh <name> [base-ref]` (copies
  `local.properties`, reference-clones SAGE); remove it with
  `scripts/dev/worktree-rm.sh <dir>`.
- Never `git stash` in a multi-worktree run: the stash is shared across linked worktrees.
- Specs and tickets live under `.scratch/<feature>/` (gitignored). Gate closure with
  `scripts/dev/check-ticket-gates.sh --feature <slug>`; check a worktree before hand-off with
  `scripts/dev/check-worktree-hygiene.sh [dir]`.

## Build / verify

```sh
./gradlew :app:assembleDebug              # Android build
./gradlew :app:installDebug               # install on an attached device
./gradlew :apps:jvm:run                   # run the Compose Desktop app
./gradlew ktlintCheck detekt              # lint — run BOTH, they gate commit/push
./gradlew ktlintFormat                    # auto-fix ktlint findings
scripts/ci/verify.sh                      # full CI-mirroring suite
```

- **Always run `scripts/ci/verify.sh` before any `git push`**, and only push when it reports
  `OVERALL: PASS`. It mirrors `.github/workflows/ci.yml`, keeps going after a failure, and
  collates logs/reports/JUnit/APKs into `build/verification/` with a `summary.txt`.
  Scope it with `--list`, `--skip-apps`, `--rerun`, or task names.
- Heed its `WARNING:` lines: uncommitted changes or a SAGE commit that is not on a remote
  branch mean CI may not see what you tested.
- **CI mirrors the script; change them together.** CI has `static-analysis`, `unit-test`,
  `android` (lint + debug/release APK), and `jvm-app` (desktop jar) jobs, on Temurin 21 and SDK
  platform 36.
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

See [`README.md § Layout`](README.md#layout) for the file-by-file map. Top level: `app/`
(Android application), `apps/jvm/` (Compose Desktop application), `core/viewmodel/`,
`features/*` (pure JVM), `scripts/ci/verify.sh` (CI mirror), `scripts/dev/` (agent tooling),
`docs/agents/` (tracker/domain docs), `sage/` (submodule).

## Agent skills

### Issue tracker

Issues and specs live as markdown files under `.scratch/<feature-slug>/`; triage state is a
`Status:` line. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, default strings: `needs-triage`, `needs-info`, `ready-for-agent`,
`ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` plus `docs/adr/` at the repo root. See
`docs/agents/domain.md`.
