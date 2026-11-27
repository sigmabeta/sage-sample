# `:features:componentlibrary`

The Component Library screen: SAGE's `:common:ui:component-library` gallery, wired in as the
reference integration. It is a debug tool reached from Settings' Developer section; the sample has
no debug gate, so it is always available.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## State and rows

`ComponentLibraryState(config: ComponentLibraryConfig) : ListState`:

- `columnType = config.layout.columnType` (one column, a regular grid, or the staggered detail layout).
- `title` is `NAV_COMPONENT_LIBRARY` with back shown.
- `toListItems` returns `componentLibraryItems(config, extraSamples = sampleFormRows(...))`.

`sampleFormRows` adds one of each sample-owned form row from `:core:ui:components:models`
(`TextFieldListModel`, `ButtonListModel`, and `FormMessageListModel` in both tones), with text from
existing Login ids and `dataId`s from 2,000,001, clear of the catalog's. They render through
`SampleRenderers.contentFor`, which `ComponentLibraryHost` preserves.

## View model

`ComponentLibraryViewModel : SampleListViewModel<ComponentLibraryState>` forwards every
`ComponentLibraryAction` to `ComponentLibraryConfig.reduce` and ignores everything else. `style`
exposes the live `ComponentLibraryStyle` as a `StateFlow` for the screen's host, because
`uiStateActual` does not carry the typed config.

## In the shell

`ComponentLibraryScreen` (in `:core:ui:shell`) resolves the view model, collects `style`, and wraps
`SampleListScreen` in `ComponentLibraryHost(style)`. `SampleDestination.COMPONENT_LIBRARY` is
non-top-level, so `navActionFor` pushes it onto the active tab.

## Tests

`ComponentLibraryStateTest` (column type per layout, title, form rows, unique `dataId`s with the
extras) and `ComponentLibraryViewModelTest` (actions reach the config, the style and the rows).
