# `:features:browse`

Browse is the sample's index of demo screens. It is a deliberately load-free, static destination
list: there is no repository, no loading placeholder, and no empty/error state.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
}
```

## What it owns

| File | Role |
|---|---|
| `BrowseState.kt` | `data object BrowseState : ListState`. Title `shouldShowBack = false` (top-level tab). Emits four `SingleTextListModel` rows with stable data ids 100–103. |
| `BrowseAction.kt` | `OpenListItems`, `OpenListItemsWithImages`, `OpenGridOfImages`, `OpenContentDetail`. |
| `BrowseViewModel.kt` | Metro-contributed `SampleListViewModel<BrowseState>`. Each action becomes a one-shot `SageEvent.NavigateTo`. |

## Navigation contract

The view model emits route strings that must stay in sync with `SampleDestination`'s `destName`:

| Action | Route |
|---|---|
| `OpenListItems` | `list-items` |
| `OpenListItemsWithImages` | `list-items-with-images` |
| `OpenGridOfImages` | `grid-of-images` |
| `OpenContentDetail` | `content-detail/<DEFAULT_CONTENT_ID>` via `contentDetailRoute` |

The Content-detail route uses `:core:models:api`'s codec (`DEFAULT_CONTENT_ID =
ContentId("browse:featured")`) so Browse does not invent its own encoding. `:core:nav`'s
`SampleDestinationTest` guards the mapping, so changing one side alone fails a test.

## Dependencies

| Dependency | Why |
|---|---|
| `:core:viewmodel` (`api`) | Base class |
| `:core:models:api` | `contentDetailRoute`, `DEFAULT_CONTENT_ID`, `ContentId` |
| `:core:strings:api` | `SampleStringId` vocabulary |
| `metrox-viewmodel` (`api`) | `@ViewModelKey` |
| SAGE `list`, `appcomm`, `analytics`, `logging`, `ui.components`, `ui.strings` | `ListState`, actions/events, analytics, models, strings |

## Tests

`BrowseStateTest` (rows, ids, title), `BrowseViewModelTest` (one navigation event per action, no
repository traffic; fixtures in `BrowseTestFixtures.kt`).
