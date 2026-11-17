package net.sigmabeta.sage.sample.server

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** `POST`/`GET`/`DELETE /api/session` through the shipped route stack, over the identity fakes. */
class SessionRoutesTest {
    private val test = TestIdentity()

    private fun sessionTest(
        secureCookie: Boolean = false,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application { configureSampleServer(test.identity, secureCookie = secureCookie) }
        block()
    }

    @Test
    fun `signing in returns the user and sets the session cookie`() = sessionTest {
        val response = client.signIn()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"user":{"id":"user-1","username":"demo"}}""", response.bodyAsText())
        val cookie = response.sessionCookie()!!
        assertEquals("sage_session", cookie.name)
        assertEquals("session-1", cookie.value)
    }

    @Test
    fun `the session cookie is HttpOnly, SameSite Lax, root-scoped and lasts 30 days`() = sessionTest {
        val cookie = client.signIn().sessionCookie()!!

        assertTrue(cookie.httpOnly)
        assertEquals("Lax", cookie.extensions["SameSite"])
        assertEquals("/", cookie.path)
        assertEquals(2_592_000, cookie.maxAge)
        assertFalse(cookie.secure)
    }

    @Test
    fun `the session cookie is Secure when the server is configured for it`() = sessionTest(secureCookie = true) {
        assertTrue(client.signIn().sessionCookie()!!.secure)
    }

    @Test
    fun `a wrong password and an unknown username are rejected identically`() = sessionTest {
        val wrongPassword = client.signIn(password = "nope")
        val unknownUser = client.signIn(username = "nobody")

        for (response in listOf(wrongPassword, unknownUser)) {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertNull(response.sessionCookie())
        }
        assertEquals(wrongPassword.bodyAsText(), unknownUser.bodyAsText())
        assertEquals("Wrong username or password", errorOf(wrongPassword))
    }

    @Test
    fun `too many failures are throttled with Retry-After in whole seconds rounded up`() = sessionTest {
        repeat(6) { assertEquals(HttpStatusCode.Unauthorized, client.signIn(password = "nope").status) }

        val throttled = client.signIn()
        assertEquals(HttpStatusCode.TooManyRequests, throttled.status)
        assertEquals("60", throttled.headers[HttpHeaders.RetryAfter])
        assertNull(throttled.sessionCookie())

        test.clock.now += 500.milliseconds
        assertEquals("60", client.signIn().headers[HttpHeaders.RetryAfter])

        test.clock.now += 1_000.milliseconds
        assertEquals("59", client.signIn().headers[HttpHeaders.RetryAfter])
    }

    @Test
    fun `reading the session returns the signed-in user`() = sessionTest {
        val cookie = client.signedInCookie()

        val response = client.get(SampleApiRoutes.SESSION) { withSession(cookie) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"user":{"id":"user-1","username":"demo"}}""", response.bodyAsText())
    }

    @Test
    fun `reading the session while signed out is a 401`() = sessionTest {
        val response = client.get(SampleApiRoutes.SESSION)
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("Signed out", errorOf(response))
    }

    @Test
    fun `an unknown or expired session cookie reads as signed out`() = sessionTest {
        val cookie = client.signedInCookie()

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get(SampleApiRoutes.SESSION) { withSession(cookie.copy(value = "forged")) }.status,
        )

        test.clock.now += 30.days
        assertEquals(HttpStatusCode.Unauthorized, client.get(SampleApiRoutes.SESSION) { withSession(cookie) }.status)
    }

    @Test
    fun `signing out revokes the session and clears the cookie`() = sessionTest {
        val cookie = client.signedInCookie()

        val response = client.delete(SampleApiRoutes.SESSION) {
            withSession(cookie)
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        val cleared = response.sessionCookie()!!
        assertEquals(0, cleared.maxAge)
        assertEquals("", cleared.value)
        assertEquals("/", cleared.path)
        assertEquals(HttpStatusCode.Unauthorized, client.get(SampleApiRoutes.SESSION) { withSession(cookie) }.status)
    }

    @Test
    fun `signing out while already signed out still succeeds`() = sessionTest {
        val response = client.delete(SampleApiRoutes.SESSION) { contentType(ContentType.Application.Json) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(0, response.sessionCookie()!!.maxAge)
    }

    @Test
    fun `sign in and sign out without a JSON content type are unsupported`() = sessionTest {
        val cookie = client.signedInCookie()

        val noType = client.post(SampleApiRoutes.SESSION) { setBody("""{"username":"demo","password":"sage"}""") }
        val form = client.post(SampleApiRoutes.SESSION) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("username=demo&password=sage")
        }
        val signOut = client.delete(SampleApiRoutes.SESSION) { withSession(cookie) }

        for (response in listOf(noType, form, signOut)) {
            assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
            assertNull(response.sessionCookie())
        }
        assertEquals("Expected application/json", errorOf(signOut))
        // The refused Sign out left the Session alone.
        assertEquals(HttpStatusCode.OK, client.get(SampleApiRoutes.SESSION) { withSession(cookie) }.status)
    }

    @Test
    fun `a JSON content type with charset is accepted`() = sessionTest {
        val response = client.post(SampleApiRoutes.SESSION) {
            contentType(ContentType.Application.Json.withParameter("charset", "utf-8"))
            setBody("""{"username":"demo","password":"sage"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `a malformed sign in body is a 400`() = sessionTest {
        for (body in listOf("not json", """{"username":"demo"}""")) {
            val response = client.post(SampleApiRoutes.SESSION) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(body, HttpStatusCode.BadRequest, response.status)
            assertEquals("Malformed request body", errorOf(response))
        }
    }

    private suspend fun errorOf(response: HttpResponse): String? =
        Json.parseToJsonElement(response.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content
}
