# SAGE Sample

The reference app real SAGE apps copy: shared features and navigation over deterministic demo
content, shipped as Android, desktop, and browser clients.

## Language

### Identity

**User**:
A durable identity with a stable id that owns user-scoped data, the same User on every device they
sign in from.
_Avoid_: Account (except in **Demo account**), principal, member, profile

**Session**:
One client's proof that it is signed in as a particular **User**. A User may hold several Sessions
at once (one per device or browser); a client holds at most one.
_Avoid_: Login, token, auth state

**User-scoped data**:
Content that belongs to one **User** and is resolved from the **Session**, never from an id the
client supplies.
_Avoid_: Personal data, my data

**Signed in / Signed out**:
The two states a client is in: whether it currently holds a **Session**.
_Avoid_: Logged in, authenticated, authorized

**Sign in / Sign out**:
The acts that create and end a **Session**.
_Avoid_: Log in, log out, login (as a verb)

**Demo account**:
One of the two published username/password pairs the sample seeds as **Users**. They are
placeholder identity, not real credentials, the way generated bitmaps are placeholder art; two exist
so that separate Users' data can be seen side by side.
_Avoid_: Test user, admin, default user

**Sign-in gate**:
The rule that a **Signed out** client sees only the Login screen and the server refuses its content
requests.
_Avoid_: Auth wall, login wall, guard
