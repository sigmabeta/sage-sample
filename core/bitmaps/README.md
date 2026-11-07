# `:core:bitmaps`

> Deterministic placeholder bitmaps for previews and `generated://` Coil requests, shared by the
> Android and desktop apps.

A Compose Multiplatform module (`sage.kmp` + `sage.compose.kmp`) so one `commonMain`
implementation resolves for both consumers. The same URL always produces the same artwork.

## Contents

| File | What it is |
| --- | --- |
| `BitmapGenerator.kt` | Seeds a `Random` from the source object's hash and paints a per-row gradient into a Compose `ImageBitmap`. Optional `width`/`height` choose the output size (one dimension implies a square; neither keeps the seeded size). `specFor` exposes the seeded geometry/colours for tests without touching Skia. |
| `GeneratedBitmap.kt` | `GeneratedBitmap(sourceInfo, width, height, modifier)` — a `@Composable` that renders the generated image through a `BitmapPainter`, for previews that don't want a Coil round trip. |
| `GeneratedImageFetcher.kt` | Coil `Fetcher` for `generated://` URLs. Seeds the generator from the URL's scheme/authority/path and reads optional `?width=&height=` query parameters. Returns an `ImageFetchResult` and leaves every other scheme to the rest of Coil's registry. `generatedImageLoader(context)` builds an `ImageLoader` with the factory registered. |
| `CoilImage.android.kt` / `CoilImage.jvm.kt` | The `ImageBitmap` → platform `coil3.Image` conversion (`asAndroidBitmap`/`asSkiaBitmap`). |

## Using it

Directly, without Coil:

```kotlin
@Preview
@Composable
private fun CoverPreview() {
    GeneratedBitmap(sourceInfo = "cover-1", width = 300, height = 200, modifier = Modifier.size(96.dp))
}
```

As a Coil request — the URL's scheme/authority/path is the seed and `width`/`height` pick the
output size, so `generated://cover-1` always has the same colours:

```kotlin
// :apps:android and :apps:jvm install the loader with SingletonImageLoader.setSafe in their entry points.
AsyncImage(
    model = GeneratedImageFetcher.urlFor("cover-1", width = 300, height = 200),
    contentDescription = null,
)

// Or pass a loader explicitly (useful in previews):
AsyncImage(
    model = GeneratedImageFetcher.urlFor("cover-1", width = 300, height = 200),
    imageLoader = generatedImageLoader(LocalPlatformContext.current),
    contentDescription = null,
)
```

## Module facts

- **Plugins:** `sage.kmp` + `sage.compose.kmp`
- **Targets:** Android + JVM
- **Source sets:** `commonMain`, `androidMain`, `jvmMain`, `jvmTest`
- **Dependencies:** `coil-kt-core` (api); no SAGE modules
