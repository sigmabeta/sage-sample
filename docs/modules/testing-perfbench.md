# `:testing:perfbench`

The host-side result-diff CLI and the companion to the Android `:benchmark` module: the device
measures, this tool compares. It is a test-system module run through an explicit `run` task —
nothing here is shipped.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.kotlin.serialization)
}
```

## `diff --a <baseline> --b <current> [--json]`

Run A is the baseline, run B the current run; each argument is a `results-<axis>.json` file. For
every scenario measured by both runs it prints that axis's primary cost metric (UI frame p50,
startup time-to-first-content p50) and the ratio — baseline ÷ current, so **above 1 means B is
better** — plus the **geometric mean**, per axis and overall.

```sh
./gradlew :testing:perfbench:run --args="diff --a build/perfbench-baseline/device/pixel-8-arm64-v8a --b build/perfbench-device/ui/current/pixel-8-arm64-v8a"
```

`--json` emits the same report as a machine-readable document (the shared perfbench JSON dialect)
instead of the human table, so a caller can read the ratios rather than parse prose:

```sh
./gradlew :testing:perfbench:run --args="diff --a <baseline> --b <current> --json" \
  | jq -r '.geometric_mean'
```

The diff **refuses** rather than report a number when the two runs are not comparable: a different
**scenario set** (e.g. a scenario was added since the baseline). The refusal names what differed.
A refusal prints prose and **no JSON document**, so a consumer must treat the absence of JSON as
"not comparable"; `scripts/benchmark/device-common.sh` does exactly that and flags the run instead of
failing it. Re-record the baseline to compare again.

## Tests

`src/test` covers the `diff` argument parser (`DiffArgsTest`) and the result-read seam's refusal of an
unreadable file (`DiffCommandTest`). The diff math itself is tested in
`:testing:perfbench-core`.

## Module facts

- **Targets:** JVM.
- **Source set:** `src/main/java`.
- **Dependencies:** `testing:perfbench-core`; JUnit 4 in `src/test`.
