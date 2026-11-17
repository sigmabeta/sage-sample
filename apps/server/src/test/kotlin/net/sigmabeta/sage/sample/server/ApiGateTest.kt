package net.sigmabeta.sage.sample.server

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days

/**
 * Every `/api` route but [SampleApiRoutes.HEALTH] and [SampleApiRoutes.SESSION] needs a Session;
 * the SPA shell and static assets do not.
 */
class ApiGateTest {
    private val test = TestIdentity()

    private fun gateTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { configureSampleServer(test.identity) }
        block()
    }

    /** One valid request per content route; a new route belongs here. */
    private val contentRequests = listOf(
        "${SampleApiRoutes.HOME_COUNT}?seed=big_image_1",
        "${SampleApiRoutes.HOME_ITEM}?seed=big_image_1&index=0",
        "${SampleApiRoutes.HOME_GRID}?seed=big_image_1&limit=3",
        "${SampleApiRoutes.CONTENT_DETAIL}?id=list-items:7",
        "${SampleApiRoutes.LIST_ITEMS}?offset=0&limit=5",
        "${SampleApiRoutes.GRID_OF_IMAGES}?offset=0&limit=5",
        "${SampleApiRoutes.SEARCH}?q=chrono&kind=list",
    )

    @Test
    fun `content routes refuse a signed-out request`() = gateTest {
        for (path in contentRequests) {
            val response = client.get(path)
            assertEquals(path, HttpStatusCode.Unauthorized, response.status)
            assertEquals(path, "Sign in required", errorOf(response))
        }
    }

    @Test
    fun `content routes serve a signed-in request`() = gateTest {
        val cookie = client.signedInCookie()
        for (path in contentRequests) {
            assertEquals(path, HttpStatusCode.OK, client.get(path) { withSession(cookie) }.status)
        }
    }

    @Test
    fun `a forged, revoked or expired session is refused`() = gateTest {
        val path = contentRequests.first()
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get(path) { withSession(Cookie(SESSION_COOKIE, "forged")) }.status,
        )

        val revoked = client.signedInCookie()
        test.sessions.revoke(revoked.value)
        assertEquals(HttpStatusCode.Unauthorized, client.get(path) { withSession(revoked) }.status)

        val expiring = client.signedInCookie()
        test.clock.now += 30.days
        assertEquals(HttpStatusCode.Unauthorized, client.get(path) { withSession(expiring) }.status)
    }

    @Test
    fun `health and the session resource stay public`() = gateTest {
        assertEquals(HttpStatusCode.OK, client.get(SampleApiRoutes.HEALTH).status)
        // The Session resource answers for itself: "Signed out", not the gate's "Sign in required".
        assertEquals("Signed out", errorOf(client.get(SampleApiRoutes.SESSION)))
        assertEquals(HttpStatusCode.OK, client.signIn().status)
    }

    @Test
    fun `unknown api paths are gated, then a json 404`() = gateTest {
        val paths = listOf("/api", "/api/nope", "/api/nope/deeper")
        for (path in paths) {
            val response = client.get(path)
            assertEquals(path, HttpStatusCode.Unauthorized, response.status)
            assertEquals(path, "Sign in required", errorOf(response))
        }

        val cookie = client.signedInCookie()
        for (path in paths) {
            val response = client.get(path) { withSession(cookie) }
            assertEquals(path, HttpStatusCode.NotFound, response.status)
            assertEquals(path, "Not found", errorOf(response))
        }
    }

    @Test
    fun `the spa shell and static assets stay public`() = gateTest {
        // Without a bundle the shell is a 503 and a missing asset a 404; neither is the gate's 401.
        for (path in listOf("/", "/home", "/content-detail/a.b", "/apix")) {
            assertNotEquals(path, HttpStatusCode.Unauthorized, client.get(path).status)
        }
        assertEquals(HttpStatusCode.NotFound, client.get("/missing.js").status)
    }

    private suspend fun errorOf(response: HttpResponse): String? =
        Json.parseToJsonElement(response.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content
}
