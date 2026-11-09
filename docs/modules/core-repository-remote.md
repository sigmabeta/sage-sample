# `:core:repository:remote`

HTTP implementations of the repository seams, bound by the browser app.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}
```

## Pieces

| Type | Role |
|---|---|
| `SampleApiClient` | Typed GET over an `HttpClient`: appends `SampleApiParams`, decodes JSON, and throws the server's message for non-2xx responses. |
| `RemoteHomeRepository` | Count/item/grid endpoints; reads the Settings demo outcome per call. |
| `RemoteContentDetailRepository` | `/api/content`; a null wire detail is the not-found state. |
| `RemoteSampleItemRepository` / `RemoteGridOfImagesRepository` | Paged item endpoints. |
| `RemoteSearchRepository` | `/api/search` with `q` + `kind`. |

All five are `@SingleIn` + `@ContributesBinding(AppScope)` + `@Inject`. `SampleApiClient` is
provided by the browser app (`WebSampleBindings`), which chooses the base URL (page origin, or a
`?api=` development override) and installs JSON content negotiation.

The Settings demo outcome is applied client-side: `EMPTY` returns an empty success and `ERROR` fails
the flow without issuing a request. The server always returns content.

## Dependencies

`:core:repository:api` (seams + wire), `:core:settings` (demo outcome), Ktor client core +
content negotiation + kotlinx JSON. The JS engine (`ktor-client-js`) is added only when
`-Psage.js` configures `jsMain`.

## Tests

`RemoteRepositoriesTest` runs on the JVM against Ktor's `MockEngine` and covers endpoint paths and
query parameters, wire decoding, the client-side outcome branches (no request in EMPTY/ERROR), and
non-2xx error messages.
