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
 * The shipped route stack: the same `configureSampleServer()` mounted by `main()`.
 */
class SampleServerTest {
    @Test
    fun `health reports ok`() = testApplication {
        application { configureSampleServer() }
        val response = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, json(response.bodyAsText()).jsonObject["ok"]?.jsonPrimitive?.content?.toBoolean())
    }

    @Test
    fun `home count and grid come from the shared demo content`() = testApplication {
        application { configureSampleServer() }
        val count = json(client.get("/api/home/count?seed=big_image_1").bodyAsText())
            .jsonObject["count"]?.jsonPrimitive?.int
        assertEquals(18, count)

        val grid = json(client.get("/api/home/grid?seed=big_image_1&limit=3").bodyAsText())
            .jsonObject["items"]?.jsonArray
        assertEquals(3, grid?.size)
    }

    @Test
    fun `home item returns null past the catalog and validates its index`() = testApplication {
        application { configureSampleServer() }
        val pastEnd = json(client.get("/api/home/item?seed=big_image_1&index=99").bodyAsText())
        assertTrue(pastEnd.jsonObject["item"]?.toString() == "null")

        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/api/home/item?seed=big_image_1&index=nope").status,
        )
    }

    @Test
    fun `list items pages and clamps`() = testApplication {
        application { configureSampleServer() }
        val firstPage = json(client.get("/api/list-items?offset=0&limit=5").bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertEquals(5, firstPage.size)

        val pastEnd = json(client.get("/api/list-items?offset=99&limit=5").bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertTrue(pastEnd.isEmpty())

        // A negative offset clamps to zero rather than failing.
        val clamped = json(client.get("/api/list-items?offset=-3&limit=2").bodyAsText())
            .jsonObject["items"]!!.jsonArray
        assertEquals(2, clamped.size)
    }

    @Test
    fun `content detail returns content, null, and null for malformed ids`() = testApplication {
        application { configureSampleServer() }
        val known = json(client.get("/api/content?id=list-items:7").bodyAsText()).jsonObject["detail"]
        assertFalse(known?.toString() == "null")

        val unknown = json(client.get("/api/content?id=not-a-surface:1").bodyAsText())
        assertEquals("null", unknown.jsonObject["detail"]?.toString())

        val malformed = json(client.get("/api/content?id=has%20space").bodyAsText())
        assertEquals("null", malformed.jsonObject["detail"]?.toString())
    }

    @Test
    fun `search returns deterministic results per kind`() = testApplication {
        application { configureSampleServer() }
        val first = client.get("/api/search?q=chrono&kind=list").bodyAsText()
        val second = client.get("/api/search?q=chrono&kind=list").bodyAsText()
        assertEquals(first, second)

        val results = json(first).jsonObject["results"]!!.jsonArray
        assertTrue(results.isNotEmpty())

        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/api/search?q=chrono&kind=nope").status,
        )
    }

    @Test
    fun `unknown api paths are a json 404`() = testApplication {
        application { configureSampleServer() }
        for (path in listOf("/api", "/api/nope", "/api/nope/deeper")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(
                "Not found",
                json(response.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content,
            )
        }
    }

    @Test
    fun `an app route containing a dot is not treated as a missing asset`() = testApplication {
        application { configureSampleServer() }
        // Content IDs may contain dots; the fallback only treats known asset extensions as files.
        assertTrue(client.get("/content-detail/a.b").status != HttpStatusCode.NotFound)
    }

    @Test
    fun `a missing asset is a real 404, not the spa shell`() = testApplication {
        application { configureSampleServer() }
        assertEquals(HttpStatusCode.NotFound, client.get("/missing.js").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/wasm/missing.wasm").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/missing.png").status)
    }

    private fun json(text: String) = Json.parseToJsonElement(text)
}
