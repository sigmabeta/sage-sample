# `:core:strings:real`

The sample's single id → text table: `SampleStringProvider`.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}
```

Every app binds it into its Metro graph and installs it at the composition root; the screenshot
harness and `ComponentsPreview` use it directly. There is no Android `R.string` map and no desktop
copy any more — the only app-owned string resource is `app_name` for the manifest label.

## What it contains

| Piece | Role |
|---|---|
| `SampleStringProvider` | `StringProvider` over one `Map<SageStringId, String>`. An unmapped id throws, so a missing string fails loudly instead of rendering blank. |
| `formatSampleString` | Multiplatform replacement for `String.format` over the Android-style `%1$s` / `%1$d` / `%s` / `%d` specifiers the sample uses. View models cannot use `stringResource`, so formatting lives here. |
| `SampleStringProvider.string(id)` | Non-composable lookup for fixtures and the desktop window title. |

Values are a plain Kotlin map rather than Compose resources. The sample has a small, static,
English-only table and no resource-codegen need; Chipbox's `composeResources` pipeline (and its
Paparazzi classpath workaround) would add build complexity without changing what the goldens pin.

## Tests

`SampleStringProviderTest` (in `src/jvmTest`) asserts every `SampleStringId` has text, exercises the
positional/plain formatters, and checks that an unknown id throws.

## Module facts

- **Plugin:** `sage.kmp`
- **Source set:** `src/commonMain` (+ `src/jvmTest`)
- **Dependencies:** `projects.core.strings.api` (`api`)
- **Consumers:** `:apps:android`, `:apps:jvm`, `:core:ui:previews`, `:core:ui:components:api`'s
  Android preview harness.
