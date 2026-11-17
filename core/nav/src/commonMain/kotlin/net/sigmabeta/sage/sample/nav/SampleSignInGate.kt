package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.User

/** What the app shows above everything else while the Sign-in gate decides. */
sealed interface SampleGateSurface {
    /**
     * The startup check has not answered: neither Login nor the tabs, so a client that is already
     * Signed in does not flash Login on launch.
     */
    data object Blank : SampleGateSurface

    /** Signed out: only Login, with no tabs, hamburger or tab top-bar chrome. */
    data object Login : SampleGateSurface

    /**
     * Signed in as [user]: the tabbed shell. The host keys the shell on [user], and a Signed out
     * client never shows it, so every Session opens a shell whose tab stacks start at their roots.
     */
    data class Shell(val user: User) : SampleGateSurface
}

/**
 * The Sign-in gate as a pure value: the client's [identity], plus the route it should land on once
 * it is Signed in.
 *
 * [routeToRestore] is the route the shell applies when it opens; `null` means the shell's default,
 * Home. It is set in two ways:
 * - a route requested while gated (a native deep link, or any external route) — the latest wins;
 * - losing the Session mid-use (a content call answered 401) — the route the shell last reported.
 *
 * A deliberate Sign out ([onSignOutRequested]) does not keep the route it was made from, and a
 * different User signing in does not inherit the previous User's route.
 *
 * Every transition returns a new gate; the host holds the current one.
 */
data class SampleSignInGate(
    val identity: IdentityState = IdentityState.Checking,
    val routeToRestore: SampleRoute? = null,
    private val shellRoute: SampleRoute? = null,
    private val signOutRequested: Boolean = false,
) {
    val surface: SampleGateSurface
        get() = when (identity) {
            IdentityState.Checking -> SampleGateSurface.Blank
            IdentityState.SignedOut -> SampleGateSurface.Login
            is IdentityState.SignedIn -> SampleGateSurface.Shell(identity.user)
        }

    /** The identity seam reported [next]. */
    fun onIdentity(next: IdentityState): SampleSignInGate {
        if (next == identity) return this
        val sessionEnded = identity is IdentityState.SignedIn
        return when {
            // Another User without a Signed out step in between: a fresh shell at Home.
            sessionEnded && next is IdentityState.SignedIn -> SampleSignInGate(identity = next)

            // The Session ended. Unless the user asked for it, keep where they were.
            sessionEnded -> SampleSignInGate(
                identity = next,
                routeToRestore = if (signOutRequested) null else shellRoute,
            )

            else -> copy(identity = next)
        }
    }

    /**
     * An external route (a deep link) was requested. While gated it becomes [routeToRestore];
     * while the shell is open the shell applies it itself, so the gate ignores it.
     */
    fun onRouteRequested(route: SampleRoute): SampleSignInGate =
        if (identity is IdentityState.SignedIn) this else copy(routeToRestore = route)

    /** The open shell reported its active route; ignored when no shell is open. */
    fun onShellRoute(route: SampleRoute): SampleSignInGate =
        if (identity is IdentityState.SignedIn) copy(shellRoute = route) else this

    /** The user asked to Sign out, so the coming Signed out state restores Home. */
    fun onSignOutRequested(): SampleSignInGate =
        if (identity is IdentityState.SignedIn) copy(signOutRequested = true) else this

    /** The shell has applied [routeToRestore]. */
    fun onRouteRestored(): SampleSignInGate = copy(routeToRestore = null)
}

/**
 * Back on a gated surface: Blank and Login fall through to the host (Android finishes the
 * activity; desktop has nothing above). `null` for the shell, whose own [backActionFor] applies.
 */
fun gateBackActionFor(surface: SampleGateSurface): SampleBackAction? = when (surface) {
    SampleGateSurface.Blank, SampleGateSurface.Login -> SampleBackAction.Outer
    is SampleGateSurface.Shell -> null
}
