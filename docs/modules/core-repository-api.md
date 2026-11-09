# `:core:repository:api`

The sample's data seams plus the JSON contract shared by `:apps:server` and
`:core:repository:remote`.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.kotlin.serialization)
}
```

## Seams

| Interface | Methods | Used by |
|---|---|---|
| `HomeRepository` | `suspend itemCount(seed)`, `suspend itemAt(seed, index)`, `gridImages(seed, limit): Flow<LCE<List<HomeItem>>>` | Home modules |
| `ContentDetailRepository` | `load(ContentId): Flow<LCE<ContentDetail?>>` | Content detail |
| `SampleItemRepository` | `load(offset, limit)` + `load()` | List Items, List Items with Images |
| `GridOfImagesRepository` | `load(offset, limit)` + `load()` | Grid of Images |
| `SearchRepository` | `searchList(query)`, `searchGrid(query)` | Search |

`itemCount`/`itemAt` are suspend because the browser's implementation performs HTTP; the demo
implementation is still a direct call. The list calls carry operation-name constants and catalog
sizes (`LIST_ITEMS_*`, `GRID_OF_IMAGES_*`) so features and implementations agree.

`SearchResultKind`, `searchSeedFor`, `normalizeSearchQuery`, the operation names and the result
bounds are here too: the demo generator, the HTTP client and Search's artwork/content-id derivation
all use the same deterministic seed.

## Wire models

`RepositoryWire.kt` defines `@Serializable` DTOs separate from the domain models. The one place
they differ materially is icons: a `ContentAction` carries SAGE's sealed `Icon` in the app and a
stable wire name (for example `"play"`) on the wire. `Icon.wireName()` / `iconFromWireName()`
convert in both directions, and an unknown name falls back to `Icon.Description` rather than
failing a screen.

`SampleApiRoutes` and `SampleApiParams` own every path and query parameter name. `normalizedPage`
clamps `offset`/`limit` for both sides. `toDto`/`toDomain`/`toDomainOrNull` are shared so the
server and client cannot drift.

## Tests

`RepositoryWireTest` round-trips every row and a full `ContentDetail`, checks every icon's wire name
round trip, asserts malformed content ids decode to `null`, and pins the paging bounds.
