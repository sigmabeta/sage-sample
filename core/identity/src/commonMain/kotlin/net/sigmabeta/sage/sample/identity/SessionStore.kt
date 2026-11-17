package net.sigmabeta.sage.sample.identity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** How long a Session lasts from Sign in. Absolute: using a Session does not extend it. */
val SESSION_LIFETIME: Duration = 30.days

/** One client's proof that it is signed in as [user], valid from [createdAt] until [expiresAt]. */
data class Session(
    val user: User,
    val createdAt: Instant,
    val expiresAt: Instant,
)

/**
 * A Session just created, with the raw [token] the client presents from now on. This is the only
 * time the token exists outside the client: the store keeps a hash of it.
 */
data class IssuedSession(
    val token: String,
    val session: Session,
)

/**
 * The Sessions every User holds. A User may hold many at once (one per device or browser); each is
 * named by an opaque token and ends when it is revoked or reaches [SESSION_LIFETIME].
 */
interface SessionStore {
    /** Signs [user] in with a new Session alongside any they already hold. */
    suspend fun create(user: User): IssuedSession

    /** The live Session [token] names, or `null` for an unknown, revoked or expired token. */
    suspend fun lookup(token: String): Session?

    /** Ends the Session [token] names (Sign out); an unknown token is a no-op. */
    suspend fun revoke(token: String)
}
