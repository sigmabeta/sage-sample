package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.User
import net.sigmabeta.sage.sample.settings.AnimationScale
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
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The browser's identity source against Ktor's MockEngine standing in for `apps:server`'s
 * `/api/session` routes, and the 401 → Signed out hook on content calls.
 */
class RemoteIdentityRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val session = RemoteSessionState()

    /** Answers `GET /api/session` with [sessionStatus] unless [handler] answers first. */
    private var sessionStatus = HttpStatusCode.Unauthorized
    private var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData? = { null }

    private val api: SampleApiClient by lazy {
        val engine = MockEngine { request ->
            requests += request
            handler(request) ?: when {
                request.url.encodedPath == SampleApiRoutes.SESSION && request.method == HttpMethod.Get ->
                    if (sessionStatus == HttpStatusCode.OK) json(SIGNED_IN_BODY) else json(SIGNED_OUT_BODY, sessionStatus)

                else -> json("""{"items":[]}""")
            }
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        SampleApiClient(http, "http://sample.test", session)
    }

    private fun TestScope.repository(scope: CoroutineScope = backgroundScope) = RemoteIdentityRepository(api, session, scope)

    private suspend fun RemoteIdentityRepository.settled(): IdentityState = state.first { it != IdentityState.Checking }

    @Test
    fun `starts checking and settles signed in when GET session is 200`() = runTest {
        sessionStatus = HttpStatusCode.OK
        val repository = repository()
        assertEquals(IdentityState.Checking, repository.state.value)

        assertEquals(IdentityState.SignedIn(DEMO), repository.settled())
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("http://sample.test/api/session", requests.single().url.toString())
    }

    @Test
    fun `settles signed out when GET session is 401`() = runTest {
        assertEquals(IdentityState.SignedOut, repository().settled())
    }

    @Test
    fun `settles signed out when the startup check cannot reach the server`() = runTest {
        handler = { throw IOException("offline") }
        assertEquals(IdentityState.SignedOut, repository().settled())
    }

    @Test
    fun `accepted sign in posts JSON credentials and signs in`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request -> if (request.method == HttpMethod.Post) json(SIGNED_IN_BODY) else null }

        val outcome = repository.signIn("demo", "sage")

        assertEquals(SignInOutcome.Accepted(DEMO), outcome)
        assertEquals(IdentityState.SignedIn(DEMO), repository.state.value)
        val post = requests.last()
        assertEquals("http://sample.test/api/session", post.url.toString())
        assertEquals(ContentType.Application.Json, post.body.contentType?.withoutParameters())
        assertEquals(
            Json.parseToJsonElement("""{"username":"demo","password":"sage"}"""),
            Json.parseToJsonElement(post.body.toByteArray().decodeToString()),
        )
    }

    @Test
    fun `401 on sign in is rejected and stays signed out`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request ->
            if (request.method == HttpMethod.Post) json("""{"error":"Wrong username or password"}""", HttpStatusCode.Unauthorized) else null
        }

        assertEquals(SignInOutcome.Rejected, repository.signIn("demo", "nope"))
        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    @Test
    fun `429 on sign in is throttled for the Retry-After seconds`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request ->
            if (request.method == HttpMethod.Post) {
                respond(
                    content = """{"error":"Too many attempts"}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        HttpHeaders.RetryAfter to listOf("42"),
                    ),
                )
            } else {
                null
            }
        }

        assertEquals(SignInOutcome.Throttled(42.seconds), repository.signIn("demo", "sage"))
    }

    @Test
    fun `429 without a usable Retry-After is throttled with no wait`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request ->
            if (request.method == HttpMethod.Post) json("""{"error":"Too many attempts"}""", HttpStatusCode.TooManyRequests) else null
        }

        assertEquals(SignInOutcome.Throttled(Duration.ZERO), repository.signIn("demo", "sage"))
    }

    @Test
    fun `a server error on sign in is unreachable`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request ->
            if (request.method == HttpMethod.Post) json("""{"error":"boom"}""", HttpStatusCode.InternalServerError) else null
        }

        assertEquals(SignInOutcome.Unreachable, repository.signIn("demo", "sage"))
        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    @Test
    fun `a network failure on sign in is unreachable`() = runTest {
        val repository = repository()
        repository.settled()
        handler = { request -> if (request.method == HttpMethod.Post) throw IOException("offline") else null }

        assertEquals(SignInOutcome.Unreachable, repository.signIn("demo", "sage"))
    }

    @Test
    fun `sign out sends a JSON DELETE and signs out`() = runTest {
        sessionStatus = HttpStatusCode.OK
        val repository = repository()
        repository.settled()
        handler = { request -> if (request.method == HttpMethod.Delete) respond("", HttpStatusCode.NoContent) else null }

        repository.signOut()

        assertEquals(IdentityState.SignedOut, repository.state.value)
        val delete = requests.last()
        assertEquals(HttpMethod.Delete, delete.method)
        assertEquals("http://sample.test/api/session", delete.url.toString())
        assertEquals(ContentType.Application.Json, delete.body.contentType?.withoutParameters())
    }

    @Test
    fun `sign out signs out locally even when the server cannot be reached`() = runTest {
        sessionStatus = HttpStatusCode.OK
        val repository = repository()
        repository.settled()
        handler = { request -> if (request.method == HttpMethod.Delete) throw IOException("offline") else null }

        repository.signOut()

        assertEquals(IdentityState.SignedOut, repository.state.value)
    }

    @Test
    fun `a content call answered 401 signs out, emits nothing, and retries once signed in again`() = runTest {
        sessionStatus = HttpStatusCode.OK
        val repository = repository()
        repository.settled()
        var contentStatus = HttpStatusCode.Unauthorized
        handler = { request ->
            when {
                request.url.encodedPath == SampleApiRoutes.LIST_ITEMS ->
                    json("""{"items":[{"id":1,"name":"Item 1","caption":"c"}]}""", contentStatus)
                        .also { if (contentStatus == HttpStatusCode.Unauthorized) contentStatus = HttpStatusCode.OK }

                request.method == HttpMethod.Post -> json(SIGNED_IN_BODY)

                else -> null
            }
        }
        val items = RemoteSampleItemRepository(api, ContentSettings())

        val emissions = mutableListOf<Any>()
        val collecting = launch { items.load(offset = 0, limit = 1).toList(emissions) }
        repository.state.first { it == IdentityState.SignedOut }

        assertTrue("no LCE.Error row for a 401", emissions.isEmpty())
        assertTrue(collecting.isActive)

        repository.signIn("demo", "sage")
        collecting.join()

        assertEquals(1, emissions.size)
        assertTrue(emissions.single() is LCE.Content<*>)
    }

    @Test
    fun `a 401 for a request sent before a newer sign in does not sign that sign in out`() = runTest {
        sessionStatus = HttpStatusCode.OK
        repository().settled()
        val signedInAgain = IdentityState.SignedIn(User("user-2", "demo2"))

        session.rejected(sentWith = IdentityState.SignedIn(DEMO))
        assertEquals(IdentityState.SignedOut, session.state.value)

        session.signedIn(signedInAgain.user)
        session.rejected(sentWith = IdentityState.SignedOut)

        assertEquals(signedInAgain, session.state.value)
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))

    /** The content repositories read the demo outcome; this one always asks the server. */
    private class ContentSettings : SampleSettingsManager {
        override val themeMode: StateFlow<ThemeMode> = MutableStateFlow(ThemeMode.DEFAULT)
        override val fontChoice: StateFlow<FontChoice> = MutableStateFlow(FontChoice.DEFAULT)
        override val simulateDelay: StateFlow<SimulateDelay> = MutableStateFlow(SimulateDelay.DEFAULT)
        override val homeShowsSameContent: StateFlow<Boolean> = MutableStateFlow(false)
        override val animationScale: StateFlow<AnimationScale> = MutableStateFlow(AnimationScale.DEFAULT)
        override val demoOutcome: StateFlow<DemoOutcome> = MutableStateFlow(DemoOutcome.CONTENT)
        override fun setThemeMode(mode: ThemeMode) = Unit
        override fun setFontChoice(choice: FontChoice) = Unit
        override fun setSimulateDelay(delay: SimulateDelay) = Unit
        override fun setAnimationScale(scale: AnimationScale) = Unit
        override fun setHomeShowsSameContent(showSameContent: Boolean) = Unit
        override fun setDemoOutcome(outcome: DemoOutcome) = Unit
    }

    private companion object {
        val DEMO = User(id = "user-1", username = "demo")
        const val SIGNED_IN_BODY = """{"user":{"id":"user-1","username":"demo"}}"""
        const val SIGNED_OUT_BODY = """{"error":"Signed out"}"""
    }
}
