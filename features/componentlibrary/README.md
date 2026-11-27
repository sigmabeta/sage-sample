# `:features:componentlibrary`

The Component Library screen: SAGE's `:common:ui:component-library` gallery as the reference
integration. A debug tool reached from Settings' Developer section. **The sample has no debug gate,
so the entry row is always visible**; a real app would hide it in release builds.

See [`docs/modules/features-componentlibrary.md`](../../docs/modules/features-componentlibrary.md).

- `ComponentLibraryState` holds a `ComponentLibraryConfig` and returns the catalog plus the sample's
  own form rows (`TextFieldListModel`, `ButtonListModel`, `FormMessageListModel`).
- `ComponentLibraryViewModel` forwards `ComponentLibraryAction`s to SAGE's pure `reduce` and exposes
  no typed state flow: the shell's `ComponentLibraryHost(state.listItems)` reads the live style from the
  invisible carrier row SAGE appends to the list.
- Plugins mirror `:features:settings`: `sage.kmp` + `sage.kmp.js` + `metro`.
