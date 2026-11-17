# `:core:identity`

The sample's credential and Session rules, written once and called by both `apps:server` and the
Android/desktop in-process identity source. It sits on `:core:database` and holds no HTTP, cookie
or Ktor code. See `docs/adr/0001-server-held-users-and-sessions.md` and `GLOSSARY.md`.

```kotlin
plugins {
    alias(libs.plugins.sage.kmp)   // Android + JVM; no sage.kmp.js — Room has no JS target
}
```

Nothing in the browser app may depend on this module.

## Contents

| Type | Source set | Role |
|---|---|---|
| `User`, `UserDirectory` | `commonMain` | `verify(username, password): User?`, `register(username, password): User?` (`null` if taken), `findById(id)` |
| `Session`, `IssuedSession`, `SessionStore`, `SESSION_LIFETIME` | `commonMain` | `create(user): IssuedSession`, `lookup(token): Session?`, `revoke(token)`; 30-day absolute expiry |
| `DemoAccount`, `DemoAccounts` | `commonMain` | `demo`/`sage` and `demo2`/`sage`; `seed(directory)` is idempotent |
| `SignInLimiter`, `SignInAllowance` | `commonMain` | in-memory sliding window; `check`, `recordFailure`, `recordSuccess` |
| `SignInService`, `SignInResult` | `commonMain` | `signIn(username, password, client)`: limiter, then password, then a new Session |
| `RoomUserDirectory` | `jvmSharedMain` | PBKDF2-HMAC-SHA256 over the `users` table |
| `RoomSessionStore` | `jvmSharedMain` | hashed tokens over the `sessions` table |

`jvmSharedMain` is the `sage.kmp` source set both the Android and JVM targets compile, so the JDK
crypto (`javax.crypto`, `java.security`) is available there and the implementations exist once.
Both take a `SampleDatabase` by constructor; this module never builds one. On the JVM use
`SampleDatabases.file(path)` (or `.inMemory()` in tests); on Android use
`AndroidSampleDatabases.file(context)`.

## Rules

- **Passwords:** PBKDF2WithHmacSHA256, a random 16-byte salt per User, a 256-bit verifier, and
  `DEFAULT_PBKDF2_ITERATIONS` (600,000) rounds. The row records the algorithm and count, and
  verification uses the row's values, so the default can be raised without touching old rows.
  Hashes compare with `MessageDigest.isEqual`. An unknown username still runs one derivation against
  a throwaway salt, so response time does not reveal which usernames exist.
- **Usernames and passwords match exactly:** case-sensitive, nothing trimmed.
- **Sessions:** a token is 32 bytes from `SecureRandom`, sent as unpadded URL-safe Base64 and
  returned only from `create`. The row is keyed by the SHA-256 of the token's UTF-8 bytes. A User
  may hold many Sessions. `lookup` checks expiry against the injected `kotlin.time.Clock` and
  deletes an expired row when it finds one; expiry is absolute, so use does not extend it.
- **Encodings:** `password_hash`, `salt` and `token_hash` are standard, padded Base64. User ids are
  random UUIDs.
- **Sign-in limiter:** failures are counted per username (exactly as typed) and per client key over
  a sliding one-minute window. Once either key holds **more than 5** failures in the window (the
  sixth failure), attempts naming it are throttled with a `retryAfter` until enough age out. It is
  checked before the password, throttled attempts are not counted, and a success clears the
  username's failures but not the client's. It is in memory, so a restart forgets it.

## Fakes

`:core:identity:fake` (plain `sage.jvm`) holds `FakeUserDirectory` (plaintext, ids `user-1`, ...),
`FakeSessionStore` (tokens `session-1`, ..., real expiry/revocation against its clock) and
`FakeClock`. Depend on it with `testImplementation` only; the real `SignInLimiter` and
`SignInService` run over the fakes.

## Tests

`src/jvmTest` (JUnit 4) runs the Room implementations over `SampleDatabases.inMemory()` with a hand-
moved clock and a low iteration count:

```sh
./gradlew :core:identity:jvmTest :core:identity:fake:test
```
