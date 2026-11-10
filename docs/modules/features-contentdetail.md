# `:features:contentdetail`

A generic detail screen driven entirely by generated content: hero art, generated call-to-action
rows, label/value rows, a horizontally scrolling related row, and image/name/caption items. The
domain types and the **content route codec** it speaks live in `:core:models:api`.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## The route codec (`:core:models:api`)

This is the shared navigation contract — Browse, Home, List Items, List Items with Images, Grid of
Images and Search all navigate through it. It lives with `ContentId` in `:core:models:api` so
`:core:nav` can decode a route without depending on this feature.

- `ContentId` — `@JvmInline value class` whose constructor validates `[A-Za-z0-9_.:-]`; a value always
  fits in a route.
- `contentDetailRoute(id)` → `content-detail/<id>`.
- `contentDetailIdFromRoute(route)` → `ContentId?`. A route under the prefix with a bad payload still
  opens the screen (not-found state), rather than being ignored.
- `relatedContentId(parentId, relatedId)` / `contentItemContentId(parentId, itemId)` embed the
  parent detail in a row's ID, so nested detail navigation keeps the row's identity.
- `contentHeroImageSeed(id)` maps a surface ID to the same `generated://` seed that surface used
  for its thumbnail. `contentDetailRelatedImageSeed` / `contentDetailItemImageSeed` do the same
  for the detail's own rows.
- `isKnownContentId(id)` — whether the demo repository can generate content for the ID's namespace
  (`home`, `browse`, `search`, `list-items`, `list-items-with-images`, `grid-of-images`,
  `content-detail`).
- `DEFAULT_CONTENT_ID = ContentId("browse:featured")` and `CONTENT_DETAIL_ROUTE = "content-detail"`
  (shared with `SampleDestination`).

## Screen state

`ContentDetailState` is a `ListState` over `LCE<ContentDetail?>`, with
`ColumnType.Staggered(320, false)` — single column when compact, SAGE's staggered `GridScreen` when
the window fits more than one column.

Each section maps the same `LCE` through its own loading shape:

| Section | Loading type / content |
|---|---|
| Hero | `BIG_IMAGE`; `HeroImageListModel` |
| CTAs | no placeholder; `CtaListModel` per generated action |
| Details | no placeholder; header + `LabelValueListModel` rows (blank fields omitted) |
| Related | `WIDE_ITEM`, horizontally scrollable; header + `HorizontalScrollerListModel` of `WideItemListModel` |
| Items | `TEXT_CAPTION_IMAGE`; header + `ImageNameCaptionListModel` rows |

An `LCE.Content(null)` payload is the explicit **not-found** state and renders one localized
`EmptyStateListModel`. An `LCE.Error` replaces the whole screen with one error row.

## Data

`ContentDetailRepository.load(contentId): Flow<LCE<ContentDetail?>>`.
`DemoContentDetailRepository` generates from an FNV-1a 64-bit seed of the ID, so the same ID
reproduces the same content on every visit; unknown IDs return `null`. For IDs produced by a
surface row, the generated title is that row's own name (`SampleItem.name`, `HomeItem.title` or
`SearchResult.name`), so the detail matches what the user tapped.

Artwork URLs are built with `GeneratedImageFetcher.urlFor` and seeds derived from the source
surface (`list-items/<id>`, `list-items-with-images/<id>`, `grid-of-images/<id>`,
`home/<module>/<item>`, `search/<seed>/<id>`), so the hero is the same art as the tapped thumbnail
at a larger size. Detail-owned rows use parent-qualified seeds
(`content-detail/related/<parent>/<id>`, `content-detail/item/<parent>/<id>`).

## View model

`ContentDetailViewModel : SampleListViewModel<ContentDetailState>`:

- `InitNoArgs` is only the screen-view report.
- The screen sends `SageAction.InitWithString(contentId)`; a missing/invalid ID publishes not-found
  without touching the repository.
- A second init cancels the previous load, so a late emission from an old ID cannot overwrite the
  new screen.
- `HeroClicked`/`CtaClicked` show a snackbar (no destination); `RelatedClicked`/`ItemClicked` push
  another detail, so Back returns through the chain.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class |
| `:core:repository:api` (`api`) | Repository seams, constants and wire contract |
| `:core:models:api` (`api`) | `ContentDetail`, `ContentId` and the route codec |
| `:core:strings:api` | `SampleStringId` vocabulary |
| `:core:bitmaps` | `GeneratedImageFetcher.urlFor` |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, analytics, `StringProvider` |

## Tests

`ContentDetailRouteTest` lives with the codec in `:core:models:api` (validation, encoding/decoding,
known namespaces). `ContentDetailStateTest`
(each section and loading/error/not-found shape), `ContentDetailViewModelTest` (init/cancel,
navigation chain, snackbars), `DemoContentDetailRepositoryTest` (determinism, unknown IDs; `jvmTest` dependency on `:core:repository:demo`).
