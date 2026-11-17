# `:apps:server`

A Ktor/Netty server that serves the `:apps:js` bundle and the sample's JSON API. Content comes from
`:core:repository:demo`'s deterministic generators; the server's own state is its Users and
Sessions, held in a Room database file through `:core:identity` (see
`docs/adr/0001-server-held-users-and-sessions.md`). Every content route needs a Session.

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
| `SAGE_SAMPLE_DB_PATH` | `sage-sample.db` in the working directory |
| `SAGE_SAMPLE_SECURE_COOKIE` | off; `true`/`1`/`yes`/`on` turn it on |

Startup opens the database, seeds the Demo accounts (`demo`/`sage`, `demo2`/`sage`; idempotent),
and logs a warning when it binds a non-loopback address with the secure-cookie flag off. Turn the
flag on whenever the server sits behind TLS.

## Routes

- `GET /api/health` → `{"ok":true}` (public)
- `/api/session` (public) — see below
- `GET /api/home/{count,item,grid}` — the Home demo catalog
- `GET /api/content?id=` — Content detail (`null` for unknown/malformed ids)
- `GET /api/list-items` / `GET /api/grid-of-images` — paged demo rows
- `GET /api/search?q=&kind=list|grid` — deterministic results
- Unknown `/api…` → 401 when Signed out, else JSON 404; app paths → `index.html`; missing assets →
  real 404. The SPA shell and assets never need a Session.

Every `/api` route except health and the Session resource answers a Signed out request with
`401 {"error":"Sign in required"}`.

### Session resource

The Session token travels in the `sage_session` cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`,
`Max-Age` = 30 days (the Session's absolute expiry), and `Secure` iff `SAGE_SAMPLE_SECURE_COOKIE`.

| Request | Response |
|---|---|
| `POST /api/session` `{"username","password"}` | 200 `{"user":{"id","username"}}` + `Set-Cookie` |
| — wrong username or password | 401 `{"error":"Wrong username or password"}` (same for both) |
| — too many recent failures | 429 `{"error":…}` + `Retry-After` (whole seconds, rounded up) |
| — unreadable body | 400 `{"error":"Malformed request body"}` |
| `GET /api/session` | 200 `{"user":…}`, or 401 `{"error":"Signed out"}` |
| `DELETE /api/session` | 204; revokes the Session and clears the cookie (`Max-Age=0`), even when Signed out |
| `POST`/`DELETE` without `Content-Type: application/json` | 415 `{"error":"Expected application/json"}` |

A `charset` parameter on the content type is fine. Requiring JSON on `DELETE` too means a cross-site
page cannot Sign a User out with a plain form post.

`configureSampleServer(identity, secureCookie)` in `SampleServer.kt` is mounted by both `main()`
(over `ServerIdentity.overRoom(SampleDatabases.file(dbPath))`) and the tests (over the
`:core:identity:fake` fakes, or an in-memory Room database).

## Tests

`./gradlew :apps:server:test` runs the route tests through Ktor's `testApplication`:
`SessionRoutesTest` (each Session status and the cookie flags), `ApiGateTest` (the 401 gate),
`SampleServerTest` (content, signed in), `RoomIdentityServerTest` (Room end to end) and
`ServerConfigTest`.
