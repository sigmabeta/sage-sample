# Server-held Users and Sessions behind a whole-app Sign-in gate

The sample was a stateless demo with no credentials. Real SAGE apps (chipbox first) need User-owned
data such as favorites that follow a User across devices, so the sample now models that shape
instead of a gate-only "who may use this install" check. Users and Sessions live in Room KMP over
bundled SQLite (`:core:database`, the same stack chipbox uses), behind `UserDirectory` and
`SessionStore` seams in a shared `:core:identity` module that both `apps:server` and the
Android/desktop in-process identity source call, so the credential rules exist once. Every app shows
the same Sign-in gate above the tabs.

## Considered Options

- **In-memory server stores**: rejected; a copying app needs the durable shape, not a demo that
  forgets every Session on restart.
- **Signed stateless cookie**: rejected; no server-side revocation, and it needs a signing secret.
- **Bearer token in `localStorage`**: rejected for the web client in favour of an HttpOnly,
  `SameSite=Lax` cookie, which keeps the token away from page script. This cost the `?api=`
  cross-origin dev override, which was removed.
- **Gate-free benchmark variant / UI sign-in in benchmark setup**: rejected for a launch extra that
  signs in a Demo account through the identity seam, compiled into the `benchmark` build type only.

## Consequences

- `apps:server` is no longer stateless: it owns a database file (`SAGE_SAMPLE_DB_PATH`) with a
  committed, migrated schema.
- User-scoped routes live under `/api/me/...` and resolve the User from the Session only; no route
  accepts a user id.
- Chipbox's ADR-0003 ("no accounts, state in `localStorage`") must be superseded in chipbox before
  it adopts this pattern; this ADR does not decide that for chipbox.
