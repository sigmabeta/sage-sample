package net.sigmabeta.sage.sample.repository.remote

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.User

/**
 * The browser client's identity state, shared by [RemoteIdentityRepository] (which answers the
 * startup check, Sign in and Sign out) and [SampleApiClient] (which reports a content call the
 * server answered 401). It is its own object so the client and the repository need not depend on
 * each other.
 */
@SingleIn(AppScope::class)
@Inject
class RemoteSessionState {
    private val mutableState = MutableStateFlow<IdentityState>(IdentityState.Checking)
    val state: StateFlow<IdentityState> = mutableState.asStateFlow()

    /**
     * The server refused the Session a request was sent with ([sentWith], the state when it was
     * sent): go Signed out, unless the client has signed in again since, in which case the refusal
     * was for the old Session and the new one stands.
     */
    fun rejected(sentWith: IdentityState) {
        if (sentWith is IdentityState.SignedOut) {
            mutableState.compareAndSet(IdentityState.Checking, IdentityState.SignedOut)
        } else {
            mutableState.compareAndSet(sentWith, IdentityState.SignedOut)
        }
    }

    /** Suspends until the client is Signed in. */
    suspend fun awaitSignedIn() {
        state.first { it is IdentityState.SignedIn }
    }

    /** The startup check answered; ignored if Sign in or a 401 already settled the state. */
    internal fun settled(state: IdentityState) {
        mutableState.compareAndSet(IdentityState.Checking, state)
    }

    internal fun signedIn(user: User) {
        mutableState.value = IdentityState.SignedIn(user)
    }

    internal fun signedOut() {
        mutableState.value = IdentityState.SignedOut
    }
}
