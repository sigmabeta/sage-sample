# `:features:settings`

The settings screen: appearance, demo behaviour, and About. All state lives in
`:core:settings`' `SampleSettingsManager`; this module only mirrors it into a `ListState` and writes
selections back.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.sage.di)
}
```

## Rows

`SettingsState.toListItems` builds, in order:

1. **Appearance** — theme dropdown (`ThemeMode`), font dropdown (`FontChoice`)
2. **Simulation** — simulate-delay dropdown (`SimulateDelay`), demo-outcome dropdown
   (`DemoOutcome`), "Home shows same content" checkbox, "Reset demo data" row
3. **About** — app version, build date (display-only `LabelValueListModel`s)

Dropdowns are closed sets: option order mirrors each enum's `entries`, so the selected index maps
straight back to a value. At most one dropdown is expanded; re-tapping the open one closes it.

## View model

`SettingsViewModel : SampleListViewModel<SettingsState>`:

- Collects `themeMode`, `fontChoice`, `simulateDelay`, `demoOutcome` and `homeShowsSameContent` from
  the app-scoped manager, so changes made elsewhere (or restored from storage) appear here.
- `DropdownExpandClicked` toggles `expandedDropdownId`; a selection writes to the manager and
  collapses the dropdown.
- `HomeShowsSameContentToggled` writes the inverse of the current value.
- `ResetDemoDataClicked` runs `DemoDataResetter.reset()` on the disk dispatcher, replaces the row
  with inline progress (`LCE.Loading`), then shows a success/failure snackbar on the main
  dispatcher. The loading guard is set synchronously, so a double tap cannot start a parallel reset.
- About values come from SAGE's `AppInfo`: version name and a localized build date formatted from
  `buildTimeMs`.

## Strings

`SampleStringId` covers titles, section headers, every dropdown option, the checkbox label and
caption, reset label/caption/snackbars, and the two About labels.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class |
| `:core:settings` (`api`) | Preferences, `DemoDataResetter`, delay seam |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| `kotlinx-collections-immutable` | Dropdown label lists |
| SAGE `list`, `appcomm`, `appinfo`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, `AppInfo`, analytics |

## Tests

`SettingsStateTest` (row order, dropdown labels/selection/expansion, About rows, reset row states)
and `SettingsViewModelTest` (mirroring, writes, expansion toggling, reset success/failure and the
double-tap guard). Fixtures live in `SettingsTestFixtures.kt`.
