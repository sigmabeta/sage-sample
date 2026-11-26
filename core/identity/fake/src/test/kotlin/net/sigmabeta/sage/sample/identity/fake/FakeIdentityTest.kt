package net.sigmabeta.sage.sample.identity.fake

import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.SignInLimiter
import net.sigmabeta.sage.sample.identity.SignInResult
import net.sigmabeta.sage.sample.identity.SignInService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class FakeIdentityTest {
    private val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
    private val users = FakeUserDirectory()
    private val sessions = FakeSessionStore(clock)

    @Test
    fun `the fake directory matches usernames and passwords exactly`() = runTest {
        val alice = users.register("alice", "pw")!!

        assertEquals(alice, users.verify("alice", "pw"))
        assertNull(users.verify("Alice", "pw"))
        assertNull(users.verify("alice ", "pw"))
        assertNull(users.verify("alice", "pw "))
        assertNull(users.verify("bob", "pw"))
        assertNull(users.register("alice", "other"))
    }

    @Test
    fun `the fake directory gives each user their own id`() = runTest {
        assertNotEquals(users.register("alice", "pw")!!.id, users.register("bob", "pw")!!.id)
    }

    @Test
    fun `fake sessions look up revoke and stack per user`() = runTest {
        val alice = users.register("alice", "pw")!!
        val phone = sessions.create(alice)
        val browser = sessions.create(alice)

        assertNotEquals(phone.token, browser.token)
        assertEquals(phone.session, sessions.lookup(phone.token))
        sessions.revoke(phone.token)

        assertNull(sessions.lookup(phone.token))
        assertEquals(browser.session, sessions.lookup(browser.token))
        assertNull(sessions.lookup("unknown"))
    }

    @Test
    fun `fake sessions expire thirty days after sign in`() = runTest {
        val alice = users.register("alice", "pw")!!
        val issued = sessions.create(alice)

        assertEquals(clock.now + 30.days, issued.session.expiresAt)
        clock.now = issued.session.expiresAt - 1.milliseconds
        assertEquals(issued.session, sessions.lookup(issued.token))
        clock.now = issued.session.expiresAt

        assertNull(sessions.lookup(issued.token))
    }

    @Test
    fun `the fakes seed and sign in through the shared service`() = runTest {
        DemoAccounts.seed(users)
        DemoAccounts.seed(users)
        val service = SignInService(users, sessions, SignInLimiter(clock))

        val result = service.signIn("demo", "sage", client = "test")

        assertTrue(result is SignInResult.SignedIn)
        assertEquals("demo", sessions.lookup((result as SignInResult.SignedIn).issued.token)?.user?.username)
        assertEquals(SignInResult.Rejected, service.signIn("demo2", "nope", client = "test"))
    }
}
