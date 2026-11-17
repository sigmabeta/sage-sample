package net.sigmabeta.sage.sample.database

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SampleDatabasesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a file database keeps its users and sessions across reopening`() = runTest {
        val path = folder.root.resolve("sample.db").absolutePath

        val first = SampleDatabases.file(path)
        first.userDao().insert(ALICE)
        first.sessionDao().insert(PHONE)
        first.close()

        val reopened = SampleDatabases.file(path)
        try {
            assertEquals(ALICE, reopened.userDao().findByUsername("alice"))
            assertEquals(listOf(PHONE), reopened.sessionDao().listForUser("user-alice"))
        } finally {
            reopened.close()
        }
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
        val PHONE = SessionEntity(
            tokenHash = "token-phone",
            userId = "user-alice",
            createdAt = 1_000L,
            expiresAt = 5_000L,
        )
    }
}
