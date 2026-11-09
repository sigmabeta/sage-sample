# `:features:browse:screenshot`

Paparazzi visual-regression companion for Browse. Browse's state is a `data object`, so the fixture
is the screen itself — there is nothing to build or seed.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.browse)
}
```

## States

| Preview | Fixture |
|---|---|
| `Browse` | `BrowseState`: the four static destination rows (List Items, List Items with Images, Grid of Images, Content detail). |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../Browse.kt` | The `@DevicePreviews` composable wrapping `BrowseState` in `ListScreenPreview`. |
| `src/test/java/.../BrowseScreenshots.kt` | Parameterized Paparazzi test over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (1 state × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:browse`; the convention adds `:core:ui:previews`, `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
