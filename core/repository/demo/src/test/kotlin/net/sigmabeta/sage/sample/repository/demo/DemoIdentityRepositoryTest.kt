package net.sigmabeta.sage.sample.repository.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.SESSION_LIFETIME
import net.sigmabeta.sage.sample.identity.SignInLimiter
import net.sigmabeta.sage.sample.identity.SignInService
import net.sigmabeta.sage.sample.identity.fake.FakeClock
import net.sigmabeta.sage.sample.identity.fake.FakeSessionStore
import net.sigmabeta.sage.sample.identity.fake.FakeUserDirectory
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import net.sigmabeta.sage.storage.common.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import net.sigmabeta.sage.sample.identity.User as IdentityUser

/**
 * The in-process identity source over `:core:identity`'s fakes: the stored Session token survives a
 * "restart" (a new repository over the same Storage), and startup validates it.
 */
class DemoIdentityRepositoryTest {
    private val clock = FakeClock(Instant.fromEpochMilliseconds(1_000_000))
    private val users = FakeUserDirectory()
    private val sessions = FakeSessionStore(clock)
    private val storage = MapStorage()

    private fun TestScope.repository(): DemoIdentityRepository {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return DemoIdentityRepository(
            users = users,
            sessions = sessions,
            signInService = SignInService(users, sessions, SignInLimiter(clock)),
            storage = storage,
            scope = CoroutineScope(dispatcher),
            dispatchers = SageDispatchers(dispatcher, dispatcher, dispatcher, dispatcher),
        )
    }

    @Test
    fun `starts checking and settles signed out without a stored token`() = runTest {
        val repository = repository()
        assertEquals(IdentityState.Checking, repository.state.value)

        advanceUntilIdle()

        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    @Test
    fun `startup with a valid stored token is signed in as its User`() = runTest {
        val user = users.register("demo", "sage")!!
        storage.saveString(DemoIdentityRepository.KEY_SESSION_TOKEN, sessions.create(user).token)

        val repository = repository()
        advanceUntilIdle()

        assertEquals(IdentityState.SignedIn(User(user.id, "demo")), repository.state.value)
    }

    @Test
    fun `startup with an expired stored token clears it and is signed out`() = runTest {
        val token = sessions.create(IdentityUser("user-9", "demo")).token
        storage.saveString(DemoIdentityRepository.KEY_SESSION_TOKEN, token)
        clock.now += SESSION_LIFETIME

        val repository = repository()
        advanceUntilIdle()

        assertEquals(IdentityState.SignedOut, repository.state.value)
        assertTrue(storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN).isNullOrEmpty())
    }

    @Test
    fun `startup with an unknown stored token clears it and is signed out`() = runTest {
        storage.saveString(DemoIdentityRepository.KEY_SESSION_TOKEN, "session-404")

        val repository = repository()
        advanceUntilIdle()

        assertEquals(IdentityState.SignedOut, repository.state.value)
        assertTrue(storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN).isNullOrEmpty())
    }

    @Test
    fun `startup seeds the Demo accounts`() = runTest {
        repository()
        advanceUntilIdle()

        DemoAccounts.all.forEach { assertTrue(users.verify(it.username, it.password) != null) }
    }

    @Test
    fun `accepted sign in signs in and the Session survives a restart`() = runTest {
        val first = repository()
        advanceUntilIdle()

        val outcome = first.signIn("demo", "sage")

        val signedIn = IdentityState.SignedIn((outcome as SignInOutcome.Accepted).user)
        assertEquals("demo", outcome.user.username)
        assertEquals(signedIn, first.state.value)

        val restarted = repository()
        advanceUntilIdle()
        assertEquals(signedIn, restarted.state.value)
    }

    @Test
    fun `sign in before the startup check answers still succeeds`() = runTest {
        val repository = repository()

        val outcome = repository.signIn(DemoAccounts.DEMO2.username, DemoAccounts.DEMO2.password)
        advanceUntilIdle()

        assertTrue(outcome is SignInOutcome.Accepted)
        assertTrue(repository.state.value is IdentityState.SignedIn)
    }

    @Test
    fun `wrong password is rejected and stays signed out`() = runTest {
        val repository = repository()
        advanceUntilIdle()

        assertEquals(SignInOutcome.Rejected, repository.signIn("demo", "Sage"))
        assertEquals(IdentityState.SignedOut, repository.state.value)
        assertTrue(storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN).isNullOrEmpty())
    }

    @Test
    fun `repeated failures are throttled with the limiter's retry-after`() = runTest {
        val repository = repository()
        advanceUntilIdle()

        repeat(SignInLimiter.DEFAULT_MAX_FAILURES + 1) { repository.signIn("demo", "wrong") }

        assertEquals(SignInOutcome.Throttled(1.minutes), repository.signIn("demo", "sage"))
        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    @Test
    fun `sign out revokes the Session, clears the token and signs out`() = runTest {
        val repository = repository()
        advanceUntilIdle()
        repository.signIn("demo", "sage")
        val token = storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN)!!

        repository.signOut()

        assertEquals(IdentityState.SignedOut, repository.state.value)
        assertTrue(storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN).isNullOrEmpty())
        assertNull(sessions.lookup(token))
    }

    @Test
    fun `signing in again revokes the Session it replaces`() = runTest {
        // One Session per device: the stored token is the device's Session, so a new one revokes it.
        val repository = repository()
        advanceUntilIdle()
        repository.signIn("demo", "sage")
        val first = storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN)!!

        repository.signIn(DemoAccounts.DEMO2.username, DemoAccounts.DEMO2.password)

        assertNull(sessions.lookup(first))
        assertTrue(sessions.lookup(storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN)!!) != null)
    }

    @Test
    fun `a rejected sign in keeps the stored Session`() = runTest {
        val repository = repository()
        advanceUntilIdle()
        repository.signIn("demo", "sage")
        val token = storage.stored(DemoIdentityRepository.KEY_SESSION_TOKEN)!!

        repository.signIn("demo", "wrong")

        assertTrue(sessions.lookup(token) != null)
    }

    @Test
    fun `sign out while signed out stays signed out`() = runTest {
        val repository = repository()
        advanceUntilIdle()

        repository.signOut()

        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    /** An in-memory [Storage]; one flow per key, like the app backends. */
    private class MapStorage : Storage {
        private val values = MutableStateFlow<Map<String, String>>(emptyMap())

        fun stored(key: String): String? = values.value[key]

        override fun saveString(key: String, value: String) {
            values.value += key to value
        }

        override fun savedStringFlow(key: String): Flow<String?> = values.map { it[key] }

        override fun saveInt(key: String, value: Int) = saveString(key, value.toString())

        override fun savedIntFlow(key: String): Flow<Int?> = values.map { it[key]?.toIntOrNull() }
    }
}
