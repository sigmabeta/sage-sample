# `:core:repository:demo`

The deterministic in-process data source. Android and desktop bind it; `:apps:server` calls the same
generators to answer the browser's requests.

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)   // JVM-only: SAGE's StringGenerator lives in a JVM source set
    alias(libs.plugins.metro)
}
```

## Why JVM-only

The content generators use SAGE's `ui.strings` `StringGenerator`, which is published from a JVM
source set and therefore has no JS variant. Keeping this module JVM-only is what lets the feature
modules and the repository interfaces stay multiplatform without dragging the generator into the
browser; the browser uses `:core:repository:remote` instead.

## Generators

`DemoContent` is a pure object and the single source of truth for the sample's data:

| Function | Result |
|---|---|
| `homeItemCount`, `homeItem`, `homeGrid` | 18 seeded Home items per module seed |
| `contentDetail(ContentId)` | Full detail seeded from a 64-bit FNV hash of the ID; its title is the source row's own name when the ID came from a surface; `null` for an unknown namespace |
| `sampleItems(offset, limit, total)` | A window of the seeded List Items catalog |
| `searchResults(query, kind)` / `searchResultsForSeed(seed)` | Deterministic results seeded by the normalized query + kind, or by the seed a route carries |

The same seed produces the same rows on Android, desktop and the server. The Settings demo outcome
(content/empty/error) is applied by the repository wrappers, not by `DemoContent`, so the server
always returns content and only the local clients simulate terminal states.

## Bindings

`DemoRepositories.kt` contains `DemoHomeRepository`, `DemoContentDetailRepository`,
`DemoSampleItemRepository`, `DemoGridOfImagesRepository` and `DemoSearchRepository`. Each is
`@SingleIn` + `@ContributesBinding(AppScope)` + `@Inject`, and reads `SampleSettingsManager.demoOutcome`
when a load is requested (not at construction). `DemoContentDetailRepository` has no outcome branch;
a missing ID is its explicit not-found result.

## Tests

The demo repository tests live beside the features that consume them (`:features:home`,
`:features:contentdetail`, `:features:listitems`, `:features:search`), with a `jvmTest` dependency
on this module. They pin determinism, paging, empty/error outcomes, and outcome re-reads.
