# `:core:strings:api`

The sample's string **vocabulary**: one `SampleStringId` enum for feature text, navigation labels,
app chrome and the renderer suite's accessibility labels.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}
```

It is multiplatform but deliberately Compose-free, so the feature modules can depend on it
without pulling Compose onto their classpath. `SageStringId` (and nothing else) comes from SAGE's
string module.

## Why one enum

One `SampleStringId` enum is the whole vocabulary:

- makes `:core:strings:real` one exhaustive map with a completeness test,
- lets `:core:nav` name navigation labels as ids instead of Android resources,
- lets a feature declare text without owning a per-module key type,
- lets the shared renderer suite use the same ids for accessibility labels.

The constant names are stable and grouped by surface: `NAV_*`, `ACCY_*`, `HOME_*`, `BROWSE_*`,
`CONTENT_DETAIL_*`, `LIST_ITEMS_*`, `GRID_OF_IMAGES_*`, `SEARCH_*`, `SETTINGS_*`.

## Where the composition local lives

`LocalSampleStringProvider` plus the `SampleStringId.text(...)` helpers live in
`:core:ui:components:api`, not here. The ids are platform-neutral; the `@Composable` helpers are UI
and belong with the renderer suite that reads them. Keeping them there also keeps this module free
of Compose.

## Using it

```kotlin
// Feature state / action code: emit an id, never text.
TitleBarModel(title = stringProvider.getString(SampleStringId.HOME_TITLE))

// Compose code: resolve through the shared local.
Text(text = SampleStringId.NAV_HOME.text())
```

## Module facts

- **Plugin:** `sage.kmp`
- **Source set:** `src/commonMain`
- **Dependencies:** `sage.common.ui.strings` (`api`)
- **Consumers:** every feature, `:core:nav`, `:core:ui:components:api`, every app.
