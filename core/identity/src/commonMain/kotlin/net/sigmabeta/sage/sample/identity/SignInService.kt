package net.sigmabeta.sage.sample.identity

import kotlin.time.Duration

/** The outcome of one Sign in attempt. */
sealed interface SignInResult {
    /** Signed in; hand [issued]'s token to the client. */
    data class SignedIn(val issued: IssuedSession) : SignInResult

    /** Wrong username or password; deliberately does not say which. */
    data object Rejected : SignInResult

    /** Too many recent failures for this username or client; the password was not checked. */
    data class Throttled(val retryAfter: Duration) : SignInResult
}

/**
 * Sign in as one rule, shared by the server and the in-process identity source: consult [limiter]
 * first, verify the password against [users] only if allowed, record the outcome, and open a new
 * Session in [sessions] on success. [client] keys the limiter (the caller's address, or a fixed key
 * for an in-process caller).
 *
 * A client holds at most one Session: [replacing] is the token the client already holds, if any,
 * and a successful Sign in revokes it before opening the new one. A refused attempt leaves it alone.
 */
class SignInService(
    private val users: UserDirectory,
    private val sessions: SessionStore,
    private val limiter: SignInLimiter,
) {
    suspend fun signIn(
        username: String,
        password: String,
        client: String,
        replacing: String? = null,
    ): SignInResult {
        val allowance = limiter.check(username, client)
        if (allowance is SignInAllowance.Throttled) return SignInResult.Throttled(allowance.retryAfter)

        val user = users.verify(username, password)
        if (user == null) {
            limiter.recordFailure(username, client)
            return SignInResult.Rejected
        }
        limiter.recordSuccess(username)
        replacing?.let { sessions.revoke(it) }
        return SignInResult.SignedIn(sessions.create(user))
    }
}
