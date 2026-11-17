# `:apps:server`

A Ktor/Netty server that serves the browser bundle and the sample's JSON API. Content comes from
the deterministic generators in `:core:repository:demo`. The server holds Users and Sessions in a
Room database file through `:core:identity` (ADR 0001), and every content route needs a Session.
There is no scan or background work.

## Plugins

```kotlin
plugins {
    alias(libs.plugins.sage.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}
```

`mainClass = net.sigmabeta.sage.sample.server.MainKt`, application name `sage-sample-server`.

## Bundle wiring

- `:apps:server:run -Psage.js=true` runs `copyJsBundle`, which depends on
  `:apps:js:jsBrowserDevelopmentExecutableDistribution` and copies it into
  `build/generated/static/static` (a resource root).
- `installDist` / `distZip` / `distTar` depend on `installProductionJsBundle`, which copies the
  production bundle and fails loudly without `-Psage.js=true`.
- Without `-Psage.js`, the server is API-only; static requests return 503 with a message.

## Routes

| Route | Behaviour |
|---|---|
| `GET /api/health` | `{"ok":true}` (public) |
| `POST /api/session` | Sign in: 200 `{"user":{"id","username"}}` + `sage_session` cookie; 401 `{"error":"Wrong username or password"}`; 429 + `Retry-After`; 400 for an unreadable body (public) |
| `GET /api/session` | 200 `{"user":…}` or 401 `{"error":"Signed out"}` (public) |
| `DELETE /api/session` | Sign out: 204, revokes and clears the cookie, also when Signed out (public) |
| `GET /api/home/count?seed=` | The seed's catalog size |
| `GET /api/home/item?seed=&index=` | The item or `{"item":null}`; 400 for a missing/invalid index |
| `GET /api/home/grid?seed=&limit=` | The first `limit` items |
| `GET /api/content?id=` | `{"detail":…}`; `null` for an unknown surface or malformed id |
| `GET /api/list-items?offset=&limit=` | A clamped page of the 25-row List Items catalog |
| `GET /api/grid-of-images?offset=&limit=` | A clamped page of the 50-row grid catalog |
| `GET /api/search?q=&kind=list\|grid` | Deterministic results; 400 for an invalid kind |
| Any other `/api…` | 401 when Signed out, else JSON 404 `{"error":"Not found"}` (never the SPA shell) |
| App paths (`/`, `/settings`, `/browse/list-items`, …) | `static/index.html` |
| Missing assets (paths with an extension) | Real 404 |

Every `/api` route but health and `/api/session` answers a Signed out request with 401
`{"error":"Sign in required"}`; the SPA shell and assets stay public. `POST`/`DELETE /api/session`
without `Content-Type: application/json` are 415. The cookie is `HttpOnly`, `SameSite=Lax`,
`Path=/`, `Max-Age` 30 days, and `Secure` iff `SAGE_SAMPLE_SECURE_COOKIE`.

`SampleApiRoutes`/`SampleApiParams` and the wire DTOs (`SignInRequest`, `SessionResponse`,
`UserDto`, `ApiErrorResponse`), all in `:core:repository:api`, are the single source of truth for
paths, parameter names and bodies, shared with `:core:repository:remote`.

## Config

`ServerConfig.fromEnv()` reads `SAGE_SAMPLE_PORT` (default 8080), `SAGE_SAMPLE_BIND_HOST`
(default 0.0.0.0), `SAGE_SAMPLE_DB_PATH` (default `sage-sample.db` in the working directory) and
`SAGE_SAMPLE_SECURE_COOKIE` (default off). Startup seeds the Demo accounts and warns when a
non-loopback bind runs with the secure-cookie flag off (`warnsOfInsecureCookie()`). Pointing a phone
on the LAN at the server works; generated art is drawn client-side.

## Files

| File | Role |
|---|---|
| `Main.kt` | Reads config, opens the database, seeds the Demo accounts, starts `embeddedServer(Netty)`. |
| `SampleServer.kt` | `configureSampleServer(identity, secureCookie)`: content negotiation, status pages, API routes, JSON 404, bundle + SPA fallback. Shared by `main()` and the tests. |
| `SessionRoutes.kt` | `ServerIdentity` (+ `overRoom`), the `/api/session` routes, the `sage_session` cookie. |
| `SessionGate.kt` | `requireSession {}`: a transparent route node whose plugin 401s a Signed out request. |
| `ServerConfig.kt` | Env-var config and the insecure-cookie warning predicate. |

## Tests

The tests mount the shipped route stack in Ktor's `testApplication`. `SessionRoutesTest` covers each
Session status and the cookie flags over the `:core:identity:fake` fakes; `ApiGateTest` the 401
gate, public routes and gated unknown paths; `SampleServerTest` (signed in) health, home
paging/validation, list paging/clamping, content detail nulls, deterministic search and
missing-asset 404s; `RoomIdentityServerTest` the Room-backed identity end to end; `ServerConfigTest`
the env vars and the warning predicate.
