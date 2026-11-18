package net.sigmabeta.sage.sample

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.sample.repository.IdentityRepository

/**
 * The debug and release builds' stand-in for the benchmark build's launch hook (`src/benchmark`):
 * it passes the real identity repository through and ignores every intent. Same name and shape in
 * both source sets, so `MainActivity` is one file for every build type.
 */
@Suppress("UNUSED_PARAMETER")
internal class BenchmarkLaunchHook(identity: IdentityRepository, scope: CoroutineScope) {
    val identityRepository: IdentityRepository = identity

    @Suppress("UNUSED_PARAMETER")
    fun onIntent(intent: Intent?) = Unit
}
