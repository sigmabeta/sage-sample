# `:apps:server`

A stateless Ktor/Netty server that serves the browser bundle and the sample's JSON API. Every
response comes from the deterministic generators in `:core:repository:demo`; there is no database,
scan, or background work.

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
| `GET /api/health` | `{"ok":true}` |
| `GET /api/home/count?seed=` | The seed's catalog size |
| `GET /api/home/item?seed=&index=` | The item or `{"item":null}`; 400 for a missing/invalid index |
| `GET /api/home/grid?seed=&limit=` | The first `limit` items |
| `GET /api/content?id=` | `{"detail":…}`; `null` for an unknown surface or malformed id |
| `GET /api/list-items?offset=&limit=` | A clamped page of the 25-row List Items catalog |
| `GET /api/grid-of-images?offset=&limit=` | A clamped page of the 50-row grid catalog |
| `GET /api/search?q=&kind=list\|grid` | Deterministic results; 400 for an invalid kind |
| Any other `/api…` | JSON 404 `{"error":"Not found"}` (never the SPA shell) |
| App paths (`/`, `/settings`, `/browse/list-items`, …) | `static/index.html` |
| Missing assets (paths with an extension) | Real 404 |

`SampleApiRoutes`/`SampleApiParams` (in `:core:repository:api`) are the single source of truth for
paths and query parameter names, shared with `:core:repository:remote`.

## Config

`ServerConfig.fromEnv()` reads `SAGE_SAMPLE_PORT` (default 8080) and `SAGE_SAMPLE_BIND_HOST`
(default 0.0.0.0). Pointing a phone on the LAN at the server works, but generated art is drawn
client-side and there is no auth.

## Files

| File | Role |
|---|---|
| `Main.kt` | Reads config and starts `embeddedServer(Netty)`. |
| `SampleServer.kt` | `configureSampleServer()`: content negotiation, status pages, API routes, JSON 404, bundle + SPA fallback. Shared by `main()` and the tests. |
| `ServerConfig.kt` | Env-var config. |

## Tests

`SampleServerTest` mounts the shipped route stack in Ktor's `testApplication` and covers health,
home paging/validation, list paging/clamping, content detail nulls, deterministic search, JSON API
404s, and missing-asset 404s.
