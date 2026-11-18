package net.sigmabeta.sage.sample

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.sample.repository.IdentityRepository

/**
 * The `benchmark` build's launch hook as `MainActivity` sees it (debug and release have a no-op of
 * the same name in `src/nonBenchmark`): the gate reads [gatedIdentity], and [onIntent] turns a
 * [BenchmarkSignIn.EXTRA_SIGN_IN] launch extra into a Sign in.
 */
internal class BenchmarkLaunchHook(identity: IdentityRepository, scope: CoroutineScope) {
    private val signIn = BenchmarkSignIn(identity, scope)

    val gatedIdentity: IdentityRepository = signIn.gatedIdentity

    fun onIntent(intent: Intent?) {
        BenchmarkSignIn.accountFor(intent?.getStringExtra(BenchmarkSignIn.EXTRA_SIGN_IN))?.let(signIn::request)
    }
}
