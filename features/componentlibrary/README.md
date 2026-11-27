# `:features:componentlibrary`

The Component Library screen: SAGE's `:common:ui:component-library` gallery as the reference
integration. A debug tool reached from Settings' Developer section. **The sample has no debug gate,
so the entry row is always visible**; a real app would hide it in release builds.

See [`docs/modules/features-componentlibrary.md`](../../docs/modules/features-componentlibrary.md).

- `ComponentLibraryState` holds a `ComponentLibraryConfig` and returns the catalog plus the sample's
  own form rows (`TextFieldListModel`, `ButtonListModel`, `FormMessageListModel`).
- `ComponentLibraryViewModel` forwards `ComponentLibraryAction`s to SAGE's pure `reduce` and exposes
  the live `style` for `ComponentLibraryHost`.
- Plugins mirror `:features:settings`: `sage.kmp` + `sage.kmp.js` + `metro`.
