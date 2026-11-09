# `:features:listitems:screenshot`

Paparazzi visual-regression companion for the three List Items screens. All three states consume the
same deterministic `SampleItem` catalogue, so the goldens show the text rows, the image rows, and the
adaptive grid over identical content.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.listitems)
}
```

## States

Each screen has four fixtures (loaded / loading / empty / error):

| Test class | States |
|---|---|
| `ListItemsScreenshots` | `ListItems`, `ListItemsLoading`, `ListItemsEmpty`, `ListItemsError` — text/caption rows. |
| `ListItemsWithImagesScreenshots` | `ListItemsWithImages`, `…Loading`, `…Empty`, `…Error` — image/name/caption rows with `generated://` art. |
| `GridOfImagesScreenshots` | `GridOfImages`, `…Loading`, `…Empty`, `…Error` — `ColumnType.Regular(160)`, so the phone snapshots two columns and wider devices add more. |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../ListItemsFixtures.kt` | `FakeModelGenerator().randomSampleItems()` plus the four `LCE` fixtures. |
| `src/main/java/.../ListItems.kt` | The text-row previews. |
| `src/main/java/.../ListItemsWithImages.kt` | The image-row previews. |
| `src/main/java/.../GridOfImages.kt` | The grid previews. |
| `src/test/java/.../` | One parameterized Paparazzi class per screen. |
| `src/test/snapshots/images/` | The LFS goldens (3 screens × 4 states × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:listitems`; the convention adds `:core:ui:previews`,
  `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
