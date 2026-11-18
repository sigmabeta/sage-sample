package net.sigmabeta.sage.sample.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.sample.models.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedFavoritesRepositoryTest {
    private val identity = FakeIdentity()
    private val source = FakeSource()

    private fun kotlinx.coroutines.test.TestScope.repository() =
        CachedFavoritesRepository(identity, source, backgroundScope)

    @Test
    fun `a signed-out client has no favorites and cannot change any`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        assertEquals(emptySet<ContentId>(), repository.favorites.first())
        assertFalse(repository.setFavorite(SEVEN, true))
        assertEquals(emptyList<String>(), source.calls)
    }

    @Test
    fun `a signed-in user sees what the source holds for them`() = runTest(UnconfinedTestDispatcher()) {
        source.stored["user-demo"] = mutableSetOf(SEVEN)
        identity.state.value = IdentityState.SignedIn(DEMO)

        assertEquals(setOf(SEVEN), repository().favorites.first())
    }

    @Test
    fun `favoriting and un-favoriting reach the source and the flow`() = runTest(UnconfinedTestDispatcher()) {
        identity.state.value = IdentityState.SignedIn(DEMO)
        val repository = repository()
        val seen = mutableListOf<Set<ContentId>>()
        backgroundScope.launch { repository.favorites.collect { seen += it } }

        assertTrue(repository.setFavorite(SEVEN, true))
        assertTrue(repository.setFavorite(SEVEN, false))

        assertEquals(listOf(emptySet(), setOf(SEVEN), emptySet()), seen)
        assertEquals(emptySet<ContentId>(), source.stored["user-demo"])
        assertEquals(listOf("load user-demo", "set user-demo list-items:7 true", "set user-demo list-items:7 false"), source.calls)
    }

    @Test
    fun `a source failure reverts the change and reports it`() = runTest(UnconfinedTestDispatcher()) {
        identity.state.value = IdentityState.SignedIn(DEMO)
        val repository = repository()
        source.failSets = true

        assertFalse(repository.setFavorite(SEVEN, true))

        assertEquals(emptySet<ContentId>(), repository.favorites.first())
    }

    @Test
    fun `signing in as another user never shows the previous user's favorites`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        identity.state.value = IdentityState.SignedIn(DEMO)
        assertTrue(repository.setFavorite(SEVEN, true))
        assertEquals(setOf(SEVEN), repository.favorites.first())

        identity.state.value = IdentityState.SignedOut
        assertEquals(emptySet<ContentId>(), repository.favorites.first())

        identity.state.value = IdentityState.SignedIn(DEMO2)
        assertEquals(emptySet<ContentId>(), repository.favorites.first())

        // Signed straight from one user to another, with no Signed out between, is the same.
        identity.state.value = IdentityState.SignedIn(DEMO)
        assertEquals(setOf(SEVEN), repository.favorites.first())
        identity.state.value = IdentityState.SignedIn(DEMO2)
        assertEquals(emptySet<ContentId>(), repository.favorites.first())
    }

    @Test
    fun `signing out drops the cache so the next sign in reads the source again`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        identity.state.value = IdentityState.SignedIn(DEMO)
        assertEquals(emptySet<ContentId>(), repository.favorites.first())

        identity.state.value = IdentityState.SignedOut
        source.stored["user-demo"] = mutableSetOf(SEVEN) // changed elsewhere, e.g. from another device
        identity.state.value = IdentityState.SignedIn(DEMO)

        assertEquals(setOf(SEVEN), repository.favorites.first())
    }

    private class FakeIdentity : IdentityRepository {
        override val state: MutableStateFlow<IdentityState> = MutableStateFlow(IdentityState.SignedOut)

        override suspend fun signIn(username: String, password: String): SignInOutcome =
            error("not used")

        override suspend fun signOut() = error("not used")
    }

    private class FakeSource : FavoritesSource {
        val stored = mutableMapOf<String, MutableSet<ContentId>>()
        val calls = mutableListOf<String>()
        var failSets = false

        override suspend fun load(user: User): Set<ContentId> {
            calls += "load ${user.id}"
            return stored[user.id].orEmpty().toSet()
        }

        override suspend fun set(user: User, contentId: ContentId, favorite: Boolean) {
            calls += "set ${user.id} ${contentId.value} $favorite"
            check(!failSets) { "offline" }
            val ids = stored.getOrPut(user.id) { mutableSetOf() }
            if (favorite) ids += contentId else ids -= contentId
        }
    }

    private companion object {
        val DEMO = User("user-demo", "demo")
        val DEMO2 = User("user-demo2", "demo2")
        val SEVEN = ContentId("list-items:7")
    }
}
