# `:features:settings:screenshot`

Paparazzi visual-regression companion for Settings. The fixture pins the About values that the app
normally reads from `BuildConfig`, so the goldens do not change on every build.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.settings)
}
```

## States

| Preview | Fixture |
|---|---|
| `Settings` | Theme/font/simulation/demo rows, the same-content toggle, reset row, and About values (`0.1.0`, `Nov 7, 2026`). |
| `SettingsExpandedDropdown` | The Theme dropdown expanded, showing Light / Dark / System and the selected marker. |

## Files

| File | What it is |
|---|---|
| `src/main/java/.../Settings.kt` | The two `@DevicePreviews` composables and the `SettingsState` fixture. |
| `src/test/java/.../SettingsScreenshots.kt` | Parameterized Paparazzi test over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (2 states × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:settings`; the convention adds `:core:ui:previews`,
  `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
