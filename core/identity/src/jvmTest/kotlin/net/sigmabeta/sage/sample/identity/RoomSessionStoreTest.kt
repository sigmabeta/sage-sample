package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.database.SampleDatabases
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class RoomSessionStoreTest {
    private val database = SampleDatabases.inMemory()
    private val clock = TestClock(Instant.parse("2026-01-01T00:00:00Z"))
    private val directory = RoomUserDirectory(database, iterations = 1_000)
    private val store = RoomSessionStore(database, clock)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `a new session's token finds its user`() = runTest {
        val alice = directory.register("alice", "pw")!!

        val issued = store.create(alice)

        assertEquals(alice, issued.session.user)
        assertEquals(issued.session, store.lookup(issued.token))
    }

    @Test
    fun `a session lasts thirty days from sign in`() = runTest {
        val alice = directory.register("alice", "pw")!!

        val session = store.create(alice).session

        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), session.createdAt)
        assertEquals(Instant.parse("2026-01-31T00:00:00Z"), session.expiresAt)
    }

    @Test
    fun `the token is 256 random bits in url-safe base64`() = runTest {
        val alice = directory.register("alice", "pw")!!

        val first = store.create(alice).token
        val second = store.create(alice).token

        assertEquals(32, Base64.getUrlDecoder().decode(first).size)
        assertNotEquals(first, second)
    }

    @Test
    fun `an unknown token finds nothing`() = runTest {
        directory.register("alice", "pw")!!.let { store.create(it) }

        assertNull(store.lookup("not-a-token"))
        assertNull(store.lookup(""))
    }

    @Test
    fun `the database holds only the token's hash`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val token = store.create(alice).token

        val stored = database.sessionDao().listForUser(alice.id).single()

        assertNotEquals(token, stored.tokenHash)
        assertNull(database.sessionDao().findByTokenHash(token))
    }

    @Test
    fun `a revoked session finds nothing`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val token = store.create(alice).token

        store.revoke(token)

        assertNull(store.lookup(token))
    }

    @Test
    fun `revoking an unknown token changes nothing`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val issued = store.create(alice)

        store.revoke("not-a-token")

        assertEquals(issued.session, store.lookup(issued.token))
    }

    @Test
    fun `a user holds several sessions and revoking one keeps the others`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val phone = store.create(alice)
        val browser = store.create(alice)

        assertEquals(alice, store.lookup(phone.token)?.user)
        assertEquals(alice, store.lookup(browser.token)?.user)

        store.revoke(phone.token)

        assertNull(store.lookup(phone.token))
        assertEquals(browser.session, store.lookup(browser.token))
    }

    @Test
    fun `sessions of different users stay apart`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val bob = directory.register("bob", "pw")!!

        val aliceToken = store.create(alice).token
        val bobToken = store.create(bob).token

        assertEquals(alice, store.lookup(aliceToken)?.user)
        assertEquals(bob, store.lookup(bobToken)?.user)
    }

    @Test
    fun `a session is valid until the last millisecond before expiry`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val issued = store.create(alice)

        clock.now = issued.session.expiresAt - 1.milliseconds

        assertEquals(issued.session, store.lookup(issued.token))
    }

    @Test
    fun `a session at its expiry finds nothing`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val token = store.create(alice).token

        clock.now += 30.days

        assertNull(store.lookup(token))
    }

    @Test
    fun `expiry is absolute so use does not extend it`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val token = store.create(alice).token

        clock.now += 29.days
        assertEquals(alice, store.lookup(token)?.user)
        clock.now += 1.days

        assertNull(store.lookup(token))
    }

    @Test
    fun `an expired session is deleted when it is looked up`() = runTest {
        val alice = directory.register("alice", "pw")!!
        val token = store.create(alice).token

        clock.now += 31.days
        assertNull(store.lookup(token))
        clock.now -= 31.days

        assertNull(store.lookup(token))
        assertTrue(database.sessionDao().listForUser(alice.id).isEmpty())
    }
}
