# `:features:listitems`

Three demo screens over one seeded generator: **List Items** (text rows, unpaginated), **List Items
with Images** (image rows, paginated), and **Grid of Images** (50 image cells, paginated).

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## Data

| Type | Role |
|---|---|
| `SampleItem` | `id`, `name`, `caption`. `id` is the row's stable list identity (backs `ListModel.dataId`). |
| `SampleItemRepository` (`:core:repository:api`) | `load(offset, limit)` window; `load()` returns the whole 25-item set. |
| `GridOfImagesRepository` (`:core:repository:api`) | Same shape over a 50-item set. |
| Demo implementations (`:core:repository:demo`) | Deterministic seeded data from `DemoContent.sampleItems`. Read `DemoOutcome` per call (`CONTENT` / `EMPTY` / `ERROR`). The browser binds the HTTP implementations instead. |
| `DemoSampleItems.kt` | `demoSampleItems(count)` and `demoSampleItemsWindow(offset, limit, total)`; the latter builds the prefix up to the window end so pages match the non-paginated screen. |

## Screens

### List Items — `ListItemsState` + `ListItemsViewModel`

Unpaginated. Loads the whole generated set, maps each item to `NameCaptionListModel` (loading type
`TEXT_CAPTION`). Empty → one `EmptyStateListModel`; error → one `ErrorStateListModel` naming
`list-items`. Row taps navigate to `content-detail/list-items:<id>`.

### List Items with Images — `ListItemsWithImagesState` + `ListItemsWithImagesViewModel`

`PaginationType.Standard(pageSize = 10)`, loading type `TEXT_CAPTION_IMAGE`. Rows are
`ImageNameCaptionListModel` with a `generated://` `SourceInfo` (`list-items-with-images/<id>`,
128 px). Tracks `windowStart`, `hasMoreAfter`, `loadingMore`, `loadingPrevious`; boundary loaders
render only beside non-empty content. Row taps navigate to
`content-detail/list-items-with-images:<id>`.

### Grid of Images — `GridOfImagesState` + `GridOfImagesViewModel`

`ColumnType.Regular(160)` makes the app choose SAGE's `GridScreen` (two columns compact, more as the
window widens). `PaginationType.Standard(pageSize = 20, LoadingType.SQUARE)` over the 50-item set.
Cells are `GridImageListModel` with `grid-of-images/<id>` art. Taps navigate to
`content-detail/grid-of-images:<id>`.

## Shared paging — `PagedListItemsViewModel`

Abstract base for the two paginated screens:

- one `pageJob` serializes requests, coalescing scroll-signal bursts;
- `InitNoArgs` opens a page at offset 0; `InitWithPageNumber` opens at a page offset (a hook for
  future "resume where I left off");
- `LoadMoreRequested` appends, `LoadPreviousRequested` prepends (no-op at offset 0);
- appends keep existing rows on failure and dedupe by `SampleItem.id` (`appendDistinct` /
  `prependDistinct`);
- a short or empty page sets `hasMoreAfter = false`.

Each screen supplies six small hooks: `pageSize`, `fetchPage`, `contentOf`, `windowStartOf`,
`hasMoreAfterOf`, `withPaging`. Fetches apply the Settings delay then run on the disk dispatcher.

## Strings

`SampleStringId` covers titles, empty and error text for all three screens. The item-clicked
string exists in the enum but the screens currently navigate instead of showing a snackbar.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base classes |
| `:core:models:api` | `SampleItem` + the route codec |
| `:core:strings:api` | `SampleStringId` vocabulary |
| `:core:bitmaps` | `generated://` URLs |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, analytics, `StringProvider` |

## Tests

State tests for each screen (loading/content/empty/error, loaders beside content), view-model tests
for load and paging, demo-repository determinism/windowing, and `ListItemsListIdentityTest` for the
`dataId` contract.
