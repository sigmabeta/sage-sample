# `:features:search`

Debounced search with two independent result sets (list rows and grid images) and a persisted
recent-search history. The app overlays its own search field; this module owns the query state
machine and the results.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## Query lifecycle

`SearchViewModel : SampleListViewModel<SearchState>`:

1. Typing sends SAGE's `SageAction.SearchQueryEntered`; only `SearchState.query` changes, so the
   previous results stay on screen while the next query is edited.
2. `observeQuery()` trims, `distinctUntilChanged`, then waits `SEARCH_DEBOUNCE_MILLIS = 300`.
3. A settled query of at least `MIN_QUERY_LENGTH = 3` characters becomes `submittedQuery` and runs
   **two** repository calls in parallel:
   - `searchList` → `ImageNameCaptionListModel` rows (`search.list`)
   - `searchGrid` → `GridImageListModel` cells (`search.grid`)
4. The previous pair is cancelled first, and each emission is published only while its query is
   still the submitted one, so a superseded search can neither waste work nor overwrite newer rows.
5. A blank query returns to the resting prompt immediately, without the debounce.

## Result generation

`DemoSearchRepository` seeds from the trimmed, lowercased query plus a `LIST`/`GRID` kind
(`searchSeedFor`). Each kind picks its own random count from 1–20, so the same query always
reproduces the same rows and `generated://` art while the two sections have independent sizes. The
repository reads `DemoOutcome` per call and maps `EMPTY`/`ERROR` onto both sections. Result taps
route through `content-detail/search:<kind>:<seed>:<id>`.

## State and resting screen

`SearchState`:

- `query` — live field text (owned here so it can be debounced)
- `submittedQuery` — what is rendered
- `listResults` / `gridResults` — independent `LCE` slots, each with its own header, loading and
  error rows, so one can fail while the other shows content
- `history` — persisted recent searches

With no submitted query it renders recent searches (`SearchHistoryListModel`), plus the resting
prompt (`SEARCH_PROMPT`) while history has fewer than 5 rows. If both result sections are empty it
renders one `SEARCH_NO_RESULTS` state.

## Recent searches

`SearchHistoryManager` (`:core:settings`) is app-scoped and persisted. Search:

- mirrors `history` into state;
- records a query only after it has been settled for `SEARCH_HISTORY_RECORD_DELAY_MILLIS = 3_000`
  **and** both result sections are terminal **and** at least one has content;
- cancels the pending record when a newer query settles or the field is cleared;
- `HistoryClicked` refills the field (the normal debounce submits it) via `fieldUpdates`;
  `HistoryRemoved` deletes one entry.

`fieldUpdates: SharedFlow<String>` exists because the app's field keeps its text in local
composition state (synchronous typing echo); it subscribes only to apply refills/clears the user did
not type.

## String IDs

`SampleStringId` holds the title, hint, back/clear accessibility labels, prompt, no-results
template, and the two section headers. `SEARCH_NO_RESULTS` takes one argument (the query).

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class |
| `:core:repository:api` (`api`) | Repository seams, constants and wire contract |
| `:core:settings` (`api`) | `SearchHistoryManager` |
| `:core:models:api` | `SearchResult` + the route codec |
| `:core:strings:api` | `SampleStringId` vocabulary |
| `:core:bitmaps` | `generated://` URLs |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | State/models, actions, analytics, `StringProvider` |

## Tests

`SearchResultGeneratorTest` (seeding, count bounds, kind independence),
`DemoSearchRepositoryTest` (determinism, outcomes), `SearchResultGeneratorTest` (shared seed and bounds), `SearchStateTest` (resting/history/sections/
no-results), `SearchViewModelTest` (debounce, min length, cancellation, two calls, history gates).
