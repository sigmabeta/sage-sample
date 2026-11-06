# `:core:bitmaps`

> Deterministic placeholder bitmaps for previews, shared by the Android and desktop apps.

A Compose Multiplatform module (`sage.kmp` + `sage.compose.kmp`) so one `commonMain`
implementation resolves for both consumers. Nothing at runtime draws these images: the
`@Preview` call sites in `:app` and `:apps:jvm` are the only users.

## Contents

| File | What it is |
| --- | --- |
| `BitmapGenerator.kt` | Seeds a `Random` from the source object's hash and paints a per-row gradient into a Compose `ImageBitmap`. Same source object means same artwork. `specFor` exposes the seeded geometry/colours for tests without touching Skia. |
| `GeneratedBitmap.kt` | `GeneratedBitmap(sourceInfo, modifier)` — a `@Composable` that renders the generated image through a `BitmapPainter`. |

## Using it

```kotlin
@Preview
@Composable
private fun CoverPreview() {
    GeneratedBitmap(sourceInfo = "cover-1", modifier = Modifier.size(96.dp))
}
```

## Module facts

- **Plugins:** `sage.kmp` + `sage.compose.kmp`
- **Targets:** Android + JVM
- **Source sets:** `commonMain`, `jvmTest`
- **SAGE dependencies:** none
