package net.sigmabeta.sage.sample

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The benchmark build's launch hook, over a fake identity source: the state the Sign-in gate reads
 * never says Signed out while the hook is signing in, so a measured launch never draws Login.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkSignInTest {
    private val demo = User(id = "user-1", username = "demo")

    @Test
    fun `the hook's class name is the one the debug and release checks look for`() {
        assertEquals(BENCHMARK_SIGN_IN_CLASS, BenchmarkSignIn::class.java.name)
    }

    @Test
    fun `signs a Demo account in and the gate never sees Signed out`() = runTest {
        val identity = FakeIdentity(accept = demo)
        val hook = hook(identity)
        val seen = record(hook.gatedIdentity.state)

        hook.request(DemoAccounts.DEMO)
        identity.state.value = IdentityState.SignedOut
        advanceUntilIdle()
        identity.answer()
        advanceUntilIdle()

        assertEquals(listOf("demo" to "sage"), identity.signIns)
        assertEquals(IdentityState.SignedIn(demo), hook.gatedIdentity.state.value)
        assertFalse(IdentityState.SignedOut in seen)
    }

    @Test
    fun `a stored Session is reused without signing in again`() = runTest {
        val identity = FakeIdentity(accept = demo)
        val hook = hook(identity)

        hook.request(DemoAccounts.DEMO)
        identity.state.value = IdentityState.SignedIn(demo)
        advanceUntilIdle()

        assertTrue(identity.signIns.isEmpty())
        assertEquals(IdentityState.SignedIn(demo), hook.gatedIdentity.state.value)
    }

    @Test
    fun `a second launch while signing in does not sign in twice`() = runTest {
        val identity = FakeIdentity(accept = demo)
        val hook = hook(identity)

        hook.request(DemoAccounts.DEMO)
        identity.state.value = IdentityState.SignedOut
        advanceUntilIdle()
        hook.request(DemoAccounts.DEMO)
        advanceUntilIdle()
        identity.answer()
        advanceUntilIdle()

        assertEquals(1, identity.signIns.size)
    }

    @Test
    fun `a rejected sign in falls through to Signed out instead of staying blank`() = runTest {
        val identity = FakeIdentity(accept = null)
        val hook = hook(identity)

        hook.request(DemoAccounts.DEMO)
        identity.state.value = IdentityState.SignedOut
        advanceUntilIdle()
        identity.answer()
        advanceUntilIdle()

        assertEquals(IdentityState.SignedOut, hook.gatedIdentity.state.value)
    }

    @Test
    fun `without a request the gate sees the identity state unchanged`() = runTest {
        val identity = FakeIdentity(accept = demo)
        val hook = hook(identity)

        identity.state.value = IdentityState.SignedOut
        advanceUntilIdle()

        assertEquals(IdentityState.SignedOut, hook.gatedIdentity.state.value)
        assertTrue(identity.signIns.isEmpty())
    }

    @Test
    fun `the launch extra names a Demo account and anything else is ignored`() {
        assertEquals("sagesample.benchmark.signIn", BenchmarkSignIn.EXTRA_SIGN_IN)
        assertSame(DemoAccounts.DEMO2, BenchmarkSignIn.accountFor("demo2"))
        assertEquals(null, BenchmarkSignIn.accountFor("root"))
        assertEquals(null, BenchmarkSignIn.accountFor(null))
    }

    /** The hook in a scope that ends with the test, dispatching immediately like `Main.immediate`. */
    private fun TestScope.hook(identity: IdentityRepository): BenchmarkSignIn = BenchmarkSignIn(
        identity,
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
    )

    private fun TestScope.record(state: StateFlow<IdentityState>): List<IdentityState> {
        val seen = mutableListOf<IdentityState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { seen += it } }
        return seen
    }

    /** Starts Checking; a Sign in suspends until [answer], then accepts [accept] or rejects. */
    private class FakeIdentity(private val accept: User?) : IdentityRepository {
        override val state = MutableStateFlow<IdentityState>(IdentityState.Checking)
        val signIns = mutableListOf<Pair<String, String>>()
        private val answered = CompletableDeferred<Unit>()

        fun answer() = answered.complete(Unit)

        override suspend fun signIn(username: String, password: String): SignInOutcome {
            signIns += username to password
            answered.await()
            return if (accept != null) {
                state.value = IdentityState.SignedIn(accept)
                SignInOutcome.Accepted(accept)
            } else {
                SignInOutcome.Rejected
            }
        }

        override suspend fun signOut() {
            state.value = IdentityState.SignedOut
        }
    }
}
