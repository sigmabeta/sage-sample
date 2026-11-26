# `:benchmark`

> Macrobenchmark test module — measures `apps/android`'s core-screen scroll performance and app
> startup on a real device, writing one perfbench result per axis.

An Android Test (`com.android.test`) module that macrobenchmarks the installed `:apps:android` app.
It instruments the real APK from outside (self-instrumenting, `targetProjectPath = :apps:android`):
one test scrolls each core surface and records frame timing + the Compose trace sections each screen
emits (`ui` axis); another measures cold and warm startup up to first content (`startup` axis). Each
assembles a perfbench result (device fingerprint + axis) with
`:testing:perfbench-core`. It is a leaf — nothing depends on it.

There is deliberately **no audio axis and no corpus**: the sample's demo content is deterministic,
so a benchmark only needs the app and a device.

```kotlin
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}
```

## Deep-link navigation

The sample already deep-links every screen (`sagesample://browse/list-items`), so each UI scenario
starts on the surface it measures instead of driving the navigation UI. The route is the same one a
user would follow, and the setup runs outside the measured block, so navigation adds no frames to
the result. `DeviceBenchmark` builds the URI from `:core:nav`'s `SampleDeepLink.SCHEME`, so the
benchmark and the app's manifest cannot drift.

The launch itself is a shell `am start -W -f 0x10008000 …` rather than
`MacrobenchmarkScope.startActivityAndWait`: the latter additionally polls `dumpsys gfxinfo` for a
new frame, and on the OnePlus 7 Pro (API 30) that poll never sees one and throws "Unable to confirm
activity launch completion" even though the app is up. `-W` still waits for the start to be accepted,
and the scenario's own landing wait (title/content plus the screen's scrollable) is the completion
signal.

## Signed in

The app's Sign-in gate would show Login to a fresh install, so every launch passes the
benchmark-build-only extra `sagesample.benchmark.signIn=demo` (an `am start --es` for deep links,
an intent extra for the startup launch). The startup axis also signs in once with
`DeviceBenchmark.signInOnce` before its measured loop: the first sign-in on an install runs PBKDF2,
which must not land inside a measured launch. Measured launches then find the stored Session at the
gate's startup check.

## What this builds / how to run it

- **Target:** a Macrobenchmark instrumentation test against `net.sigmabeta.sage.sample`, built into
  the `benchmark` build variant (matching `:apps:android`'s `benchmark` variant: `release`-based,
  non-debuggable, profileable).
- **Run:** `./gradlew :benchmark:connectedBenchmarkAndroidTest` on a **physical device** — numbers
  aren't representative on an emulator (`androidx.benchmark.suppressErrors = EMULATOR` lets it run
  there anyway; remove that when measuring for real).
- **Per-axis results:** each test method writes its own artifact to the run's additional test output
  dir, named for the axis it measured — `results-ui.json` and `results-startup.json` — so no axis
  overwrites another. They're pulled to
  `benchmark/build/outputs/connected_android_test_additional_output/.../` alongside the
  per-iteration Perfetto traces (which open directly in <https://ui.perfetto.dev>). Each file carries
  the axis the test measured plus the device fingerprint (manufacturer/model, Android build, ABI,
  thermal status at start, and the app version).

## Deterministic data

The sample's demo repositories return deterministic content immediately, so there is nothing to scan
or push. Benchmarks assume the app's **launch defaults**: the stable Home plan, no simulated delay,
and the content demo outcome. If you have changed the demo outcome, Home plan, or delay in Settings,
reset them before measuring (Settings → Reset demo data), or the scenario may have no content or a
different frame budget.

Home's big-image hero is picked by the day. The benchmark build binds a fixed `HomeDay` (see
`SampleBindings`), so every night's Home shows the same hero as the run its baseline came from;
every other build follows the clock.

The scenarios drive portrait layouts, so a run needs **auto-rotate off**. `device-<axis>.sh` (and
the device farm) turn it off and pin the natural rotation before the instrumentation starts, and
nothing turns it back on. Each benchmark's `setupBlock` sets the same lock again, which holds it
during a bare Gradle run, but that lock can't outlast the test: UiAutomation restores the rotation
state it saw at connect when it disconnects. Before a bare Gradle run on a phone that may have
auto-rotate on, lock it first:

