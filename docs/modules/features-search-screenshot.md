# `:features:search:screenshot`

Paparazzi visual-regression companion for Search. Search owns its chrome (the in-screen field
replaces the app top bar), so these previews render through `ScreenPreview` and the app's
`SearchContent` rather than the generic list pipeline.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.search)
}
```

## States

| Preview | Fixture |
|---|---|
| `SearchResting` | No submitted query, three history rows; below the five-row threshold, so the resting prompt follows them. |
| `SearchResults` | A settled `sample` query with four list rows and four grid cells, both from `FakeModelGenerator().randomSearchResults(...)`. |
| `SearchLoading` | Both result slots are `LCE.Loading`, so each section renders its own placeholders. |
| `SearchNoResults` | Both slots are `LCE.Content(emptyList())`, producing the localized "no results" empty state. |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../Search.kt` | The four `@DevicePreviews` composables, the `SearchState` fixtures, and the private `SearchContent` wrapper. |
| `src/test/java/.../SearchScreenshots.kt` | Parameterized Paparazzi test over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (4 states × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:search`; the convention adds `:core:ui:previews`,
  `:core:strings:real`, SAGE `appcomm`/`list`. `SearchContent` arrives through
  `:core:ui:previews`' `api` dependency on the renderer suite.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
