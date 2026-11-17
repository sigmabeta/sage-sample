package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.saveable.SaverScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.nav.SampleDestination
import net.sigmabeta.sage.sample.nav.SampleGateSurface
import net.sigmabeta.sage.sample.nav.SampleRoute
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The shell's side of the Sign-in gate: [SampleSignInGateHolder] follows the identity seam, holds
 * native deep links that arrive while gated, and survives a saved-state round trip.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SampleSignInGateHolderTest {
    private val ada = User(id = "user-ada", username = "ada")
    private val listItems = SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS)
    private val settings = SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS)

    @Test
    fun `a holder starts on the identity's current state, with no blank frame`() {
        // A recreated activity rebuilds the holder while Signed in; showing Blank for even one frame
        // would take the tab shell out of composition and reset its stacks.
        val holder = SampleSignInGateHolder(FakeIdentity(IdentityState.SignedIn(ada)))

        assertEquals(SampleGateSurface.Shell(ada), holder.gate.value.surface)
    }

    @Test
    fun `the holder follows the identity seam`() = runTest(UnconfinedTestDispatcher()) {
        val identity = FakeIdentity(IdentityState.Checking)
        val holder = SampleSignInGateHolder(identity)
        backgroundScope.launch { holder.follow(deepLinks = null) }

        assertEquals(SampleGateSurface.Blank, holder.gate.value.surface)
        identity.state.value = IdentityState.SignedOut
        assertEquals(SampleGateSurface.Login, holder.gate.value.surface)
        identity.state.value = IdentityState.SignedIn(ada)
        assertEquals(SampleGateSurface.Shell(ada), holder.gate.value.surface)
    }

    @Test
    fun `a deep link offered while Signed out is held and opened after sign in`() =
        runTest(UnconfinedTestDispatcher()) {
            val identity = FakeIdentity(IdentityState.SignedOut)
            val deepLinks = MutableSampleDeepLinkSource()
            val holder = SampleSignInGateHolder(identity)
            backgroundScope.launch { holder.follow(deepLinks) }

            deepLinks.offer(listItems)

            assertNull("the gate took the link off the source", deepLinks.pending.value)
            identity.state.value = IdentityState.SignedIn(ada)
            assertEquals(listItems, holder.gate.value.routeToRestore)
        }

    @Test
    fun `a deep link already waiting at launch is held while checking`() =
        runTest(UnconfinedTestDispatcher()) {
            val deepLinks = MutableSampleDeepLinkSource(initial = listItems)
            val holder = SampleSignInGateHolder(FakeIdentity(IdentityState.Checking))
            backgroundScope.launch { holder.follow(deepLinks) }

            assertNull(deepLinks.pending.value)
            assertEquals(listItems, holder.gate.value.routeToRestore)
        }

    @Test
    fun `a deep link while the shell is open is left for the shell`() =
        runTest(UnconfinedTestDispatcher()) {
            val deepLinks = MutableSampleDeepLinkSource()
            val holder = SampleSignInGateHolder(FakeIdentity(IdentityState.SignedIn(ada)))
            backgroundScope.launch { holder.follow(deepLinks) }

            deepLinks.offer(listItems)

            assertEquals(listItems, deepLinks.pending.value)
            assertNull(holder.gate.value.routeToRestore)
        }

    @Test
    fun `losing the Session mid-use keeps the route the shell reported`() =
        runTest(UnconfinedTestDispatcher()) {
            val identity = FakeIdentity(IdentityState.SignedIn(ada))
            val holder = SampleSignInGateHolder(identity)
            backgroundScope.launch { holder.follow(deepLinks = null) }

            holder.onShellRoute(listItems)
            identity.state.value = IdentityState.SignedOut

            assertEquals(SampleGateSurface.Login, holder.gate.value.surface)
            assertEquals(listItems, holder.gate.value.routeToRestore)
        }

    @Test
    fun `signing out through the holder ends the Session and restores Home`() =
        runTest(UnconfinedTestDispatcher()) {
            val identity = FakeIdentity(IdentityState.SignedIn(ada))
            val holder = SampleSignInGateHolder(identity)
            backgroundScope.launch { holder.follow(deepLinks = null) }

            holder.onShellRoute(settings)
            holder.signOut()

            assertEquals(1, identity.signOuts)
            assertEquals(SampleGateSurface.Login, holder.gate.value.surface)
            assertNull(holder.gate.value.routeToRestore)
        }

    @Test
    fun `a deliberate sign out on the web rewrites the URL to Home before the Session ends`() =
        runTest(UnconfinedTestDispatcher()) {
            // The browser applies its URL after the next Sign in, so a URL left on Settings would
            // undo the gate's "a deliberate sign-out restores Home".
            val identity = FakeIdentity(IdentityState.SignedIn(ada))
            val browser = RecordingBrowserNavigation(identity)
            val holder = SampleSignInGateHolder(identity)
            backgroundScope.launch { holder.follow(deepLinks = null) }

            holder.onShellRoute(settings)
            holder.signOut(browser)

            assertEquals(listOf(Report(SampleRoute.HOME, push = false, signedIn = true)), browser.reports)
            assertEquals(1, identity.signOuts)
        }

    @Test
    fun `the restore route is cleared once the shell applied it`() {
        val holder = SampleSignInGateHolder(FakeIdentity(IdentityState.SignedOut), routeToRestore = listItems)

        holder.onRouteRestored()

        assertNull(holder.gate.value.routeToRestore)
    }

    @Test
    fun `the held route survives a saved-state round trip`() {
        val identity = FakeIdentity(IdentityState.SignedOut)
        val holder = SampleSignInGateHolder(identity, routeToRestore = listItems)
        val saver = SampleSignInGateHolder.saver(identity)

        val saved = with(saver) { SaverScope { true }.save(holder) }
        val restored = checkNotNull(saver.restore(checkNotNull(saved)))

        assertEquals(listItems, restored.gate.value.routeToRestore)
        assertEquals(SampleGateSurface.Login, restored.gate.value.surface)
    }

    @Test
    fun `a holder with nothing held round-trips to Home`() {
        val identity = FakeIdentity(IdentityState.SignedOut)
        val saver = SampleSignInGateHolder.saver(identity)

        val saved = with(saver) { SaverScope { true }.save(SampleSignInGateHolder(identity)) }
        val restored = checkNotNull(saver.restore(checkNotNull(saved)))

        assertNull(restored.gate.value.routeToRestore)
    }

    private data class Report(val route: SampleRoute, val push: Boolean, val signedIn: Boolean)

    /** Records route reports, and whether the Session was still open (bridge still attached). */
    private class RecordingBrowserNavigation(private val identity: IdentityRepository) :
        SampleBrowserNavigation {
        val reports = mutableListOf<Report>()

        override fun onShellReady(navigation: SampleShellNavigation) = Unit

        override fun onRouteChanged(route: SampleRoute, push: Boolean) {
            reports += Report(route, push, signedIn = identity.state.value is IdentityState.SignedIn)
        }

        override fun onBackRequested(): Boolean = false

        override fun onShellDisposed() = Unit
    }

    private class FakeIdentity(initial: IdentityState) : IdentityRepository {
        override val state = MutableStateFlow(initial)
        var signOuts = 0

        override suspend fun signIn(username: String, password: String): SignInOutcome =
            error("not used")

        override suspend fun signOut() {
            signOuts += 1
            state.value = IdentityState.SignedOut
        }
    }
}
