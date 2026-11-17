package net.sigmabeta.sage.sample.identity

import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.database.SampleDatabases
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RoomUserDirectoryTest {
    private val database = SampleDatabases.inMemory()
    private var kdfRuns = 0
    private val directory = RoomUserDirectory(
        database = database,
        iterations = TEST_ITERATIONS,
        derive = { password, salt, iterations ->
            kdfRuns++
            pbkdf2HmacSha256(password, salt, iterations)
        },
    )

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `a registered user signs in with their password`() = runTest {
        val registered = directory.register("alice", "correct horse")

        assertNotNull(registered)
        assertEquals("alice", registered!!.username)
        assertEquals(registered, directory.verify("alice", "correct horse"))
    }

    @Test
    fun `a wrong password is rejected`() = runTest {
        directory.register("alice", "correct horse")

        assertNull(directory.verify("alice", "battery staple"))
        assertNull(directory.verify("alice", ""))
    }

    @Test
    fun `an unknown username is rejected`() = runTest {
        directory.register("alice", "correct horse")

        assertNull(directory.verify("bob", "correct horse"))
    }

    @Test
    fun `the username is case sensitive`() = runTest {
        directory.register("alice", "correct horse")

        assertNull(directory.verify("Alice", "correct horse"))
        assertNull(directory.verify("ALICE", "correct horse"))
    }

    @Test
    fun `nothing is trimmed from the username or the password`() = runTest {
        directory.register("alice", "correct horse")

        assertNull(directory.verify(" alice", "correct horse"))
        assertNull(directory.verify("alice ", "correct horse"))
        assertNull(directory.verify("alice", "correct horse "))
        assertNull(directory.verify("alice", " correct horse"))
    }

    @Test
    fun `a taken username is not registered again and keeps its password`() = runTest {
        val first = directory.register("alice", "correct horse")

        assertNull(directory.register("alice", "something else"))
        assertEquals(first, directory.verify("alice", "correct horse"))
        assertNull(directory.verify("alice", "something else"))
    }

    @Test
    fun `each user gets their own stable id`() = runTest {
        val alice = directory.register("alice", "same password")!!
        val bob = directory.register("bob", "same password")!!

        assertNotEquals(alice.id, bob.id)
        assertEquals(alice, directory.findById(alice.id))
        assertEquals(bob, directory.findById(bob.id))
        assertNull(directory.findById("no-such-id"))
    }

    @Test
    fun `an unknown username still costs one key derivation`() = runTest {
        directory.register("alice", "correct horse")
        kdfRuns = 0

        directory.verify("bob", "correct horse")
        val unknownRuns = kdfRuns
        kdfRuns = 0
        directory.verify("alice", "wrong")

        assertEquals(1, unknownRuns)
        assertEquals(1, kdfRuns)
    }

    @Test
    fun `a user stored with another iteration count still verifies`() = runTest {
        RoomUserDirectory(database, iterations = TEST_ITERATIONS * 2).register("alice", "correct horse")

        assertNotNull(directory.verify("alice", "correct horse"))
    }

    private companion object {
        // Far below the production default so the suite stays fast; the row records the count, so
        // verification honours whatever produced the hash.
        const val TEST_ITERATIONS = 1_000
    }
}
