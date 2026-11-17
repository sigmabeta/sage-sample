package net.sigmabeta.sage.sample.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shipped route stack: the same `configureSampleServer` mounted by `main()`, over the identity
 * fakes. Content routes need a Session, so each content test signs in as the Demo account first.
 */
class SampleServerTest {
    @Test
    fun `health reports ok`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val response = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, json(response.bodyAsText()).jsonObject["ok"]?.jsonPrimitive?.content?.toBoolean())
    }

    @Test
    fun `home count and grid come from the shared demo content`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val cookie = client.signedInCookie()
        val count = json(client.get("/api/home/count?seed=big_image_1") { withSession(cookie) }.bodyAsText())
            .jsonObject["count"]?.jsonPrimitive?.int
        assertEquals(18, count)

        val grid = json(client.get("/api/home/grid?seed=big_image_1&limit=3") { withSession(cookie) }.bodyAsText())
            .jsonObject["items"]?.jsonArray
        assertEquals(3, grid?.size)
    }

    @Test
    fun `home item returns null past the catalog and validates its index`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val cookie = client.signedInCookie()
        val pastEnd = json(client.get("/api/home/item?seed=big_image_1&index=99") { withSession(cookie) }.bodyAsText())
        assertTrue(pastEnd.jsonObject["item"]?.toString() == "null")

        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/api/home/item?seed=big_image_1&index=nope") { withSession(cookie) }.status,
        )
    }

    @Test
    fun `list items pages and clamps`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val cookie = client.signedInCookie()
        val firstPage = json(client.get("/api/list-items?offset=0&limit=5") { withSession(cookie) }.bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertEquals(5, firstPage.size)

        val pastEnd = json(client.get("/api/list-items?offset=99&limit=5") { withSession(cookie) }.bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertTrue(pastEnd.isEmpty())

        // A negative offset clamps to zero rather than failing.
        val clamped = json(client.get("/api/list-items?offset=-3&limit=2") { withSession(cookie) }.bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertEquals(2, clamped.size)
    }

    @Test
    fun `content detail returns content, null, and null for malformed ids`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val cookie = client.signedInCookie()
        val known = json(client.get("/api/content?id=list-items:7") { withSession(cookie) }.bodyAsText()).jsonObject["detail"]
        assertFalse(known?.toString() == "null")

        val unknown = json(client.get("/api/content?id=not-a-surface:1") { withSession(cookie) }.bodyAsText())
        assertEquals("null", unknown.jsonObject["detail"]?.toString())

        val malformed = json(client.get("/api/content?id=has%20space") { withSession(cookie) }.bodyAsText())
        assertEquals("null", malformed.jsonObject["detail"]?.toString())
    }

    @Test
    fun `search returns deterministic results per kind`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        val cookie = client.signedInCookie()
        val first = client.get("/api/search?q=chrono&kind=list") { withSession(cookie) }.bodyAsText()
        val second = client.get("/api/search?q=chrono&kind=list") { withSession(cookie) }.bodyAsText()
        assertEquals(first, second)

        val results = json(first).jsonObject["results"]!!.jsonArray
        assertTrue(results.isNotEmpty())

        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/api/search?q=chrono&kind=nope") { withSession(cookie) }.status,
        )
    }

    @Test
    fun `an app route containing a dot is not treated as a missing asset`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        // Content IDs may contain dots; the fallback only treats known asset extensions as files.
        assertTrue(client.get("/content-detail/a.b").status != HttpStatusCode.NotFound)
    }

    @Test
    fun `a missing asset is a real 404, not the spa shell`() = testApplication {
        application { configureSampleServer(TestIdentity().identity) }
        assertEquals(HttpStatusCode.NotFound, client.get("/missing.js").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/wasm/missing.wasm").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/missing.png").status)
    }

    private fun json(text: String) = Json.parseToJsonElement(text)
}
