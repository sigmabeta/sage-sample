# `:core:ui:previews`

Paparazzi / `@Preview` scaffolding: renders a screen's SAGE `ListState` through the real list
pipeline with no ViewModels or DI, so a snapshot is deterministic and meaningful.

```kotlin
plugins {
    alias(libs.plugins.sage.android)
    alias(libs.plugins.sage.compose.android)
}
```

It is Android-only because Paparazzi renders an Android window, and it is a **test-support leaf**:
only `:features:*:screenshot` modules depend on it, so the Paparazzi runtime never reaches either
shipped app.

## The main seam

`ScreenPreview.kt` exposes two composables:

| Composable | Use |
|---|---|
| `ListScreenPreview(screenState, darkTheme, syntheticWidthClass)` | A screen whose state renders through the generic list/grid pipeline. Calls `ListState.toActual` with the shared `SampleStringProvider`, then `ListScreen` or `GridScreen` exactly as the app's `SampleListScreen` would. |
| `ScreenPreview(darkTheme, content)` | A screen that owns its presentation (Search draws its own field over an adaptive grid). Gives `content` the shared `StringProvider` so it can turn a `ListStateActual` into items itself. |

Both install:

- `:core:ui:theme:api`'s `SampleTheme` (light/dark schemes + `sampleTypography`),
- `LocalInspectionMode = true`, so `CrossfadeImage` renders the shared `:core:bitmaps` generated
  art instead of trying (and failing) to fetch through Coil,
- `LocalSampleStringProvider` backed by `:core:strings:real`'s `SampleStringProvider`, so the
  preview and the running app resolve identical text.

`ListScreenPreview` also wraps the content in a VM-free `PreviewChrome`: a Material 3 top bar
showing the state's title. The nav bar/rail and the scrolling top bar are app-shell concerns and
are intentionally not snapshot.

## Other public surface

| File | Contents |
|---|---|
| `DevicePreviews.kt` | `@DevicePreviews`: light/dark × phone/foldable/tablet Compose previews for the IDE gutter. |
| `PreviewTestUtils.kt` | `INTERESTING_DEVICES` (six devices × light/dark), the parameterized-test name format, and `DeviceConfig.toWidthClass()` mapped onto SAGE's 600 dp / 840 dp breakpoints. |
| `fake/FakeModelGenerator.kt` | Deterministic `HomeItem` / `SampleItem` / `SearchResult` / `ContentDetail` generator seeded once per call, the sample's analogue of Chipbox's generator. |

## Using it

```kotlin
@DevicePreviews
@Composable
internal fun Home(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = homeContentState(),      // HomeState built with FakeModelGenerator
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}
```

Fixtures are hand-built states, never ViewModels collected from DI or repositories: the goldens
must not depend on time, the demo delay, or a repository seed. `FakeModelGenerator` supplies the
domain rows so a fixture does not hardcode names and captions.

## Module facts

- **Plugin:** `sage.android` + `sage.compose.android` (not the screenshot convention — this module
  uses Paparazzi's `DeviceConfig` but is not itself a test module).
- **Targets:** Android only.
- **Source set:** `src/main/java`.
- **Dependencies:** `:core:ui:components:api` (`api`, for the renderers and `ListModel.Content`),
  `:core:models:api` (generator types), `:core:strings:real` (shared text), `:core:ui:theme:api`
  (theme), SAGE `list`, `appcomm`, `ui.listScreens`, `ui.components`, `ui.strings`, Compose via the
  convention, and `paparazzi.runtime` for `DeviceConfig`/`NightMode`.
