package net.sigmabeta.sage.sample.server

import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.sample.database.SampleDatabases
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.RoomUserDirectory
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** The shipped [RoomFavoritesStore] wiring: favorites live in the same database as the Users. */
class RoomFavoritesServerTest {
    private val database = SampleDatabases.inMemory()

    @After
    fun closeDatabase() = database.close()

    private fun identity() = runBlocking {
        ServerIdentity.overRoom(database, RoomUserDirectory(database, iterations = 1_000))
    }

    @Test
    fun `favorites are kept per user across a server restart`() {
        lateinit var demo: Cookie
        lateinit var demo2: Cookie

        testApplication {
            application { configureSampleServer(identity(), favorites = RoomFavoritesStore(database)) }
            demo = client.signedInCookie()
            demo2 = client.signIn(DemoAccounts.DEMO2.username, DemoAccounts.DEMO2.password).sessionCookie()!!
            val added = client.put(SampleApiRoutes.meFavorite("list-items:7")) {
                withSession(demo)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(HttpStatusCode.NoContent, added.status)
        }

        // A second application over the same database still knows both Sessions and demo's favorite.
        testApplication {
            application { configureSampleServer(identity(), favorites = RoomFavoritesStore(database)) }
            assertEquals(
                """{"contentIds":["list-items:7"]}""",
                client.get(SampleApiRoutes.ME_FAVORITES) { withSession(demo) }.bodyAsText(),
            )
            assertEquals(
                """{"contentIds":[]}""",
                client.get(SampleApiRoutes.ME_FAVORITES) { withSession(demo2) }.bodyAsText(),
            )
        }
    }
}
