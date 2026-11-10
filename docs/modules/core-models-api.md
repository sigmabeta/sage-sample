# `:core:models:api`

The sample's shared domain model: the types features, navigation and the screenshot generator all
speak.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}
```

## Contents

| Type | Used by |
|---|---|
| `HomeItem` | Home repository, modules, `:core:ui:previews`' generator |
| `SampleItem` | List Items repositories, states |
| `SearchResult` | Search repository, state, generator |
| `ContentDetail`, `ContentDetailField`, `ContentAction`, `RelatedContent`, `ContentItem` | Content detail repository/state, generator |
| `ContentId` + route codec (`contentDetailRoute`, `contentDetailIdFromRoute`, `contentIdOrNull`, `relatedContentId`, `contentItemContentId`, `contentHeroImageSeed`, `isKnownContentId`, `DEFAULT_CONTENT_ID`, `CONTENT_DETAIL_ROUTE`) | every feature that links to detail, `:core:nav` |

The route codec moved with `ContentId`: it is the destination's data contract, not a
content-detail-feature detail, and `:core:nav` must decode a route without depending on a feature.
Keeping the models here is what lets `FakeModelGenerator` live in the preview harness and return
real domain objects, rather than a parallel fake vocabulary.

## Tests

`ContentDetailRouteTest` pins `ContentId` validation, route encode/decode, the not-found decode,
the parent-qualified related/part IDs, the source-matched artwork seeds, and `isKnownContentId`'s
accepted namespaces.

## Module facts

- **Plugin:** `sage.kmp` + `sage.kmp.js` (multiplatform; no Compose, no Android). `ContentId` is a data class rather than a value class because `@JvmInline` does not compile for the JS target.
- **Source sets:** `src/commonMain/kotlin`, tests in `src/jvmTest/kotlin`
- **Dependencies:** `sage.common.ui.iconsApi` (`api`, for `ContentAction.icon`)
- **Consumers:** all features, `:core:nav`, `:core:ui:previews`.
