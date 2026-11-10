# `:core:bitmaps`

Deterministic placeholder artwork shared by every app. It serves two consumers:

1. `@Preview` renders that need meaningful art instead of a loading placeholder.
2. Coil requests for `generated://` URLs, so demo screens can show "real" images without shipping
   bitmap assets or adding a network fetcher.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)          // Android + JVM targets
    alias(libs.plugins.sage.compose.kmp)  // Compose Multiplatform
}
```

It is multiplatform because `commonMain` is the whole implementation: the Android app and the
Compose Desktop app resolve their own variant from one source set.

## Contents

| File | Role |
|---|---|
| `BitmapGenerator.kt` | Seeds `kotlin.random.Random` from `sourceInfo.hashCode()` and draws a per-row gradient into a Compose `ImageBitmap`. Same input ⇒ same art. `width`/`height` choose the output size; one dimension alone gives a square; neither uses the seeded size. |
| `GeneratedBitmap.kt` | `@Composable GeneratedBitmap(sourceInfo, width, height, …)` — remembers the bitmap and draws it with `ContentScale.Crop`. Intended for previews. |
| `GeneratedImageFetcher.kt` | A Coil 3 `Fetcher` for the `generated://` scheme. The URL without query/fragment is the seed, so `?width=`/`?height=` change only resolution. Returns an `ImageFetchResult`, skipping the decode pipeline. |
| `GeneratedImageFetcher.urlFor(seed, width, height)` | The single source of truth for building a `generated://` URL. Features call this; they never concatenate the scheme themselves. |
| `generatedImageLoader(context)` | Builds an `ImageLoader` with the fetcher registered, alongside Coil's defaults. Android and desktop install it via `SingletonImageLoader.setSafe`; the browser instead provides `LocalForceFakeImages = true`, so `CrossfadeImage` draws the bitmap directly. |
| `CoilImage.android.kt` / `CoilImage.jvm.kt` | `expect`/`actual` conversion from Compose `ImageBitmap` to Coil's platform `Image`. The `jsMain` actual fails loudly: the browser renders `GeneratedBitmap` directly and never routes generated art through Coil. |

`commonMain` re-exports `coil-kt-core` with `api`, so consumers can build a loader without
declaring Coil core themselves.

## URL contract

```
generated://<seed>?width=<px>&height=<px>
```

`seed` is any stable string (an ID, a query+kind pair, a content ID). Features namespace their
seeds (`home/…`, `search/…`, `list-items-with-images/…`, `grid-of-images/…`) so different surfaces
get different art; Content detail reuses the source surface's seed so its hero is the tapped row's
artwork. Non-positive or malformed size parameters are ignored.

## Using it

Directly, without Coil:

```kotlin
@Preview
@Composable
private fun CoverPreview() {
    GeneratedBitmap(sourceInfo = "cover-1", width = 300, height = 200, modifier = Modifier.size(96.dp))
}
```

As a Coil request:

```kotlin
// Android and desktop install the loader with SingletonImageLoader.setSafe in their entry points.
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

- **Plugins:** `sage.kmp` + `sage.kmp.js` + `sage.compose.kmp`
- **Targets:** Android + JVM
- **Source sets:** `commonMain`, `androidMain`, `jvmMain`, `jsMain`, `jvmTest`
- **Dependencies:** `coil-kt-core` (`api`); no SAGE modules

## Wire-up

- Android: `SampleApplication.onCreate` registers `generatedImageLoader`.
- Desktop: `Main.kt` does the same before the window opens.

Both must register before any composable resolves `SingletonImageLoader`, otherwise
`AsyncImage` falls back to the default loader and fails on the custom scheme.

## Tests

`src/jvmTest`, no Skia or Compose UI test dependency:

- `BitmapGeneratorTest` — `specFor` seeding, size resolution, validation of non-positive sizes.
- `GeneratedImageFetcherTest` — `urlFor` formatting, seed extraction ignoring query, size parsing,
  and factory routing (custom scheme vs. everything else).
