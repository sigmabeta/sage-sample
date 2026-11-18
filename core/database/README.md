# `:core:database`

The sample's durable store for Users and their Sessions: a Room KMP database over the bundled SQLite
driver, the same stack chipbox uses. It holds rows only; password hashing, token generation, expiry
and every other credential rule belong to `:core:identity`, which is this module's only intended
caller. See `docs/adr/0001-server-held-users-and-sessions.md` and `GLOSSARY.md`.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)   // Android + JVM; no sage.kmp.js — Room has no JS target
    alias(libs.plugins.ksp)        // room-compiler, run per target (kspAndroid, kspJvm)
}
```

Nothing in the browser app may depend on this module.

## Schema (version 2)

| Table | Columns | Constraints |
|---|---|---|
| `users` | `id`, `username`, `password_hash`, `salt`, `kdf_algorithm`, `kdf_iterations` | `id` primary key (stable, chosen by the caller); `username` unique |
| `sessions` | `token_hash`, `user_id`, `created_at`, `expires_at` | `token_hash` primary key; `user_id` → `users.id` (cascade on delete), indexed; many rows per User |
| `favorites` | `user_id`, `content_id`, `created_at` | primary key (`user_id`, `content_id`); `user_id` → `users.id` (cascade on delete); added in version 2 |

The verifier and token columns are opaque strings encoded by `:core:identity`; only hashes of
Session tokens are stored. Times are epoch milliseconds.

The schema JSON is exported by KSP (`room.schemaLocation`) to `schemas/` and committed. A schema
change bumps the version, commits the new JSON next to the old one, and ships a migration: version 2 is an
`AutoMigration(from = 1, to = 2)` that Room generates from the two committed JSON files. There is
no `fallbackToDestructiveMigration` anywhere: these rows are the identity a User signs in with.

## Contents

| Type | Source set | Role |
|---|---|---|
| `SampleDatabase` | `commonMain` | `@Database` with `userDao()` / `sessionDao()`; `SampleDatabaseConstructor` actuals are KSP-generated per target |
| `UserEntity`, `UserDao` | `commonMain` | insert (aborts on a taken id or username), find by username, find by id |
| `FavoriteEntity`, `FavoriteDao` | `commonMain` | add (an existing favorite is kept), remove, list a User's favorites oldest first; `content_id` is a `ContentId` string |
| `SessionEntity`, `SessionDao` | `commonMain` | insert (aborts on a reused hash or unknown User), find by token hash, list a User's Sessions oldest first, delete by token hash |
| `SampleDatabases` | `jvmMain` | `inMemory()` for tests and `file(path)` for a durable database, both on `BundledSQLiteDriver` with `Dispatchers.IO` queries |
| `AndroidSampleDatabases` | `androidMain` | `file(context, name = "sage-sample.db")`: a durable database under `context.getDatabasePath(name)`, same driver and query context |

The two builder objects are deliberately platform-only. A JVM library that Android also ships (such as
`:core:repository:demo`) must not call `SampleDatabases`: the Android variant lacks it. Let the app
provide the `SampleDatabase` instead.

## Tests

`src/jvmTest` (JUnit 4) round-trips every DAO through `SampleDatabases.inMemory()`, checks that a
file database keeps its rows across reopening, and `MigrationTest` writes a real version 1 file (the
committed `1.json`, raw SQL, `user_version = 1`) and opens it with the current database to prove the
Users and Sessions survive and favorites work:

```sh
./gradlew :core:database:jvmTest
```
