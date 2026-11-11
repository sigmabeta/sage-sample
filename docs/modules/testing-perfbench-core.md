# `:testing:perfbench-core`

The shared performance-result contract: the serialized schema, scenario ids, device fingerprint,
pure report math, diff guards, and JSON dialect. It is contract-first — no device, no I/O — so the
whole contract is unit-tested without a phone.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.kotlin.serialization)
}
```

KMP (`android` + `jvm`) because the Android `:benchmark` module and the JVM diff CLI both need a
variant. It has no JS target.

## Public surface

| Area | Types |
|---|---|
| Result document | `PerfResult`, `PerfConfig`, sealed `Axis` (`UiAxis` + `UiResult`, `StartupAxis` + `StartupResult`) |
| Scenarios | `UiScenario` (`ui.home_scroll`, …), `StartupScenario` (`startup.cold`, `startup.warm`) |
| Fingerprint | `DeviceFingerprint`, `DeviceEnvironment`, `deviceFingerprint(...)` |
| Report math | `uiResult`, `startupResult`, `percentile`, `jankPercent` |
| Diff | `compareRuns`, `DiffOutcome`, `DiffRefusal`, `DiffReport`, `AxisComparison`, `ScenarioComparison`, `geometricMean` |
| Artifacts | `axisResultFileName(axis)`, `RESULTS_FILE_NAME` |
| Serialization | `PerfbenchJson` |

The diff's primary metrics are UI frame p50 and startup time-to-first-content p50; ratios are
baseline ÷ current, so above one means the current run is better.

## Contents

| file | role |
|---|---|
| `PerfResult.kt` | the `results-<axis>.json` schema: `PerfResult`, `PerfConfig`, and the sealed `Axis` hierarchy |
| `Fingerprint.kt` | `DeviceFingerprint` + the pure `deviceFingerprint(DeviceEnvironment)` builder |
| `UiScenario.kt` | the canonical UI scenario ids |
| `StartupScenario.kt` | the canonical startup scenario ids |
| `UiReport.kt` | pure report math: `uiResult(UiMeasurement)` plus `percentile` and `jankPercent` |
| `StartupReport.kt` | pure report math for the startup axis: `startupResult(StartupMeasurement)` |
| `Diff.kt` | the baseline-diff contract and `geometricMean` |
| `PerfbenchJson.kt` | the one JSON dialect (`class` discriminator, pretty-printed, defaults encoded) |
| `ResultArtifact.kt` | `RESULTS_FILE_NAME` and `axisResultFileName(axis)` |

## Using it

```kotlin
// Reduce a device scroll's pooled frames + trace sums to one UI scenario result.
val ui = uiResult(
    UiMeasurement(
        scenario = UiScenario.HOME_SCROLL.id,
        frameCpuMillis = listOf(8.0, 9.5, 12.0, 7.0),
        frameOverrunMillis = listOf(-2.0, 1.0, -3.0, -1.0),
        traceSections = listOf(TraceSectionMeasurement("GridImage", sumMs = 120.0, count = 240)),
    ),
)

// Reduce a device startup's per-launch times to one scenario result.
val startup = startupResult(
    StartupMeasurement(
        scenario = StartupScenario.COLD.id,
        timeToFirstContentMillis = listOf(320.0, 305.0, 412.0, 298.0, 331.0),
    ),
)

// Compare every axis both runs share; a refusal means they aren't comparable (scenario set).
when (val outcome = compareRuns(baseline, current)) {
    is DiffOutcome.Compared -> outcome.report.axes.forEach { println("${it.axis}: ${it.geometricMean}") }
    is DiffOutcome.Refused -> println(outcome.reason.message)
}
```

`kotlinx-serialization-json` is an `api` dependency because the schema types carry generated
serializers that any consumer needs resolvable. `kotlinx-coroutines-core` is also `api` only so
SAGE's convention opt-in marker resolves on this module's compile classpath.

## Tests

`src/jvmTest` covers the report math (`UiReportTest`, `StartupReportTest`), the scenario ids, the
fingerprint builder and artifact names (`FingerprintTest`, `ResultArtifactTest`), the schema
round-trips and snake_case keys (`PerfResultSerializationTest`), and the diff contract —
comparability refusal, per-axis ratios, and geometric means (`DiffTest`).

## Module facts

- **Targets:** Android + JVM.
- **Source sets:** `commonMain` (the whole contract), `jvmTest`.
- **Dependencies:** `kotlinx-serialization-json` and `kotlinx-coroutines-core` (`api`); JUnit 4 in
  `jvmTest`.