```sh
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
```

The benchmark build shares the app's package name and data. Installing it for a run replaces a
debug/release install, and its persisted settings survive; that is intentional, but do not run it on
a phone whose settings you need to keep.

## Progress logging

A device run is otherwise a black box, so each axis logs begin/end and per-scenario sections with
the `SageBench` tag:

```sh
adb logcat -s SageBench
```

Each test emits a `begin test=<name>` / `end test=<name> ms=<millis>` pair (the end is emitted from a
`finally`); inside a test, each scenario is a `section=<id>` begin/end pair with its wall duration.
A ~15-minute scenario run is otherwise silent between `TestRunner: started` and
`TestRunner: finished`.

## Gating a regression (`scripts/benchmark/device-<axis>.sh`)

The module itself is a measurement; the `scripts/benchmark/device-<axis>.sh` scripts are the local
**gate** built on top of it — **one script per axis** (`ui`, `startup`), each the matching
`device-<axis>` task in `scripts/ci/verify.sh` (selected with `--device-ui` / `--device-startup`).

Each runs on **every attached device** (AGP fans out, in the reused Gradle daemon), then diffs each
device's `results-<axis>.json` against that device's **locally-recorded** baseline with
`perfbench diff --json` (ratio = baseline ÷ current; below the threshold — default `0.95` — is a
regression). The scripts need `adb` and `jq` on `PATH`. It fails if any one device regresses. The
baseline is never committed; the first run (or `--record`) writes it under
`build/perfbench-baseline/device/<model>-<abi>/results-<axis>.json`, keyed by the device's hardware
identity so a new device records a fresh baseline rather than gating one device's numbers against
another's.

`--record` measures `--record-runs` passes (default 3) and records, per scenario, the pass whose gate
metric is the median (`perfbench baseline`), keeping the passes under `<baseline>/records/`. One
recording is a single draw from a noisy distribution, and a baseline that happens to be a fast draw
makes every later run look regressed. A first run with no baseline records its single pass and
flags it.

Outputs land under `build/perfbench-device/<axis>/current/<model>-<abi>/`: the run's
`results-<axis>.json`, and — only when that device was actually compared against its baseline — the
`perfbench diff --json` report as `diff-<axis>.json` beside it. A recording run, a failed diff, or a
refused compare (scenario drift) leaves no `diff-<axis>.json`, so a consumer such as sagefarm's
publisher can treat the file's presence as "this result was compared".

A phone attached over two transports at once (mDNS *and* IP) would be run twice and collide, so the
scripts group the online serials by `ro.serialno` and **skip** rather than produce the opaque crash.

```sh
scripts/benchmark/device-ui.sh                    # gate
scripts/benchmark/device-ui.sh --record           # (re)record the baseline: median of 3 passes
scripts/benchmark/device-ui.sh --record --record-runs 5
scripts/benchmark/device-startup.sh --threshold 0.97
scripts/ci/verify.sh --device-ui                  # the same run inside verify.sh
```

Each exits `0` on pass (or when it records a baseline), `3` when it **skips** (no device attached, or
a device attached over two transports), and `1` when its test fails or a device regresses. `verify.sh`
maps `3` to a flagged `SKIP`, so a machine with no phone attached still passes verification.

**Why one axis per process.** Each axis is independent, so a failing or slow one must not take the
other with it — the script runs only that axis's test class. It also keeps the result safe if a
non-macrobenchmark test is ever added to the module: the benchmark library's `Outputs` singleton
wipes the additional-test-output directory the first time a macrobenchmark initialises it, so a
result written by a plain instrumentation test earlier in that process would be gone. (A bare
`connectedBenchmarkAndroidTest` runs both axes; both are macrobenchmarks writing after their own
measurement, so both files survive.)

## Running & debugging on a device

The suite fans out to **every** connected device. To run one device, set `ANDROID_SERIAL` and pass
`--no-daemon`; adb serials can contain spaces and a `(2)` suffix, so quote them.

