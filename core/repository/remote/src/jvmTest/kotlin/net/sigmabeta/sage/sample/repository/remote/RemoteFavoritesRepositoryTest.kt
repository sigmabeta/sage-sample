package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The browser's favorites over `/api/me/favorites`, against Ktor's MockEngine. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteFavoritesRepositoryTest {
    private val calls = mutableListOf<String>()
    private val contentTypes = mutableListOf<String?>()
    private var putStatus = HttpStatusCode.NoContent
    private val session = RemoteSessionState().also { it.signedIn(DEMO) }

    private fun TestScope.repository(): RemoteFavoritesRepository {
        val engine = MockEngine { request ->
            calls += "${request.method.value} ${request.url.encodedPath}"
            contentTypes += request.body.contentType?.toString()
            when {
                request.method == HttpMethod.Get ->
                    respond(
                        """{"contentIds":["list-items:7"]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                putStatus == HttpStatusCode.NoContent -> respond("", HttpStatusCode.NoContent)

                else -> respondError(putStatus)
            }
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val api = SampleApiClient(http, "http://sample.test", session)
        return RemoteFavoritesRepository(RemoteIdentityStub(session), api, backgroundScope)
    }

    @Test
    fun `favorites load from the server for the signed-in user`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        assertEquals(setOf(SEVEN), repository.favorites.first())
        assertEquals(listOf("GET /api/me/favorites"), calls)
    }

    @Test
    fun `favoriting and un-favoriting send json PUT and DELETE for that id`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()

        assertTrue(repository.setFavorite(ContentId("grid-of-images:2"), true))
        assertTrue(repository.setFavorite(ContentId("grid-of-images:2"), false))

        assertEquals(
            listOf(
                "GET /api/me/favorites",
                "PUT /api/me/favorites/grid-of-images:2",
                "DELETE /api/me/favorites/grid-of-images:2",
            ),
            calls,
        )
        assertEquals(listOf(true, true), contentTypes.drop(1).map { it?.startsWith("application/json") })
    }

    @Test
    fun `a refused change is reverted and reported`() = runTest(UnconfinedTestDispatcher()) {
        putStatus = HttpStatusCode.InternalServerError
        val repository = repository()

        assertFalse(repository.setFavorite(ContentId("grid-of-images:2"), true))

        assertEquals(setOf(SEVEN), repository.favorites.first())
    }

    @Test
    fun `a 401 on a change signs the client out instead of replaying it for the next user`() =
        runTest(UnconfinedTestDispatcher()) {
            putStatus = HttpStatusCode.Unauthorized
            val repository = repository()

            assertFalse(repository.setFavorite(ContentId("grid-of-images:2"), true))

            assertEquals(IdentityState.SignedOut, session.state.value)
            assertEquals(1, calls.count { it.startsWith("PUT") })
        }

    /** An [IdentityRepository] that just reports the shared [RemoteSessionState]. */
    private class RemoteIdentityStub(session: RemoteSessionState) : IdentityRepository {
        override val state = session.state

        override suspend fun signIn(username: String, password: String): SignInOutcome = error("not used")

        override suspend fun signOut() = error("not used")
    }

    private companion object {
        val DEMO = User("user-demo", "demo")
        val SEVEN = ContentId("list-items:7")
    }
}
