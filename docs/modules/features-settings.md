# `:features:settings`

The settings screen: appearance, demo behaviour, and About. All state lives in
`:core:settings`' `SampleSettingsManager`; this module only mirrors it into a `ListState` and writes
selections back.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## Rows

`SettingsState.toListItems` builds, in order:

0. **Account** — only while a User is signed in: a section header, a "Signed in as <username>"
   `LabelValueListModel`, and a "Sign out" `ButtonListModel` (no confirmation)
1. **Appearance** — theme dropdown (`ThemeMode`), font dropdown (`FontChoice`)
2. **Simulation** — simulate-delay dropdown (`SimulateDelay`), animation-scale dropdown
   (`AnimationScale`), demo-outcome dropdown (`DemoOutcome`), "Home shows same content" checkbox,
   "Reset demo data" row
3. **About** — app version, build date (display-only `LabelValueListModel`s)
4. **Developer** — a "Component library" row that opens `:features:componentlibrary`. The sample
   has no debug gate, so the row is always visible.
4. **Developer** — a section header and a "Component library" `CtaListModel`. It dispatches
   `SettingsAction.OpenComponentLibraryClicked`, which the view model turns into
   `SageEvent.NavigateTo("component-library")` (the route string is guarded by
   `SampleDestinationTest`, like Browse's). The sample has no debug gate, so the row is always
   visible; a real app would hide the section in release builds.

Dropdowns are closed sets: option order mirrors each enum's `entries`, so the selected index maps
straight back to a value. At most one dropdown is expanded; re-tapping the open one closes it.

## View model

`SettingsViewModel : SampleListViewModel<SettingsState>`:

- Collects `themeMode`, `fontChoice`, `simulateDelay`, `animationScale`, `demoOutcome` and
  `homeShowsSameContent` from the app-scoped manager, so changes made elsewhere (or restored from
  storage) appear here.
- `DropdownExpandClicked` toggles `expandedDropdownId`; a selection writes to the manager and
  collapses the dropdown.
- `HomeShowsSameContentToggled` writes the inverse of the current value.
- Collects `IdentityRepository.state` into `signedInUser`. `SignOutClicked` emits
  `SignOutRequested` (from `:core:repository:api`) and never calls `IdentityRepository.signOut()`:
  the shell maps it to `SampleSignInGateHolder.signOut`, which marks the sign-out deliberate so the
  next Sign in opens Home (on the web the URL is first replaced with Home's).
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
| `kotlinx-datetime` | ISO build-date formatting shared with the browser |
| SAGE `list`, `appcomm`, `appinfo`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, `AppInfo`, analytics |

## Tests

`SettingsStateTest` (row order, dropdown labels/selection/expansion, About rows, reset row states)
and `SettingsViewModelTest` (mirroring, writes, expansion toggling, reset success/failure and the
double-tap guard). Fixtures live in `SettingsTestFixtures.kt`.
