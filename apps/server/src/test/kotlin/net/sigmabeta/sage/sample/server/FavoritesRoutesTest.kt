package net.sigmabeta.sage.sample.server

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import org.junit.Assert.assertEquals
import org.junit.Test

/** `/api/me/favorites`: the User comes from the Session cookie, never from the request. */
class FavoritesRoutesTest {
    private val test = TestIdentity()
    private val favorites = InMemoryFavoritesStore()

    private fun favoritesTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { configureSampleServer(test.identity, favorites = favorites) }
        block()
    }

    @Test
    fun `every favorites route refuses a signed-out request`() = favoritesTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get(SampleApiRoutes.ME_FAVORITES).status)
        assertEquals(HttpStatusCode.Unauthorized, client.putFavorite(null, "list-items:7").status)
        assertEquals(HttpStatusCode.Unauthorized, client.deleteFavorite(null, "list-items:7").status)
    }

    @Test
    fun `a new user has no favorites`() = favoritesTest {
        val cookie = client.signedInCookie()

        assertEquals(emptyList<String>(), client.favoritesOf(cookie))
    }

    @Test
    fun `a favorite is listed until it is removed`() = favoritesTest {
        val cookie = client.signedInCookie()

        assertEquals(HttpStatusCode.NoContent, client.putFavorite(cookie, "list-items:7").status)
        assertEquals(HttpStatusCode.NoContent, client.putFavorite(cookie, "grid-of-images:2").status)
        assertEquals(listOf("list-items:7", "grid-of-images:2"), client.favoritesOf(cookie))

        assertEquals(HttpStatusCode.NoContent, client.deleteFavorite(cookie, "list-items:7").status)
        assertEquals(listOf("grid-of-images:2"), client.favoritesOf(cookie))
    }

    @Test
    fun `favoriting twice and un-favoriting something absent both succeed`() = favoritesTest {
        val cookie = client.signedInCookie()

        assertEquals(HttpStatusCode.NoContent, client.putFavorite(cookie, "list-items:7").status)
        assertEquals(HttpStatusCode.NoContent, client.putFavorite(cookie, "list-items:7").status)
        assertEquals(listOf("list-items:7"), client.favoritesOf(cookie))

        assertEquals(HttpStatusCode.NoContent, client.deleteFavorite(cookie, "list-items:99").status)
        assertEquals(listOf("list-items:7"), client.favoritesOf(cookie))
    }

    @Test
    fun `two users' favorites are separate`() = favoritesTest {
        val demo = client.signedInCookie()
        val demo2 = client.signIn(DemoAccounts.DEMO2.username, DemoAccounts.DEMO2.password).sessionCookie()!!

        client.putFavorite(demo, "list-items:7")
        client.putFavorite(demo2, "list-items:8")

        assertEquals(listOf("list-items:7"), client.favoritesOf(demo))
        assertEquals(listOf("list-items:8"), client.favoritesOf(demo2))

        client.deleteFavorite(demo2, "list-items:7")
        assertEquals(listOf("list-items:7"), client.favoritesOf(demo))
    }

    @Test
    fun `one user's second session sees the same favorites`() = favoritesTest {
        val phone = client.signedInCookie()
        val browser = client.signedInCookie()
        client.putFavorite(phone, "list-items:7")

        assertEquals(listOf("list-items:7"), client.favoritesOf(browser))
    }

    @Test
    fun `a user id in the request is ignored`() = favoritesTest {
        val demo = client.signedInCookie()
        val demo2 = client.signIn(DemoAccounts.DEMO2.username, DemoAccounts.DEMO2.password).sessionCookie()!!
        client.putFavorite(demo2, "list-items:8")

        // The fake directory hands out ids user-1, user-2; demo2 is the second User.
        val listed = client.get("${SampleApiRoutes.ME_FAVORITES}?userId=user-2") { withSession(demo) }
        assertEquals(emptyList<String>(), idsOf(listed))
    }

    @Test
    fun `a malformed content id is a bad request`() = favoritesTest {
        val cookie = client.signedInCookie()

        assertEquals(HttpStatusCode.BadRequest, client.putFavorite(cookie, "not%20valid").status)
        assertEquals(emptyList<String>(), client.favoritesOf(cookie))
    }

    @Test
    fun `changing a favorite needs a json content type`() = favoritesTest {
        val cookie = client.signedInCookie()

        val put = client.put("${SampleApiRoutes.ME_FAVORITES}/list-items:7") { withSession(cookie) }
        val delete = client.delete("${SampleApiRoutes.ME_FAVORITES}/list-items:7") { withSession(cookie) }

        assertEquals(HttpStatusCode.UnsupportedMediaType, put.status)
        assertEquals(HttpStatusCode.UnsupportedMediaType, delete.status)
        assertEquals(emptyList<String>(), client.favoritesOf(cookie))
    }

    private suspend fun HttpClient.putFavorite(cookie: Cookie?, id: String): HttpResponse =
        put("${SampleApiRoutes.ME_FAVORITES}/$id") {
            cookie?.let { withSession(it) }
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

    private suspend fun HttpClient.deleteFavorite(cookie: Cookie?, id: String): HttpResponse =
        delete("${SampleApiRoutes.ME_FAVORITES}/$id") {
            cookie?.let { withSession(it) }
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

    private suspend fun HttpClient.favoritesOf(cookie: Cookie): List<String> =
        idsOf(get(SampleApiRoutes.ME_FAVORITES) { withSession(cookie) })

    private suspend fun idsOf(response: HttpResponse): List<String> {
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject["contentIds"]!!
            .jsonArray.map { it.jsonPrimitive.content }
    }
}