```sh
ANDROID_SERIAL='<adb serial>' ./gradlew --no-daemon :benchmark:connectedBenchmarkAndroidTest
```

Run a single test class with the instrumentation class filter:

```sh
ANDROID_SERIAL='<adb serial>' ./gradlew --no-daemon :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.sigmabeta.sage.sample.benchmark.UiScrollBenchmark
```

Run a single UI scenario — the fast way to tune one device recipe without paying for the whole suite
(the `scenario` argument takes a `UiScenario` id, e.g. `ui.grid_of_images_scroll`):

```sh
ANDROID_SERIAL='<adb serial>' ./gradlew --no-daemon :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.sigmabeta.sage.sample.benchmark.UiScrollBenchmark \
  -Pandroid.testInstrumentationRunnerArguments.scenario=ui.grid_of_images_scroll
```

**A device can appear twice in `adb devices`** — once by mDNS and once by IP. Gradle then fans out
to both serials and runs two instrumentation sessions on the one phone, which collides and reports a
misleading `Process crashed` / `DELETE_FAILED_INTERNAL_ERROR`. Pin `ANDROID_SERIAL` to one serial, or
`adb disconnect` the duplicate, before running.

When a run fails, **`adb logcat` is the ground truth** — a macrobenchmark failure looks opaque from
Gradle, but logcat shows the cause: the compilation-mode reset, a profile-install broadcast the app
never answered, or an exception in the app's `Application.onCreate`. Each test's result, its
`additionaltestoutput.benchmark.message.*.txt`, and the per-iteration Perfetto traces land under
`benchmark/build/outputs/connected_android_test_additional_output/<device>/`. A scenario that fails
to reach its landing screen also writes `failure-<scenario>.xml` (the window hierarchy) and
`failure-<scenario>.png` (a screenshot) there, so the screen it actually showed is recoverable
without re-running on the device.

## UI scenarios

`UiScrollBenchmark` measures the **ui** axis: one WARM-startup scroll per core surface, all written
into a **single** `results-ui.json` so no scenario overwrites another. Each scenario carries its own
deep link, landing signal, gesture, and trace-section set:

| Scenario | Deep link | Trace sections | Gesture |
|---|---|---|---|
| `ui.home_scroll` | `home` | `GridImage`, `CrossfadeImage` | A vertical stroke reveals the nested image row, 3 strokes across it and back, then back to the top. |
| `ui.list_items_scroll` | `browse/list-items` | none (text-only rows) | 3 bounded strokes down and back. |
| `ui.grid_of_images_scroll` | `browse/grid-of-images` | `GridImage`, `CrossfadeImage` | 3 bounded strokes down and back. |
| `ui.content_detail_scroll` | `home/content-detail/grid-of-images:7` | `CrossfadeImage` | 3 bounded strokes down and back so the hero and item images leave and re-enter composition. |

`browse/list-items-with-images` is deliberately not a scenario: its first page is 10 rows, which
fits a phone viewport, so the screen has nothing to scroll and every measurement would be idle.
Home's horizontal row and Content detail cover the image-list surface.

