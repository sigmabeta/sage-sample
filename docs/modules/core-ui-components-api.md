# `:core:ui:components:api`

The shared Compose renderer suite, ported from Chipbox's `:cbox:common:ui:components:api`. Both apps
supply one `ListModel.Content` implementation to SAGE's `ListScreen`/`GridScreen`, so every SAGE
`ListModel` subtype renders the same way on Android and desktop.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.compose.kmp)
}
```

It is multiplatform (`commonMain`, `androidMain`, `jvmMain`) because its Compose code is shared.
`sage.kmp` also creates a `jvmSharedMain` intermediate source set from `src/main/java` for code the
Android and JVM targets share; `PlatformImageModel.jvmShared.kt` lives there.

## The main seam

`ComposableMapping.kt` defines:

```kotlin
@Composable
fun ListModel.Content(sink: ActionSink, debug: Boolean, mod: Modifier, pad: PaddingValues)
```

It dispatches each non-Chipbox-specific SAGE model to a renderer:

- rows — `NameCaption`, `ImageNameCaption`, `IconNameCaption`, `WideItem`, `GridImage`, …
- chrome — `SectionHeader`, `SubsectionHeader`, `Section`, `Subsection`, `SmallText`
- states — `EmptyStateListModel` / `ErrorStateListModel` (via `EmptyListIndicator`), loading items
- wrappers — `HorizontalScroller`, `DraggableListModel`, `DismissibleListModel` (swipe to remove)
- settings — `DropdownSettingListModel`, `CheckableListModel`, `LabelValueListModel`, …
- search — `SearchHistoryListModel`, `SearchResultListModel`
- unknown models fall through to a `Spacer`, so a feature can add a model without breaking the app

Each item is tagged with its model type for UI tests and wrapped in SAGE's perf measurement
(`WithMeasurementComponent`).

`sage.common.ui.components` and `sage.common.ui.strings` are re-exported with `api`, so consumers get
SAGE's `ListModel` vocabulary and this module's renderers from one dependency.

## Other public surface

| Area | Contents |
|---|---|
| `SearchContent.kt` | Chipbox-style search: one adaptive `LazyVerticalGrid` with the field overlaid and a status-bar scrim. Full-width rows/headers span the grid; grid cells keep one lane. |
| `SearchField.kt` | The search box chrome: back action, clear action, focus request, constant 48 dp control height. Not a `ListModel`; the caller owns the text and passes semantics strings. |
| `SampleTypography.kt` | `sampleTypography(fontFamily)`: Material 3 type scale rendered in the chosen family. |
| `ComponentStringId.kt` | Accessibility labels owned by the renderers (`ACCY_OCL_*`, `ACCY_ST_DESC_*`). |
| `LocalComponentStringProvider.kt` | Composition-local `StringProvider` plus `SageStringId.text(...)` helpers. Both apps install their graph's provider. |
| `subs/` | Drawing/effect building blocks: `CrossfadeImage` (Coil + crossfade + generated-art fallback), `FakeImage`, `Flasher`, `TintedText`, `TintedIcon`, `Rating`, `Elevated*`, `GridCellOffscreenLayerEnabled`, `PlatformImageModel` (`jvmSharedMain`), `TraceSection`. |
| `utils/` | Focus-area padding, `ImageSize`, small math/lerp helpers. |
| `previews/` (common) | Shared sizing constants: `BigImage`, `CoverArt`, `Notif`, `Square`, `WideItem`. |
| `previews/` (android) | `ComponentsPreview` theme/provider harness and one `@Preview` file per renderer. |

### Platform gates in `subs`

- `gridCellOffscreenLayerEnabled` is `true` on JVM (cached rasterized grid cells help Skiko) and
  `false` on Android (extra GPU surfaces with no win — the hardware renderer already caches cells).
- `crossfadeImagesEnabled` is `true` for both current targets.
- `platformImageModel` normalises image references; the shared Android/JVM actual hands Coil an okio
  `Path` so file paths containing `#`/`?` are not truncated by URI parsing.
- `LocalForceFakeImages` / `LocalFirstContentReporter` are host-provided hooks; the sample's default
  is off/null.

## Using it

Build a list of `ListModel`s in the state, then dispatch each one:

```kotlin
LazyColumn {
    items(models, key = { it.dataId }) { model ->
        model.Content(
            sink = sink,
            debug = false,
            mod = Modifier.animateItem(),
            pad = PaddingValues(horizontal = 16.dp),
        )
    }
}
```

## Differences from Chipbox

- Chipbox-only models (`NowPlayingHomeCard`, `ScanStatusCard`) and their domain types were not
  ported.
- `FakeImage` uses the shared `:core:bitmaps` generator on every target instead of Android's
  `android.graphics` goldens and a separate desktop gradient.
- `TraceSection` is a structural no-op: this repo has no macrobenchmark target.
- `StackFrameSummary` is a single common implementation; there is no JavaScript target here.
- Accessibility text uses `ComponentStringId` + `LocalComponentStringProvider` instead of
  Chipbox's string module.

## Module facts

- **Targets:** Android + JVM (no JS)
- **Source sets:** `commonMain` (renderers, models, mapping), `androidMain` (`@Preview` harnesses
  and the Android offscreen-layer actual), `jvmMain` (the JVM offscreen-layer actual),
  `src/main/java` (`jvmSharedMain`: the okio path rewrite shared by both targets)
- `androidMain` previews also provide `FullscreenBlack`, `FullScreenOf` and `PreviewActionSink`
  alongside one `*Previews.kt` harness per component.

## Dependencies

| Dependency | Why |
|---|---|
| `sage.common.ui.components` (`api`) | SAGE `ListModel` types and reused building blocks |
| `sage.common.ui.strings` (`api`) | `SageStringId`, `StringProvider` |
| `sage.common.appcomm` | `ActionSink` |
| `sage.common.images` | `SourceInfo` |
| `sage.common.ui.perfCompose` | `WithMeasurementComponent` |
| `sage.common.ui.iconsReal` | Real icon vectors |
| `:core:bitmaps` | `FakeImage` renders generated art in previews |
| `coil-kt-core`, `coil-kt-compose` | `CrossfadeImage`, `AsyncImagePainter` |
| `okio` | `Path` model for raw filesystem image paths |
| `androidx.compose.bom` + `ui-tooling-preview` (androidMain) | `@Preview` harnesses; declared through the top-level handler so the BOM uses `platform()` |

## Conventions

- Renderers never read `R.string`. Accessibility text goes through `ComponentStringId` and
  `LocalComponentStringProvider`.
- Keep renderers model-generic. Chipbox-specific models do not belong here; the sample has none.
- This module has no tests: it is exercised through the app modules' render tests and previews.
