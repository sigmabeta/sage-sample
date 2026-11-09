package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The browser's HTTP repositories against Ktor's MockEngine: paths, query parameters, wire
 * decoding, and the client-side demo-outcome switch.
 */
class RemoteRepositoriesTest {
    private val requests = mutableListOf<String>()
    private val settings = FakeSettings()

    private fun client(
        handler: (String) -> Pair<HttpStatusCode, String> = { url ->
        HttpStatusCode.OK to responseFor(url)
    }
    ): SampleApiClient {
        val engine = MockEngine { request ->
            requests += request.url.toString()
            val (status, body) = handler(request.url.toString())
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return SampleApiClient(http, "http://sample.test")
    }

    @Test
    fun `home count and item use the documented endpoints`() = runTest {
        val repository = RemoteHomeRepository(client(), settings)

        assertEquals(18, repository.itemCount("big_image_1"))
        assertEquals("Item 3", repository.itemAt("big_image_1", 3)?.title)

        assertTrue(requests[0].startsWith("http://sample.test/api/home/count"))
        assertTrue(requests[0].contains("seed=big_image_1"))
        assertTrue(requests[1].startsWith("http://sample.test/api/home/item"))
        assertTrue(requests[1].contains("index=3"))
    }

    @Test
    fun `home grid decodes the item list`() = runTest {
        val repository = RemoteHomeRepository(client(), settings)
        val result = repository.gridImages("grid_images_2", limit = 2).single()

        assertEquals(
            LCE.Content(listOf(net.sigmabeta.sage.sample.models.HomeItem(1, "Item 1"))),
            result,
        )
        assertTrue(requests.single().contains("limit=2"))
    }

    @Test
    fun `empty and error outcomes are applied client-side without a request`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)
        val repository = RemoteHomeRepository(client(), settings)
        assertEquals(0, repository.itemCount("big_image_1"))
        assertEquals(
            LCE.Content(emptyList<HomeItem>()),
            repository.gridImages("s", 1).single(),
        )
        assertTrue(requests.isEmpty())

        settings.setDemoOutcome(DemoOutcome.ERROR)
        assertTrue(
            runCatching { repository.itemCount("big_image_1") }.exceptionOrNull()
                is IllegalStateException,
        )
        val grid = repository.gridImages("s", 1).single()
        assertTrue(grid is LCE.Error)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `search sends the query and kind and decodes results`() = runTest {
        val repository = RemoteSearchRepository(client(), settings)
        val results = repository.searchList("chrono").toList().single()

        assertEquals(1, (results as LCE.Content).data.size)
        assertTrue(requests.single().contains("q=chrono"))
        assertTrue(requests.single().contains("kind=list"))
    }

    @Test
    fun `content detail decodes a detail and a null not-found`() = runTest {
        val detail = RemoteContentDetailRepository(client()).load(ContentId("list-items:7"))
            .single() as LCE.Content
        assertEquals(ContentId("list-items:7"), detail.data?.id)
        assertTrue(requests.single().contains("id=list-items%3A7"))

        val missing = RemoteContentDetailRepository(client { HttpStatusCode.OK to """{"detail":null}""" })
            .load(ContentId("nope:1"))
            .single() as LCE.Content
        assertEquals(null, missing.data)
    }

    @Test
    fun `a non-2xx response surfaces the server message`() = runTest {
        val repository = RemoteHomeRepository(
            client { HttpStatusCode.NotFound to """{"error":"Not found"}""" },
            settings,
        )
        val error = runCatching { repository.itemCount("s") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("Not found"))
    }

    private fun responseFor(url: String): String = when {
        "/api/home/count" in url -> """{"count":18}"""

        "/api/home/item" in url -> """{"item":{"id":4,"title":"Item 3"}}"""

        "/api/home/grid" in url -> """{"items":[{"id":1,"title":"Item 1"}]}"""

        "/api/search" in url ->
            """{"results":[{"id":1,"name":"Result","caption":"Caption"}]}"""

        "/api/content" in url ->
            """{"detail":{"id":"list-items:7","title":"Title","details":[],"actions":[],"related":[],"items":[]}}"""

        else -> "{}"
    }

    private class FakeSettings : SampleSettingsManager {
        override val themeMode: StateFlow<ThemeMode> = MutableStateFlow(ThemeMode.DEFAULT)
        override val fontChoice: StateFlow<FontChoice> = MutableStateFlow(FontChoice.DEFAULT)
        override val simulateDelay: StateFlow<SimulateDelay> = MutableStateFlow(SimulateDelay.DEFAULT)
        override val homeShowsSameContent: StateFlow<Boolean> = MutableStateFlow(true)

        private val outcome = MutableStateFlow(DemoOutcome.DEFAULT)
        override val demoOutcome: StateFlow<DemoOutcome> = outcome

        override fun setThemeMode(mode: ThemeMode) = Unit
        override fun setFontChoice(choice: FontChoice) = Unit
        override fun setSimulateDelay(delay: SimulateDelay) = Unit
        override fun setHomeShowsSameContent(showSameContent: Boolean) = Unit
        override fun setDemoOutcome(outcome: DemoOutcome) {
            this.outcome.value = outcome
        }
    }
}
