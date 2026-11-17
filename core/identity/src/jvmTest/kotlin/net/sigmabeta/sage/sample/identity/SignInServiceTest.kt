package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.database.SampleDatabases
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SignInServiceTest {
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = TestClock(start)
    private val database = SampleDatabases.inMemory()
    private val directory = RoomUserDirectory(database, iterations = 1_000)
    private val sessions = RoomSessionStore(database, clock)
    private val service = SignInService(directory, sessions, SignInLimiter(clock))

    @Before
    fun seed() = runTest {
        DemoAccounts.seed(directory)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `the right password signs in with a session that looks up to the user`() = runTest {
        val result = service.signIn("demo", "sage", client = "client-a")

        assertTrue(result is SignInResult.SignedIn)
        val issued = (result as SignInResult.SignedIn).issued
        assertEquals("demo", issued.session.user.username)
        assertEquals(issued.session, sessions.lookup(issued.token))
    }

    @Test
    fun `a wrong password or unknown username is rejected alike`() = runTest {
        assertEquals(SignInResult.Rejected, service.signIn("demo", "wrong", client = "client-a"))
        assertEquals(SignInResult.Rejected, service.signIn("nobody", "sage", client = "client-a"))
    }

    @Test
    fun `signing in twice gives the user two sessions`() = runTest {
        val phone = service.signIn("demo", "sage", client = "phone") as SignInResult.SignedIn
        val browser = service.signIn("demo", "sage", client = "browser") as SignInResult.SignedIn

        assertEquals(phone.issued.session.user, sessions.lookup(browser.issued.token)?.user)
        assertEquals(browser.issued.session.user, sessions.lookup(phone.issued.token)?.user)
    }

    @Test
    fun `a throttled username is refused even with the right password`() = runTest {
        repeat(6) { service.signIn("demo", "wrong", client = "client-$it") }

        assertEquals(SignInResult.Throttled(60.seconds), service.signIn("demo", "sage", client = "client-new"))
    }

    @Test
    fun `a throttled client is refused for every username`() = runTest {
        repeat(6) { service.signIn("guess-$it", "wrong", client = "client-a") }

        assertEquals(SignInResult.Throttled(60.seconds), service.signIn("demo2", "sage", client = "client-a"))
    }

    @Test
    fun `throttled attempts do not count as failures`() = runTest {
        repeat(6) { service.signIn("demo", "wrong", client = "client-a") }
        clock.now = start + 30.seconds
        repeat(10) { service.signIn("demo", "wrong", client = "client-a") }

        clock.now = start + 60.seconds

        assertTrue(service.signIn("demo", "sage", client = "client-a") is SignInResult.SignedIn)
    }

    @Test
    fun `a success clears the username's failures`() = runTest {
        repeat(5) { service.signIn("demo", "wrong", client = "client-$it") }
        assertTrue(service.signIn("demo", "sage", client = "client-ok") is SignInResult.SignedIn)

        repeat(5) { service.signIn("demo", "wrong", client = "client-again-$it") }

        assertTrue(service.signIn("demo", "sage", client = "client-ok") is SignInResult.SignedIn)
    }
}
