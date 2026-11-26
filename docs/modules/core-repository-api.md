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
| `IdentityRepository` | `state: StateFlow<IdentityState>`, `suspend signIn(username, password): SignInOutcome`, `suspend signOut()` | Login, the Sign-in gate, Settings |

`itemCount`/`itemAt` are suspend because the browser's implementation performs HTTP; the demo
implementation is still a direct call. The list calls carry operation-name constants and catalog
sizes (`LIST_ITEMS_*`, `GRID_OF_IMAGES_*`) so features and implementations agree.

`SearchResultKind`, `searchSeedFor`, `normalizeSearchQuery`, the operation names and the result
bounds are here too: the demo generator, the HTTP client and Search's artwork/content-id derivation
all use the same deterministic seed.

## Identity

`IdentityRepository` is the client's Session behind one seam (vocabulary from `GLOSSARY.md`).
`IdentityState` is `Checking` (the startup check has not answered; the gate shows neither Login nor
content, so a signed-in client does not flash Login), `SignedOut`, or `SignedIn(user: User)`.
`SignInOutcome` is `Accepted(user)`, `Rejected`, `Throttled(retryAfter: Duration)` or `Unreachable`.
`User(id, username)` is this module's own type, not `:core:identity`'s, because this module compiles
to JS. `state` also drops to `SignedOut` on its own when the identity source refuses the Session (a
content call answered 401 on the web); no screen reports that itself.

## Wire models

`RepositoryWire.kt` defines `@Serializable` DTOs separate from the domain models. The one place
they differ materially is icons: a `ContentAction` carries SAGE's sealed `Icon` in the app and a
stable wire name (for example `"play"`) on the wire. `Icon.wireName()` / `iconFromWireName()`
convert in both directions, and an unknown name falls back to `Icon.Description` rather than
failing a screen.

`SampleApiRoutes` and `SampleApiParams` own every path and query parameter name. `normalizedPage`
clamps `offset`/`limit` for both sides. `toDto`/`toDomain`/`toDomainOrNull` are shared so the
server and client cannot drift.

The Session resource, `SampleApiRoutes.SESSION` (`/api/session`), takes a `SignInRequest(username,
password)` and answers with `SessionResponse(user: UserDto(id, username))`; failures are
`ApiErrorResponse(error)`. The token itself rides in an HttpOnly cookie the server sets, so no DTO
carries it. See `docs/modules/apps-server.md` for the statuses.

## Tests

`RepositoryWireTest` round-trips every row and a full `ContentDetail`, checks every icon's wire name
round trip, asserts malformed content ids decode to `null`, and pins the paging bounds.
