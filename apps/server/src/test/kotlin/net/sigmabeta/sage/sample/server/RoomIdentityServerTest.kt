package net.sigmabeta.sage.sample.server

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.database.SampleDatabases
import net.sigmabeta.sage.sample.identity.RoomUserDirectory
import net.sigmabeta.sage.sample.identity.SignInResult
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shipped [ServerIdentity.overRoom] wiring, end to end over an in-memory Room database. */
class RoomIdentityServerTest {
    private val database: SampleDatabase = SampleDatabases.inMemory()

    @After
    fun closeDatabase() = database.close()

    /** A low iteration count keeps PBKDF2 fast; the stored row records it, as in production. */
    private fun identity() = runBlocking {
        ServerIdentity.overRoom(database, RoomUserDirectory(database, iterations = TEST_ITERATIONS))
    }

    @Test
    fun `a seeded demo account signs in, reads content, and signs out`() = testApplication {
        val identity = identity()
        application { configureSampleServer(identity) }

        val signIn = client.signIn()
        assertEquals(HttpStatusCode.OK, signIn.status)
        assertTrue(signIn.bodyAsText().contains(""""username":"demo""""))
        val cookie = signIn.sessionCookie()!!
        assertEquals(HttpStatusCode.OK, client.get(SampleApiRoutes.LIST_ITEMS) { withSession(cookie) }.status)

        val signOut = client.delete(SampleApiRoutes.SESSION) {
            withSession(cookie)
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.NoContent, signOut.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(SampleApiRoutes.LIST_ITEMS) { withSession(cookie) }.status)
    }

    @Test
    fun `restarting over the same database keeps Users and their Sessions`() = testApplication {
        val issued = runBlocking {
            val result = identity().signIn.signIn("demo", "sage", client = "test")
            (result as SignInResult.SignedIn).issued
        }

        // A second start seeds again (a no-op) and still knows the first start's Session.
        val restarted = identity()
        application { configureSampleServer(restarted) }

        val response = client.get(SampleApiRoutes.SESSION) { withSession(Cookie(SESSION_COOKIE, issued.token)) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"user":{"id":"${issued.session.user.id}","username":"demo"}}""",
            response.bodyAsText(),
        )
    }

    private companion object {
        const val TEST_ITERATIONS = 1_000
    }
}
