# `:core:ui:components:api`

The sample-owned rows plus the renderer configuration for SAGE's shared renderer suite
(`:common:ui:composables`). The suite itself — `ListModel.Content`, the per-model renderers, the
`subs/` and `utils/` building blocks, and the `@Preview` harnesses — lives in SAGE and is re-exported
here so every app gets it from one dependency.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.sage.compose.kmp)
}
```

It is multiplatform (`commonMain`, `androidMain`) because its Compose code is shared by Android,
desktop and the browser.

## The seam

SAGE's `ListModel.Content` consults `LocalSageRenderers.current.contentFor` before its built-in
renderers, and falls through to the built-ins when it returns `null`. `SampleRenderers.kt` supplies
that hook:

```kotlin
val SampleRenderers = SageRenderers(
    contentFor = { model ->
        when (model) {
            is TextFieldListModel -> { _, sink, _, modifier, padding ->
                TextFieldListItem(model = model, actionSink = sink, modifier = modifier, padding = padding)
            }
            is ButtonListModel -> { _, sink, _, modifier, padding ->
                ButtonListItem(model = model, actionSink = sink, modifier = modifier, padding = padding)
            }
            is FormMessageListModel -> { _, _, _, modifier, padding ->
                FormMessageListItem(model = model, modifier = modifier, padding = padding)
            }
            else -> null
        }
    },
    previewImage = { sourceInfo, modifier ->
        GeneratedBitmap(sourceInfo = sourceInfo.info.toString(), modifier = modifier)
    },
)
```

`contentFor` maps the sample's three form models to their rows — the branches the old
`ComposableMapping.kt` owned — and returns `null` for everything else, so SAGE's built-ins draw the
rest. `previewImage` plugs `:core:bitmaps`' generated art back into SAGE's inspection-mode stand-in
(`FakeImage`), keeping the Paparazzi goldens byte-identical.

The apps install this config with `ProvideSampleText` (in `LocalSampleStringProvider.kt`), which
wraps both the sample's `LocalSampleStringProvider` and SAGE's `ProvideSageRenderers` around the same
content, so the two locals cannot drift apart.

## Sample-owned surface

| Area | Contents |
|---|---|
| `SampleRenderers.kt` | `SampleRenderers`: `contentFor` (form rows) + `previewImage` (`:core:bitmaps` art) |
| `FormListItems.kt` | `TextFieldListItem`, `ButtonListItem`, `FormMessageListItem` for the Compose-free form models in `:core:ui:components:models` |
| `SearchContent.kt` | App-owned search: one adaptive `LazyVerticalGrid` with the field overlaid and a status-bar scrim. Full-width rows/headers span the grid; grid cells keep one lane. |
| `SearchField.kt` | The search box chrome: back action, clear action, focus request, constant 48 dp control height. Not a `ListModel`; the caller owns the text and passes semantics strings. |
| `LocalSampleStringProvider.kt` | `LocalSampleStringProvider` + `SampleStringId.text(...)` helpers, plus `ProvideSampleText`, the one shared installer for both string locals. |
| `previews/ComponentsTheme.kt` (android) | `ComponentsPreview`: the preview wrapper for the sample's form rows. |

## Design choices

- Domain-specific models and their domain types are not part of the shared suite; an app adds its
  own alongside this module.
- Accessibility text uses `SampleStringId` + `LocalSampleStringProvider`, and SAGE's renderers read
  their eight `SageComponentStringId`s from the same provider through `ProvideSageRenderers`.
- The module has no tests: it is exercised through the app modules' render tests and previews.

## Module facts

- **Targets:** Android + JVM; JS (Node) when built with `-Psage.js`
- **Source sets:** `commonMain` (sample rows + `SampleRenderers` + `ProvideSampleText`), `androidMain`
  (`FormListItemsPreviews`, `ComponentsPreview`)

## Dependencies

| Dependency | Why |
|---|---|
| `sage.common.ui.composables` (`api`) | SAGE's shared renderer suite — `ListModel.Content`, `SageRenderers`, `ProvideSageRenderers`, and (transitively) the `ListModel` vocabulary, `StringProvider`/`SageStringId`, `ActionSink`, `SourceInfo`/`ArtworkCorner` |
| `:core:ui:components:models` (`api`) | The sample's Compose-free form models |
| `:core:strings:api` (`api`) | The shared `SampleStringId` vocabulary |
| `sage.common.ui.iconsReal` | Real icon vectors (`SearchField`'s back/clear actions) |
| `:core:bitmaps` | `SampleRenderers.previewImage` draws generated art in previews |
| `:core:strings:real` (`androidMain`) | The shared provider for `ComponentsPreview` |
| `androidx.compose.bom` + `ui-tooling-preview` (androidMain) | `@Preview` harness for the form rows |

## Conventions

- Renderers never read `R.string`. Accessibility text goes through `SampleStringId` /
  `SageComponentStringId` and the shared provider; the strings module owns the values.
- Keep renderers model-generic. Screen-specific models do not belong here; the form rows are
  generic enough for any form.
