# `:core:ui:theme:api`

The sample's single Compose theme: Material 3 color schemes plus the shared type scale.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.compose.kmp)
}
```

## Surface

| Piece | Role |
|---|---|
| `SampleTheme(darkTheme, fontFamily, content)` | Wraps `MaterialTheme` with the light/dark schemes and `sampleTypography(fontFamily)`. |
| `sampleTypography(fontFamily)` | The Material 3 type scale rendered in the chosen family; `FontFamily.Default` reproduces the Material defaults. |

Both apps use it, and so does `:core:ui:previews`, so the theme cannot drift between an app and a
golden.

## Fonts stay app-side

The type scale is shared, but loading the *family* is platform-specific:

- Android maps `FontChoice.INTER` to `Font(R.font.inter_*, …)`.
- Desktop maps it to the classpath OTFs under `resources/fonts/`.

Each app passes the resulting `FontFamily` into `SampleTheme`; the theme module never touches
platform resources.

## Module facts

- **Plugins:** `sage.kmp` + `sage.compose.kmp`
- **Source set:** `src/commonMain`
- **Consumers:** `:apps:android`, `:apps:jvm`, `:core:ui:previews`.
