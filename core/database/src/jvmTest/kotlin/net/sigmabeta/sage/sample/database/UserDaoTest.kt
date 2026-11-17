package net.sigmabeta.sage.sample.database

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDaoTest {
    private val database = SampleDatabases.inMemory()
    private val users = database.userDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `an inserted user is found by username and by id`() = runTest {
        users.insert(ALICE)

        assertEquals(ALICE, users.findByUsername("alice"))
        assertEquals(ALICE, users.findById("user-alice"))
    }

    @Test
    fun `an unknown username finds no user`() = runTest {
        users.insert(ALICE)

        assertNull(users.findByUsername("bob"))
    }

    @Test
    fun `a taken username is refused and the first user is kept`() = runTest {
        users.insert(ALICE)

        val refused = runCatching { users.insert(ALICE.copy(id = "user-other", salt = "b3RoZXI=")) }

        assertTrue(refused.exceptionOrNull() is SQLiteException)
        assertEquals(ALICE, users.findByUsername("alice"))
        assertNull(users.findById("user-other"))
    }

    private companion object {
        val ALICE = UserEntity(
            id = "user-alice",
            username = "alice",
            passwordHash = "aGFzaA==",
            salt = "c2FsdA==",
            kdfAlgorithm = "PBKDF2WithHmacSHA256",
            kdfIterations = 210_000,
        )
    }
}
