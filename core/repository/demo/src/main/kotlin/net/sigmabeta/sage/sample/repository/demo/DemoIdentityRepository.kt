package net.sigmabeta.sage.sample.repository.demo

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.SessionStore
import net.sigmabeta.sage.sample.identity.SignInResult
import net.sigmabeta.sage.sample.identity.SignInService
import net.sigmabeta.sage.sample.identity.UserDirectory
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import net.sigmabeta.sage.storage.common.Storage
import net.sigmabeta.sage.sample.identity.User as IdentityUser

/**
 * The Android and desktop apps' [IdentityRepository]: `:core:identity`'s rules run in-process, so
 * the credential rules are the same ones `apps:server` applies.
 *
 * The Session token is kept in SAGE [Storage] so the Session survives a restart. At startup the
 * repository seeds the Demo accounts and looks the stored token up: a live Session is
 * [IdentityState.SignedIn], and an unknown, revoked or expired one is cleared and
 * [IdentityState.SignedOut]. Seeding and Sign in run PBKDF2 (hundreds of thousands of rounds), so
 * they run on [SageDispatchers.computation], never on the caller's thread.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoIdentityRepository(
    private val users: UserDirectory,
    private val sessions: SessionStore,
    private val signInService: SignInService,
    private val storage: Storage,
    scope: CoroutineScope,
    private val dispatchers: SageDispatchers,
) : IdentityRepository {
    private val mutableState = MutableStateFlow<IdentityState>(IdentityState.Checking)
    override val state: StateFlow<IdentityState> = mutableState.asStateFlow()

    private val seeded = scope.async(dispatchers.computation) { DemoAccounts.seed(users) }

    private val startup = scope.launch(dispatchers.disk) {
        val token = storedToken() ?: return@launch run { mutableState.value = IdentityState.SignedOut }
        val session = sessions.lookup(token)
        if (session == null) clearToken()
        mutableState.value = session?.let { IdentityState.SignedIn(it.user.toSeam()) } ?: IdentityState.SignedOut
    }

    override suspend fun signIn(username: String, password: String): SignInOutcome {
        // Sign in waits for startup, so the startup check can never clear or overwrite its token.
        startup.join()
        seeded.await()
        val result = withContext(dispatchers.computation) {
            signInService.signIn(username, password, client = LOCAL_CLIENT)
        }
        return when (result) {
            is SignInResult.SignedIn -> {
                storage.saveString(KEY_SESSION_TOKEN, result.issued.token)
                val user = result.issued.session.user.toSeam()
                mutableState.value = IdentityState.SignedIn(user)
                SignInOutcome.Accepted(user)
            }

            SignInResult.Rejected -> SignInOutcome.Rejected

            is SignInResult.Throttled -> SignInOutcome.Throttled(result.retryAfter)
        }
    }

    override suspend fun signOut() {
        startup.join()
        storedToken()?.let { token -> withContext(dispatchers.disk) { sessions.revoke(token) } }
        clearToken()
        mutableState.value = IdentityState.SignedOut
    }

    private suspend fun storedToken(): String? =
        storage.savedStringFlow(KEY_SESSION_TOKEN).first()?.takeIf { it.isNotEmpty() }

    /** [Storage] has no remove, so an empty string is "no token". */
    private fun clearToken() = storage.saveString(KEY_SESSION_TOKEN, "")

    internal companion object {
        const val KEY_SESSION_TOKEN = "identity.session_token"

        /** The limiter's client key: an in-process caller has one client, this install. */
        const val LOCAL_CLIENT = "local"
    }
}

private fun IdentityUser.toSeam(): User = User(id = id, username = username)
