package net.sigmabeta.sage.sample.database

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteDaoTest {
    private val database = SampleDatabases.inMemory()
    private val users = database.userDao()
    private val favorites = database.favoriteDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `a user's favorites come back oldest first`() = runTest {
        users.insert(ALICE)
        favorites.add(FavoriteEntity("user-alice", "list-items:2", createdAt = 2_000L))
        favorites.add(FavoriteEntity("user-alice", "list-items:1", createdAt = 1_000L))

        assertEquals(listOf("list-items:1", "list-items:2"), favorites.listForUser("user-alice").map { it.contentId })
    }

    @Test
    fun `favoriting twice keeps the first row`() = runTest {
        users.insert(ALICE)
        favorites.add(FavoriteEntity("user-alice", "list-items:1", createdAt = 1_000L))
        favorites.add(FavoriteEntity("user-alice", "list-items:1", createdAt = 9_000L))

        assertEquals(listOf(1_000L), favorites.listForUser("user-alice").map { it.createdAt })
    }

    @Test
    fun `two users favorite the same content separately`() = runTest {
        users.insert(ALICE)
        users.insert(BOB)
        favorites.add(FavoriteEntity("user-alice", "list-items:1", createdAt = 1_000L))
        favorites.add(FavoriteEntity("user-bob", "list-items:1", createdAt = 1_000L))

        favorites.remove("user-alice", "list-items:1")

        assertEquals(emptyList<FavoriteEntity>(), favorites.listForUser("user-alice"))
        assertEquals(1, favorites.listForUser("user-bob").size)
    }

    @Test
    fun `removing something that was never a favorite is a no-op`() = runTest {
        users.insert(ALICE)

        favorites.remove("user-alice", "list-items:1")

        assertEquals(emptyList<FavoriteEntity>(), favorites.listForUser("user-alice"))
    }

    @Test
    fun `a favorite for an unknown user is refused`() = runTest {
        val refused = runCatching { favorites.add(FavoriteEntity("user-ghost", "list-items:1", createdAt = 1L)) }

        assertTrue(refused.exceptionOrNull() is SQLiteException)
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
    }
}
