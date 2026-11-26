# `:core:repository:cache`

The User-keyed favorites cache that both favorites implementations put in front of their store.
`:core:repository:api` keeps only the `FavoritesRepository` seam; this module holds the concrete
cache, so the seams module stays interfaces, wire DTOs and route constants.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)
    alias(libs.plugins.sage.kmp.js)
}
```

Multiplatform because its two users are on different targets: `:core:repository:demo` (JVM, Room
rows) and `:core:repository:remote` (KMP including JS, `/api/me/favorites`). Both take it as an
`implementation` dependency; features never see it.

## Types

- `FavoritesSource` — where favorites are really kept: `suspend load(user)` and
  `suspend set(user, contentId, favorite)` (throws when the change cannot be saved).
- `CachedFavoritesRepository(identity, source, scope)` — a `FavoritesRepository` holding one User's
  set in memory. It is app-scoped and outlives a Sign out, so it is keyed by User id: the held set
  is dropped whenever `identity` leaves `SignedIn` or switches User, and a write is optimistic with
  a rollback when the source throws.

## Tests

`CachedFavoritesRepositoryTest` (jvmTest): Signed out has no favorites and cannot change any; a
Signed in User sees what the source holds for them; (un-)favoriting reaches the source and the flow;
a source failure reverts the change and returns `false`; signing in as another User never shows the
previous User's set; and signing out drops the cache so the next Sign in reads the source again.
