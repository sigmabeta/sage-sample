package net.sigmabeta.sage.sample.repository.demo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.database.SampleDatabases
import net.sigmabeta.sage.sample.database.UserEntity
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The in-process favorites: rows in [SampleDatabases] keyed by the User the Session belongs to. */
@OptIn(ExperimentalCoroutinesApi::class)
class DemoFavoritesRepositoryTest {
    private val database = SampleDatabases.inMemory()
    private val identity = FakeIdentity()

    @After
    fun closeDatabase() = database.close()

    private suspend fun TestScope.repository(): DemoFavoritesRepository {
        listOf(DEMO, DEMO2).forEach {
            database.userDao().insert(UserEntity(it.id, it.username, "aA==", "cw==", "PBKDF2WithHmacSHA256", 1))
        }
        return DemoFavoritesRepository(identity, database, backgroundScope)
    }

    @Test
    fun `a favorite is stored for the signed-in user`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        identity.state.value = IdentityState.SignedIn(DEMO)

        assertTrue(repository.setFavorite(SEVEN, true))

        assertEquals(setOf(SEVEN), repository.favorites.first())
        assertEquals(listOf("list-items:7"), database.favoriteDao().listForUser("user-demo").map { it.contentId })
    }

    @Test
    fun `demo2 never sees demo's favorites, in the cache or in the database`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        identity.state.value = IdentityState.SignedIn(DEMO)
        repository.setFavorite(SEVEN, true)

        identity.state.value = IdentityState.SignedOut
        identity.state.value = IdentityState.SignedIn(DEMO2)

        assertEquals(emptySet<ContentId>(), repository.favorites.first())
        assertEquals(emptyList<Any>(), database.favoriteDao().listForUser("user-demo2"))

        identity.state.value = IdentityState.SignedIn(DEMO)
        assertEquals(setOf(SEVEN), repository.favorites.first())
    }

    @Test
    fun `favorites survive a new repository over the same database`() = runTest(UnconfinedTestDispatcher()) {
        val first = repository()
        identity.state.value = IdentityState.SignedIn(DEMO)
        first.setFavorite(SEVEN, true)

        val second = DemoFavoritesRepository(identity, database, backgroundScope)

        assertEquals(setOf(SEVEN), second.favorites.first())
        assertTrue(second.setFavorite(SEVEN, false))
        assertEquals(emptySet<ContentId>(), second.favorites.first())
    }

    @Test
    fun `signed out changes are refused`() = runTest(UnconfinedTestDispatcher()) {
        assertFalse(repository().setFavorite(SEVEN, true))
    }

    private class FakeIdentity : IdentityRepository {
        override val state = MutableStateFlow<IdentityState>(IdentityState.SignedOut)

        override suspend fun signIn(username: String, password: String): SignInOutcome = error("not used")

        override suspend fun signOut() = error("not used")
    }

    private companion object {
        val DEMO = User("user-demo", "demo")
        val DEMO2 = User("user-demo2", "demo2")
        val SEVEN = ContentId("list-items:7")
    }
}
