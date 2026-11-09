# `:features:home:screenshot`

Paparazzi visual-regression companion for Home. It snapshots `HomeState` fixtures through
`:core:ui:previews`, so the goldens pin the SAGE list pipeline plus the sample's renderers without
touching DI, repositories, the daily seed, or the demo delay.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.home)
}
```

## States

| Preview | Fixture |
|---|---|
| `Home` | Two settled sections built from `FakeModelGenerator`: a single full-width hero (`maxWidthDp = 400`, 3:4) and a four-item horizontal scroller. |
| `HomeLoading` | Two sections with `LCE.Loading` and `hasEmitted = true`, so the per-row loading rows render instead of the empty state. |
| `HomeEmpty` | `HomeState()` with no sections; every slot has resolved to nothing, so the screen-level `HOME_EMPTY` row renders. |
| `HomeError` | One section with `LCE.Error`; all sections have emitted and failed, so the screen-level `HOME_ALL_ERROR` row renders. |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../Home.kt` | The four `@DevicePreviews` composables and their hand-built `HomeState` fixtures. Section items come from `FakeModelGenerator().randomHomeItems(...)`; the shared generator turns their `screenshot://home/…` seeds into stable art. |
| `src/test/java/.../HomeScreenshots.kt` | Parameterized Paparazzi test: one `@Test` per fixture, repeated over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (4 states × 12 device/theme configs). |

## Using it

```sh
./gradlew :features:home:screenshot:verifyPaparazziDebug   # check (does not record)
./gradlew :features:home:screenshot:recordPaparazziDebug   # re-record
scripts/dev/paparazzi-diff.sh :features:home:screenshot:recordPaparazziDebug  # review diffs
```

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:home`; the convention adds `:core:ui:previews`, `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme` — so the app and the golden cannot drift.