Every scenario is a **fixed workload**: the same strokes from the same offset on every pass. A
stroke is a `UiObject2.scroll` of a set share of the scroller at 1200 dp/s, which holds still before
lifting, so the release has no velocity and nothing flings. The stroke back is longer than the
stroke out (60% then 80% vertically, 70% then 90% across Home's row), so it stops against the
list's start edge, the same place every cycle; equal strokes are not quite symmetric. Each cycle
must end where it started (the scroller shows the same texts at the same offsets), or the run fails
rather than record a different workload. Strokes keep a margin from the screen edges, where the
system gesture zones are. One unmeasured cycle in the
`setupBlock` loads the screen's images first, so every measured iteration starts from the same
cache state.

The scenarios used to stroke with `device.drag`, which moves in a few steps and lifts at full
speed: the list flung, the next drag caught it at a random offset, and one commit's frame counts
varied by up to 45% between nightlies. Flings are no better on these short screens: a fling waits
for accessibility scroll events, so at the end of a screen it times out (10 s per attempt) without
moving the list. Home additionally needs its horizontal row exercised: its two-row stable plan is
already fully composed after landing, so a vertical-only gesture records no image sections.

`ui.content_detail_scroll` uses a known generated Content ID rather than whichever item was opened
last, so the route is stable. The symmetric stroke matters on a long page: a gesture that only heads
toward the end never returns to the top, so the hero stays disposed and its trace reads zero.

## Device-driver gotchas

Each of these cost a device run to find; they are the traps in the `androidx.test.uiautomator`
driver the scenarios use:

- **`UiDevice.executeShellCommand` does not strip shell quotes.** A quoted `-d "sagesample://…"`
  arrives at `am` with the quotes as part of the data and the intent fails to resolve
  (`unable to resolve Intent … dat="sagesample://…"`). Pass an unquoted URI; a route has no shell
  metacharacters.
- **`macrobenchmark`'s `startActivityAndWait` can fail after the app is up.** It also polls
  `dumpsys gfxinfo` for a new frame, and the OnePlus 7 Pro on API 30 never reports one, throwing
  "Unable to confirm activity launch completion". The UI setup uses a shell `am start -W` and the
  scenario's own landing wait instead.
- **`fling` waits for accessibility scroll events.** At the end of a screen (or when the list cannot
  move) it times out rather than no-op'ing, so a fling-based gesture can spend seconds idle and
  compose nothing.
- **`device.drag` flings.** It moves in a few steps and lifts at full speed, so the list keeps moving
  after the gesture and the next one starts from a random offset. The scenarios use
  `UiObject2.scroll` with a speed, which holds still before lifting.
- **`Direction` means different things per call.** `UiObject2.scroll` and `fling` name where the
  *content* goes: `DOWN` moves toward the end. `swipe` names where the *finger* goes: `UP` moves
  toward the end. Getting it backwards is a silent no-op at the top.
- A trace section is recorded only when its composable **enters composition**. To measure a page's
  images, scroll a bounded distance toward the end and back so they leave and re-enter.
- Tune **one scenario at a time** with `-P…scenario=<id>`: the full suite resets compilation per
  scenario (~15–20 min).

## Startup scenarios

`StartupBenchmark` measures app startup in both a **cold** launch (no process) and a **warm** launch
(a new Activity in a live process), reporting time-to-first-content percentiles in the `startup`
axis. "First content" is Home showing its first content-bearing image: in the `benchmark` build type
the app calls `Activity.reportFullyDrawn()` the first time a `CrossfadeImage` composes (via
`LocalFirstContentReporter`), which `StartupTimingMetric` records as `timeToFullDisplayMs`.

## Trace sections

`GridImage` and `CrossfadeImage` each open a Perfetto/systrace section around their composition (via
`TraceSection` in `:core:ui:components:api`), so `TraceSectionMetric`s can sum the labelled work next
to frame timing. The Android `TraceSection` actual delegates to `androidx.tracing.Trace`; desktop and
the browser are no-ops.

## Contents

| File | What it is |
| --- | --- |
| `DeviceBenchmark.kt` | Shared device plumbing: target package, iteration count, device fingerprint facts, the deep-link intent builder, the per-axis result sink, and the `SageBench`-tagged progress logging. |
| `UiScrollBenchmark.kt` | `scrollScenarios` — WARM-startup benchmark that deep-links to each surface, scrolls its vertical list, then maps every scenario into one perfbench `results-ui.json`. A `scenario` instrumentation argument narrows it to one scenario. |
| `StartupBenchmark.kt` | `coldAndWarmStartup` — measures cold and warm launches up to first content and reports time-to-first-content percentiles, written to `results-startup.json`. |

## Module facts

- **Targets:** Android only (instrumentation test, `minSdk 26` / `targetSdk 36`).
- **Source set:** `src/main/java`.
- **Dependencies:** `:testing:perfbench-core` (the result schema / math) and `:core:nav`
  (`SampleDeepLink.SCHEME`). Test libs: `androidx.test.ext.junit`,
  `androidx.test.runner`, `androidx.test.uiautomator`, `androidx.benchmark.macro.junit4`.
- **Not a SAGE convention module:** it applies ktlint/detekt itself so its Kotlin is linted.
