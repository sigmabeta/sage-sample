package net.sigmabeta.sage.sample.identity.fake

import net.sigmabeta.sage.sample.identity.IssuedSession
import net.sigmabeta.sage.sample.identity.SESSION_LIFETIME
import net.sigmabeta.sage.sample.identity.Session
import net.sigmabeta.sage.sample.identity.SessionStore
import net.sigmabeta.sage.sample.identity.User
import net.sigmabeta.sage.sample.identity.UserDirectory
import kotlin.time.Clock
import kotlin.time.Instant

/** A clock a test sets by hand. */
class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

/**
 * An in-memory [UserDirectory] for tests: plaintext passwords, exact matching, and readable ids
 * (`user-1`, `user-2`, ... in registration order). No hashing, so it is fast and deterministic.
 */
class FakeUserDirectory : UserDirectory {
    private val lock = Any()
    private val byUsername = mutableMapOf<String, Pair<User, String>>()

    override suspend fun verify(username: String, password: String): User? = synchronized(lock) {
        byUsername[username]?.takeIf { (_, stored) -> stored == password }?.first
    }

    override suspend fun register(username: String, password: String): User? = synchronized(lock) {
        if (username in byUsername) return null
        val user = User(id = "user-${byUsername.size + 1}", username = username)
        byUsername[username] = user to password
        user
    }

    override suspend fun findById(id: String): User? = synchronized(lock) {
        byUsername.values.firstOrNull { (user, _) -> user.id == id }?.first
    }
}

/**
 * An in-memory [SessionStore] for tests with readable tokens (`session-1`, `session-2`, ...). It
 * keeps the real rules a route can observe: [SESSION_LIFETIME] absolute expiry against [clock],
 * several Sessions per User, and revocation.
 */
class FakeSessionStore(private val clock: Clock = Clock.System) : SessionStore {
    private val lock = Any()
    private val byToken = mutableMapOf<String, Session>()
    private var issued = 0

    override suspend fun create(user: User): IssuedSession = synchronized(lock) {
        val createdAt = clock.now()
        val session = Session(user = user, createdAt = createdAt, expiresAt = createdAt + SESSION_LIFETIME)
        val token = "session-${++issued}"
        byToken[token] = session
        IssuedSession(token = token, session = session)
    }

    override suspend fun lookup(token: String): Session? = synchronized(lock) {
        val session = byToken[token] ?: return null
        if (clock.now() >= session.expiresAt) {
            byToken.remove(token)
            return null
        }
        session
    }

    override suspend fun revoke(token: String) {
        synchronized(lock) { byToken.remove(token) }
    }
}
