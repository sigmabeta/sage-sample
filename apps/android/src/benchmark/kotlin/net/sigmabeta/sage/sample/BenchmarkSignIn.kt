package net.sigmabeta.sage.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.sigmabeta.sage.sample.identity.DemoAccount
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState

/**
 * The `benchmark` build type's launch hook: signs a Demo account in through [identity] (a real
 * Session, not a gate bypass) so a benchmark launch lands on its screen instead of on Login.
 *
 * This file lives in `src/benchmark`, so release and debug builds do not contain it
 * (`NoBenchmarkHookTest` asserts that).
 *
 * The Sign-in gate reads [gatedIdentity], not [identity]. While a requested Sign in is unfinished,
 * [gatedIdentity] reports [IdentityState.Checking] instead of [IdentityState.SignedOut], so the gate
 * keeps its blank surface and a measured launch never draws Login. If the Sign in is rejected, the
 * real state shows through rather than leaving the gate blank forever.
 */
internal class BenchmarkSignIn(
    private val identity: IdentityRepository,
    private val scope: CoroutineScope,
) {
    private val pending = MutableStateFlow(false)
    private var job: Job? = null

    /** What the gate observes: [identity], except that a Signed out in the middle of a Sign in is Checking. */
    val gatedIdentity: IdentityRepository = object : IdentityRepository by identity {
        override val state: StateFlow<IdentityState> = combine(identity.state, pending) { state, signingIn ->
            if (signingIn && state == IdentityState.SignedOut) IdentityState.Checking else state
        }.stateIn(scope, SharingStarted.Eagerly, IdentityState.Checking)
    }

    /**
     * Signs [account] in once the startup check has settled, unless a Session is already stored
     * (then the launch reuses it). A request while one is in flight is ignored.
     */
    fun request(account: DemoAccount) {
        if (job?.isActive == true) return
        pending.value = true
        job = scope.launch {
            try {
                val settled = identity.state.first { it !is IdentityState.Checking }
                // A rejection is not thrown: the gate then shows Login, which the benchmark's
                // landing wait reports as a failed launch.
                if (settled == IdentityState.SignedOut) identity.signIn(account.username, account.password)
            } finally {
                pending.value = false
            }
        }
    }

    companion object {
        /** The launch extra: the username of a Demo account to sign in, e.g. `--es ...signIn demo`. */
        const val EXTRA_SIGN_IN = "sagesample.benchmark.signIn"

        /** The Demo account named [username]; anything else (including no extra) is no request. */
        fun accountFor(username: String?): DemoAccount? = DemoAccounts.all.firstOrNull { it.username == username }
    }
}
