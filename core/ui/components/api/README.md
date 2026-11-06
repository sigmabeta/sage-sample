# `:core:ui:components:api`

> The SAGE Sample Compose renderer suite — the composables SAGE's list framework draws, plus the
> mapping that wires every supported `ListModel` to its renderer.

Ported from Chipbox's `:cbox:common:ui:components:api` and kept in the same shape: one module
that re-exports `libs.sage.common.ui.components` (`api(...)`) and owns the renderers. Consumers
get SAGE's `ListModel` vocabulary and this module's composables from one dependency. There is no
separate `:real` module.

## Contents

- **`ComposableMapping.kt`** — `ListModel.Content(sink, debug, mod, pad)` is the keystone: a
  `when` over every supported `ListModel` subtype that dispatches to the matching composable. It
  tags each row with its model's `simpleName` (for UI-test selection) and wraps rendering in
  SAGE's `WithMeasurementComponent` perf probe. Unknown models fall through to a `Spacer`.
- **List-item renderers** — `IconNameListItem`, `NameCaptionListItem`,
  `NameCaptionValueListItem`, `ImageNameListItem`, `ImageNameCaptionListItem`,
  `LabelValueListItem`, `LabelRatingListItem`, `LabelCheckboxItem`, `ConfirmationListItem`,
  `ActionItem`, `EditTextListItem`, `ExpandingDropdownListItem`,
  `CollapsibleDetailsListItem`, `SearchHistoryListItem`, `NotifListItem`, `WideItem`,
  `Section*`/`Subsection*`, `EmptyListIndicator`, and the `Loading*` placeholder family.
- **Image components** — `HeroImage`, `GridImage`, `HorizontalScroller` (a `LazyRow` that
  recursively renders nested `ListModel`s), and `subs/CrossfadeImage` (Coil loading with
  size-bucketing and a crossfade).
- **Reorder / dismiss wrappers** — `DraggableListModel` / `DraggableListItem` and
  `DismissibleListModel` / `SwipeToRemoveBox`. `Subsection` can render any child model through
  the same dispatcher.
- **`subs/`** — `TintedText`, `TintedIcon`, `FakeImage` (delegates to `:core:bitmaps`),
  `ElevatedPill` / `ElevatedCircle` / `ElevatedRoundRect`, `Flasher`, `Rating`,
  `LabeledThingy`, `LoadingThingy`, `MenuActionIcon`, `TraceSection`, and the
  `LocalForceFakeImages` / `LocalFirstContentReporter` plumbing.
- **`utils/`** — `FocusArea.kt`, `ImageSize.kt`, and `Math.kt`.
- **`previews/`** — shared sizing constants in `commonMain`; `ComponentsPreview`,
  `FullscreenBlack`, `FullScreenOf`, and `PreviewActionSink` in `androidMain`, alongside one
  `*Previews.kt` `@Preview` harness per component.

## Using it

Build a list of `ListModel`s in the state, then dispatch each one through `Content`:

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

Both composition roots install the app's `StringProvider` into
`LocalComponentStringProvider`; renderers resolve accessibility labels from it through their
own `ComponentStringId : SageStringId` enum. The preview harness installs an English-only
provider so previews render without the app graph.

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

- **Plugins:** `sage.kmp` + `sage.compose.kmp`
- **Targets:** Android + JVM (no JS)
- **Source sets:** `commonMain` (renderers, models, mapping), `androidMain` (`@Preview`
  harnesses and the Android offscreen-layer actual), `jvmMain` (the JVM offscreen-layer
  actual), `src/main/java` (`jvmSharedMain`: the okio path rewrite shared by both targets)
- **Dependencies:** `libs.sage.common.ui.components` (`api`),
  `libs.sage.common.ui.strings` (`api`), `sage.common.appcomm`, `sage.common.images`,
  `sage.common.ui.perfCompose`, `sage.common.ui.iconsReal`, `:core:bitmaps`, Coil, okio;
  `androidMain` adds Compose tooling for the previews.
