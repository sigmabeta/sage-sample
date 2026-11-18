package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.sigmabeta.sage.sample.nav.SampleGateSurface
import net.sigmabeta.sage.sample.nav.SampleRoute
import net.sigmabeta.sage.sample.nav.SampleRouteUrl
import net.sigmabeta.sage.sample.nav.SampleSignInGate
import net.sigmabeta.sage.sample.repository.IdentityRepository

/**
 * Holds the app's [SampleSignInGate] and feeds it from the identity seam.
 *
 * The gate starts on [identity]'s current state rather than [SampleSignInGate]'s `Checking`
 * default: a recreated host (rotation, process restore) that is already Signed in must compose the
 * tab shell in its first frame, or the shell would leave composition and drop its tab stacks.
 *
 * [follow] runs for as long as the gate is on screen. While no shell is open it also takes native
 * deep links off the [SampleDeepLinkSource] and holds them as the route to restore, so a link that
 * arrives on Login survives until Sign in (and, through [saver], a rotation of the Login screen).
 * Once the shell is open, links are left in the source for the shell to apply.
 */
@Stable
class SampleSignInGateHolder(
    private val identity: IdentityRepository,
    routeToRestore: SampleRoute? = null,
) {
    private val mutableGate = MutableStateFlow(
        SampleSignInGate(routeToRestore = routeToRestore).onIdentity(identity.state.value),
    )

    val gate: StateFlow<SampleSignInGate> = mutableGate.asStateFlow()

    /** Follows the identity seam and, while gated, holds [deepLinks]. Suspends until cancelled. */
    suspend fun follow(deepLinks: SampleDeepLinkSource?) {
        coroutineScope {
            launch {
                identity.state.collect { next -> mutableGate.update { it.onIdentity(next) } }
            }
            if (deepLinks != null) {
                launch {
                    // The combined view only says when to look; whether the gate holds the link is
                    // decided against the current gate in the same update that records it. The
                    // shell may have opened since `open` was computed, and a link taken then would
                    // be lost: the gate ignores it and the shell no longer finds it pending.
                    val shellOpen = mutableGate.map { it.isShellOpen }.distinctUntilChanged()
                    combine(shellOpen, deepLinks.pending) { open, route -> route.takeUnless { open } }
                        .filterNotNull()
                        .collect { route -> if (holdWhileGated(route)) deepLinks.consume() }
                }
            }
        }
    }

    /** Records [route] as the route to restore unless the shell is open; true when it was held. */
    private fun holdWhileGated(route: SampleRoute): Boolean {
        var held = false
        mutableGate.update { gate ->
            held = !gate.isShellOpen
            if (held) gate.onRouteRequested(route) else gate
        }
        return held
    }

    private val SampleSignInGate.isShellOpen: Boolean
        get() = surface is SampleGateSurface.Shell

    /** The open shell's active route, kept in case the Session is lost mid-use. */
    fun onShellRoute(route: SampleRoute) {
        mutableGate.update { it.onShellRoute(route) }
    }

    /** The shell has applied the route to restore. */
    fun onRouteRestored() {
        mutableGate.update { it.onRouteRestored() }
    }

    /**
     * A deliberate Sign out: ends the Session, and the next Sign in opens Home.
     *
     * With a [browser] bridge the URL is the route the next Sign in opens, so it is first replaced
     * with Home's, while the shell is still open and the bridge still attached.
     */
    suspend fun signOut(browser: SampleBrowserNavigation? = null) {
        browser?.onRouteChanged(SampleRoute.HOME, push = false)
        mutableGate.update { it.onSignOutRequested() }
        identity.signOut()
    }

    companion object {
        /**
         * Saves the held route as its canonical URL ([SampleRouteUrl]); an empty string means none.
         * The identity is not saved: it is read back from [identity] when the holder is rebuilt.
         */
        fun saver(identity: IdentityRepository): Saver<SampleSignInGateHolder, String> = Saver(
            save = { holder -> holder.gate.value.routeToRestore?.let(SampleRouteUrl::encode).orEmpty() },
            restore = { saved ->
                SampleSignInGateHolder(
                    identity = identity,
                    routeToRestore = saved.takeIf { it.isNotEmpty() }?.let(SampleRouteUrl::decode),
                )
            },
        )
    }
}
