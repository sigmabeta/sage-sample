package net.sigmabeta.sage.sample.repository

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * The signed-in User as the clients see it: a stable [id] and the [username] they signed in with.
 *
 * This is the seam's own type rather than `:core:identity`'s, because this module also compiles to
 * JS and `:core:identity` (Room) does not.
 */
data class User(
    val id: String,
    val username: String,
)

/** Whether this client holds a Session, as seen by the Sign-in gate. */
sealed interface IdentityState {
    /**
     * The startup check has not answered yet (a stored token being looked up, or `GET /api/session`
     * in flight). The gate shows neither Login nor content here, so a client that is already signed
     * in does not flash the Login screen on launch.
     */
    data object Checking : IdentityState

    /** No Session: the Sign-in gate shows Login. */
    data object SignedOut : IdentityState

    /** Signed in as [user]. */
    data class SignedIn(val user: User) : IdentityState
}

/** The outcome of one Sign in attempt through [IdentityRepository.signIn]. */
sealed interface SignInOutcome {
    /** Signed in as [user]; [IdentityRepository.state] is now [IdentityState.SignedIn]. */
    data class Accepted(val user: User) : SignInOutcome

    /** Wrong username or password; deliberately does not say which. */
    data object Rejected : SignInOutcome

    /** Too many recent failures; the same attempt may succeed after [retryAfter]. */
    data class Throttled(val retryAfter: Duration) : SignInOutcome

    /** No answer from the identity source (network failure or a server error); try again later. */
    data object Unreachable : SignInOutcome
}

/**
 * The client's Session, behind one seam for every app: the Android and desktop apps bind the
 * in-process implementation in `:core:repository:demo`, the browser binds the HTTP one in
 * `:core:repository:remote`.
 *
 * [state] starts at [IdentityState.Checking] and settles on its own once the startup check answers.
 * It also drops to [IdentityState.SignedOut] whenever the identity source refuses the Session
 * (a content call answered 401 on the web), so the gate can show Login over the current route.
 */
interface IdentityRepository {
    val state: StateFlow<IdentityState>

    /** Signs in as [username]/[password] (matched exactly, nothing trimmed). */
    suspend fun signIn(username: String, password: String): SignInOutcome

    /** Ends this client's Session. Always leaves [state] at [IdentityState.SignedOut]. */
    suspend fun signOut()
}
