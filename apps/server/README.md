# `:apps:server`

A stateless Ktor/Netty server that serves the `:apps:js` bundle and the sample's JSON API. Every
response comes from `:core:repository:demo`'s deterministic generators.

```sh
SAGE_SAMPLE_PORT=8080 ./gradlew :apps:server:run -Psage.js=true
```

`run` copies the development bundle before starting; `installDist`/`distZip`/`distTar` copy the
production bundle and fail without `-Psage.js=true`. Without the flag the server still serves the
API, and static requests return a 503 explaining how to build the bundle.

## Config

| Variable | Default |
|---|---|
| `SAGE_SAMPLE_PORT` | `8080` |
| `SAGE_SAMPLE_BIND_HOST` | `0.0.0.0` |

## Routes

- `GET /api/health` → `{"ok":true}`
- `GET /api/home/{count,item,grid}` — the Home demo catalog
- `GET /api/content?id=` — Content detail (`null` for unknown/malformed ids)
- `GET /api/list-items` / `GET /api/grid-of-images` — paged demo rows
- `GET /api/search?q=&kind=list|grid` — deterministic results
- Unknown `/api…` → JSON 404; app paths → `index.html`; missing assets → real 404

`configureSampleServer()` in `SampleServer.kt` is mounted by both `main()` and the tests.

## Tests

`./gradlew :apps:server:test` runs the route tests through Ktor's `testApplication`.
