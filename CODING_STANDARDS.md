# Coding standards

Read during review (the `code-review` skill or any review agent). ktlint, detekt, and Android
lint already enforce formatting and the mechanical rules; these are the judgement calls they
cannot.

## Closed-set logic gets a table test

Any pure function that maps a closed set — an enum, a sealed type, `ColumnType`, `WidthClass`,
`SampleDestination`, `SageAction`/`SageEvent` — gets a table test that names every case.
Include the empty, zero, one, and boundary values, not just the comfortable middle.

- `screenLayoutFor`: every `WidthClass` against `ColumnType.One`, a `Regular` that fits 0, 1,
  and more than 1 column, and a `Staggered` with each flag value.
- `navActionFor`: every destination the app can reach, every handled event type, and an
  unknown route.

A `require`/`check` is for a programmer error the type system cannot express. A
data-dependent value gets a defined fallback — for example zero columns is the single-column
list — and that fallback is tested.

## Intent to existing action: enumerate the branches

When a typed intent maps onto an existing app action, enumerate that action's branches and
test each one. "A top-level tab resets the stack" and "Settings pushes" are different branches
of the same `NavigateTo`; one test per branch.

## Text ownership

User-visible text — including demo or repository content — goes through `SampleStringId`
(`:core:strings:api`) and the shared `StringProvider` from `:core:strings:real`. No feature,
renderer, screenshot module or app keeps its own id → text table; the Android app's only string
resource is `app_name` for the manifest. Repository diagnostics (exception messages) are not UI text
and may stay literals.

## Screenshots are fixtures, not live screens

A `:features:<x>:screenshot` preview renders a hand-built `ListState` through
`:core:ui:previews`; it never constructs a ViewModel, collects a repository, or reads the clock. Use
`FakeModelGenerator` for domain rows instead of hardcoding names and captions, and the shared
`SampleStringProvider`/`SampleTheme` instead of a per-module text or theme copy. That is what keeps
a golden stable and makes a diff mean "the UI changed", not "the seed changed". Add both halves of a
state: a `@DevicePreviews` composable and a matching `@Test` in the module's `screenshot` test
class.

## Comments describe the code

Comments explain the code or the domain. Merge mechanics ("kept in its own section so branches
merge cleanly") belong in the PR description, not in the source.

## Review output

Separate documented-standard breaches (hard) from Fowler baseline smells (judgement calls). A
documented standard overrides the baseline. Skip anything the tooling already enforces.
