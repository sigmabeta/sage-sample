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
| `SampleApiClient` | Typed GET over an `HttpClient`: appends `SampleApiParams`, decodes JSON, and throws the server's message for non-2xx responses. A 401 is not thrown: it reports the refusal to `RemoteSessionState`, waits until Signed in again and retries, so the screen stays loading and recovers without an error row. |
| `RemoteSessionState` | The shared `IdentityState` (`@SingleIn` + `@Inject`); `rejected(sentWith)` ignores a 401 for a Session older than the current one. |
| `RemoteIdentityRepository` | `IdentityRepository` over `/api/session`: `GET` at startup (200 → Signed in; 401 or no answer → Signed out), JSON `POST` to Sign in (200/401/429 + `Retry-After`; 5xx, other statuses and network failures are `Unreachable`), JSON `DELETE` to Sign out (Signed out locally whatever the answer). |
| `RemoteHomeRepository` | Count/item/grid endpoints; reads the Settings demo outcome per call. |
| `RemoteContentDetailRepository` | `/api/content`; a null wire detail is the not-found state. |
| `RemoteSampleItemRepository` / `RemoteGridOfImagesRepository` | Paged item endpoints. |
| `RemoteSearchRepository` | `/api/search` with `q` + `kind`. |

The repositories are `@SingleIn` + `@ContributesBinding(AppScope)` + `@Inject`. `SampleApiClient` is
provided by the browser app (`WebSampleBindings`) with the page origin as its base URL, the
`RemoteSessionState`, and JSON content negotiation. The API is same-origin only: the Session is an
HttpOnly `SameSite=Lax` cookie the browser sends, and Kotlin never handles the token.

The Settings demo outcome is applied client-side: `EMPTY` returns an empty success and `ERROR` fails
the flow without issuing a request. The server always returns content.

## Dependencies

`:core:repository:api` (seams + wire), `:core:settings` (demo outcome), Ktor client core +
content negotiation + kotlinx JSON. The JS engine (`ktor-client-js`) is added only when
`-Psage.js` configures `jsMain`.

## Tests

`RemoteRepositoriesTest` runs on the JVM against Ktor's `MockEngine` and covers endpoint paths and
query parameters, wire decoding, the client-side outcome branches (no request in EMPTY/ERROR), and
non-2xx error messages. `RemoteIdentityRepositoryTest` covers the startup check (200, 401,
unreachable), every Sign in outcome, Sign out, and a content call's 401 → Signed out → retried after
Sign in.
