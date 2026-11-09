# `:features:contentdetail:screenshot`

Paparazzi visual-regression companion for Content detail. The fixture is a hand-built
`ContentDetail`, so the golden does not depend on the generated repository or a particular ID's
seed.

```kotlin
plugins {
    id("sagesample.screenshot")
}

dependencies {
    implementation(projects.features.contentdetail)
}
```

## States

| Preview | Fixture |
|---|---|
| `ContentDetailLoaded` | `FakeModelGenerator().randomContentDetail()`: hero, CTAs, metadata, a five-item related scroller, and six image/name/caption part rows. |
| `ContentDetailLoading` | `LCE.Loading`, so each section renders its own loading shape. |
| `ContentDetailNotFound` | `LCE.Content(null)` — the explicit not-found explanation with the Back affordance. |
| `ContentDetailError` | `LCE.Error`, rendering the standard error row. |

The state declares `ColumnType.Staggered(320, false)`, so compact windows snapshot the single-column
list and wider devices snapshot the staggered grid — the same split `screenLayoutFor` makes in the
app.

## Files

| File | What it is |
|---|---|
| `src/main/java/.../ContentDetail.kt` | The four `@DevicePreviews` composables and the `ContentDetail` fixture. |
| `src/test/java/.../ContentDetailScreenshots.kt` | Parameterized Paparazzi test over `INTERESTING_DEVICES`. |
| `src/test/snapshots/images/` | The LFS goldens (4 states × 12 device/theme configs). |

## Module facts

- **Plugin:** `sagesample.screenshot` (sage.android + sage.compose.android + Paparazzi).
- **Targets:** Android only. **Source sets:** `src/main/java` (fixtures) + `src/test/java` (tests).
- **Dependencies:** `:features:contentdetail`; the convention adds `:core:ui:previews`,
  `:core:strings:real`, SAGE `appcomm`/`list`.
- **Text/theme:** both come from the shared modules — `SampleStringProvider` and `SampleTheme`.
