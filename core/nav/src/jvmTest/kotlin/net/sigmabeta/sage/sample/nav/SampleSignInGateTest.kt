package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Sign-in gate's pure policy: which surface the app shows for each identity state, which route
 * a Signed out client keeps for after Sign in, and what Back means while gated.
 */
class SampleSignInGateTest {
    private val ada = User(id = "user-ada", username = "ada")
    private val grace = User(id = "user-grace", username = "grace")

    private val listItems = SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS)
    private val detail = SampleRoute(
        SampleDestination.HOME,
        SampleDestination.CONTENT_DETAIL,
        ContentId("list-items:7"),
    )
    private val settings = SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS)

    private val signedInAsAda = SampleSignInGate().onIdentity(IdentityState.SignedIn(ada))

    @Test
    fun `each identity state shows exactly one surface`() {
        val expected = mapOf(
            IdentityState.Checking to SampleGateSurface.Blank,
            IdentityState.SignedOut to SampleGateSurface.Login,
            IdentityState.SignedIn(ada) to SampleGateSurface.Shell(ada),
        )

        expected.forEach { (identity, surface) ->
            assertEquals("surface for $identity", surface, SampleSignInGate().onIdentity(identity).surface)
        }
    }

    @Test
    fun `a new gate is checking and shows the blank surface`() {
        assertEquals(SampleGateSurface.Blank, SampleSignInGate().surface)
        assertNull(SampleSignInGate().routeToRestore)
    }

    @Test
    fun `signing in with nothing requested opens Home`() {
        val gate = SampleSignInGate()
            .onIdentity(IdentityState.SignedOut)
            .onIdentity(IdentityState.SignedIn(ada))

        assertEquals(SampleGateSurface.Shell(ada), gate.surface)
        assertNull("null means the shell's default, Home", gate.routeToRestore)
    }

    @Test
    fun `a route requested while checking is kept for after sign in`() {
        val gate = SampleSignInGate()
            .onRouteRequested(listItems)
            .onIdentity(IdentityState.SignedOut)
            .onIdentity(IdentityState.SignedIn(ada))

        assertEquals(listItems, gate.routeToRestore)
    }

    @Test
    fun `a route requested on Login is kept and the latest request wins`() {
        val gate = SampleSignInGate()
            .onIdentity(IdentityState.SignedOut)
            .onRouteRequested(listItems)
            .onRouteRequested(detail)
            .onIdentity(IdentityState.SignedIn(ada))

        assertEquals(detail, gate.routeToRestore)
    }

    @Test
    fun `a route requested while the shell is open is left to the shell`() {
        val gate = signedInAsAda.onRouteRequested(listItems)

        assertNull(gate.routeToRestore)
    }

    @Test
    fun `losing the Session mid-use shows Login and keeps the current route`() {
        val gate = signedInAsAda
            .onShellRoute(detail)
            .onIdentity(IdentityState.SignedOut)

        assertEquals(SampleGateSurface.Login, gate.surface)
        assertEquals(detail, gate.routeToRestore)
        assertEquals(detail, gate.onIdentity(IdentityState.SignedIn(ada)).routeToRestore)
    }

    @Test
    fun `losing the Session before the shell reported a route restores Home`() {
        val gate = signedInAsAda.onIdentity(IdentityState.SignedOut)

        assertNull(gate.routeToRestore)
    }

    @Test
    fun `a deliberate sign out restores Home, not the route it was made from`() {
        val gate = signedInAsAda
            .onShellRoute(settings)
            .onSignOutRequested()
            .onIdentity(IdentityState.SignedOut)

        assertEquals(SampleGateSurface.Login, gate.surface)
        assertNull(gate.routeToRestore)
    }

    @Test
    fun `a sign out request is forgotten once the Session has ended`() {
        // Signed out deliberately, signed back in, then lost the Session: that loss is not the
        // deliberate one, so the current route is kept.
        val gate = signedInAsAda
            .onSignOutRequested()
            .onIdentity(IdentityState.SignedOut)
            .onIdentity(IdentityState.SignedIn(ada))
            .onShellRoute(listItems)
            .onIdentity(IdentityState.SignedOut)

        assertEquals(listItems, gate.routeToRestore)
    }

    @Test
    fun `a request on Login after a lost Session replaces the kept route`() {
        val gate = signedInAsAda
            .onShellRoute(detail)
            .onIdentity(IdentityState.SignedOut)
            .onRouteRequested(listItems)

        assertEquals(listItems, gate.routeToRestore)
    }

    @Test
    fun `the restore route is cleared once the shell has applied it`() {
        val gate = SampleSignInGate()
            .onRouteRequested(listItems)
            .onIdentity(IdentityState.SignedIn(ada))
            .onRouteRestored()

        assertNull(gate.routeToRestore)
    }

    @Test
    fun `route reports outside the shell are ignored`() {
        val gate = SampleSignInGate()
            .onIdentity(IdentityState.SignedOut)
            .onShellRoute(detail)
            .onIdentity(IdentityState.SignedIn(ada))

        assertNull(gate.routeToRestore)
    }

    @Test
    fun `another User signing in opens a fresh shell at Home`() {
        val gate = signedInAsAda
            .onShellRoute(detail)
            .onIdentity(IdentityState.SignedIn(grace))

        assertEquals(SampleGateSurface.Shell(grace), gate.surface)
        assertNull(gate.routeToRestore)
        assertNull(
            "the previous User's route is gone",
            gate.onIdentity(IdentityState.SignedOut).routeToRestore,
        )
    }

    @Test
    fun `the same identity again changes nothing`() {
        val gate = signedInAsAda.onShellRoute(detail)

        assertEquals(gate, gate.onIdentity(IdentityState.SignedIn(ada)))
    }

    @Test
    fun `back while gated falls through to the host and the shell keeps its own policy`() {
        val expected = mapOf(
            SampleGateSurface.Blank to SampleBackAction.Outer,
            SampleGateSurface.Login to SampleBackAction.Outer,
            SampleGateSurface.Shell(ada) to null,
        )

        expected.forEach { (surface, action) ->
            assertEquals("back on $surface", action, gateBackActionFor(surface))
        }
    }
}
