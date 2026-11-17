package net.sigmabeta.sage.sample.database

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDaoTest {
    private val database = SampleDatabases.inMemory()
    private val users = database.userDao()
    private val sessions = database.sessionDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `an inserted session is found by its token hash`() = runTest {
        users.insert(ALICE)
        sessions.insert(PHONE)

        assertEquals(PHONE, sessions.findByTokenHash("token-phone"))
        assertNull(sessions.findByTokenHash("token-unknown"))
    }

    @Test
    fun `one user holds a session per device`() = runTest {
        users.insert(ALICE)
        sessions.insert(PHONE)
        sessions.insert(BROWSER)

        assertEquals(listOf(PHONE, BROWSER), sessions.listForUser("user-alice"))
    }

    @Test
    fun `deleting one session leaves the user's other sessions`() = runTest {
        users.insert(ALICE)
        sessions.insert(PHONE)
        sessions.insert(BROWSER)

        sessions.deleteByTokenHash("token-phone")

        assertNull(sessions.findByTokenHash("token-phone"))
        assertEquals(listOf(BROWSER), sessions.listForUser("user-alice"))
    }

    @Test
    fun `a session for an unknown user is refused`() = runTest {
        val refused = runCatching { sessions.insert(PHONE) }

        assertTrue(refused.exceptionOrNull() is SQLiteException)
        assertNull(sessions.findByTokenHash("token-phone"))
    }

    @Test
    fun `a reused token hash is refused and the first session is kept`() = runTest {
        users.insert(ALICE)
        users.insert(BOB)
        sessions.insert(PHONE)

        val refused = runCatching { sessions.insert(PHONE.copy(userId = "user-bob")) }

        assertTrue(refused.exceptionOrNull() is SQLiteException)
        assertEquals(PHONE, sessions.findByTokenHash("token-phone"))
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
        val BOB = ALICE.copy(id = "user-bob", username = "bob")
        val PHONE = SessionEntity(
            tokenHash = "token-phone",
            userId = "user-alice",
            createdAt = 1_000L,
            expiresAt = 5_000L,
        )
        val BROWSER = SessionEntity(
            tokenHash = "token-browser",
            userId = "user-alice",
            createdAt = 2_000L,
            expiresAt = 9_000L,
        )
    }
}
