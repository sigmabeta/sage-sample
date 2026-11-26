package net.sigmabeta.sage.sample.server

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.SignInLimiter
import net.sigmabeta.sage.sample.identity.SignInService
import net.sigmabeta.sage.sample.identity.fake.FakeClock
import net.sigmabeta.sage.sample.identity.fake.FakeSessionStore
import net.sigmabeta.sage.sample.identity.fake.FakeUserDirectory
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import kotlin.time.Instant

/**
 * A [ServerIdentity] over the in-memory fakes with the Demo accounts seeded, and a clock the test
 * moves by hand. The real [SignInLimiter] and [SignInService] run over it.
 */
internal class TestIdentity {
    val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
    val users = FakeUserDirectory()
    val sessions = FakeSessionStore(clock)
    val identity = ServerIdentity(
        signIn = SignInService(users, sessions, SignInLimiter(clock)),
        sessions = sessions,
    )

    init {
        runBlocking { DemoAccounts.seed(users) }
    }
}

/** POSTs a Sign in with a JSON body, the way the browser client sends one. */
internal suspend fun HttpClient.signIn(
    username: String = DemoAccounts.DEMO.username,
    password: String = DemoAccounts.DEMO.password,
    configure: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = post(SampleApiRoutes.SESSION) {
    contentType(ContentType.Application.Json)
    setBody("""{"username":"$username","password":"$password"}""")
    configure()
}

/** Signs in as the Demo account and returns the Session cookie to send with later requests. */
internal suspend fun HttpClient.signedInCookie(): Cookie = signIn().sessionCookie()
    ?: error("Sign in did not set a Session cookie")

/** The Session cookie this response sets, parsed with its attributes, or `null`. */
internal fun HttpResponse.sessionCookie(): Cookie? = headers.getAll(HttpHeaders.SetCookie)
    .orEmpty()
    .map { parseServerSetCookieHeader(it) }
    .firstOrNull { it.name == SESSION_COOKIE }

/** Presents [cookie] the way a browser would. */
internal fun HttpRequestBuilder.withSession(cookie: Cookie) {
    header(HttpHeaders.Cookie, "${cookie.name}=${cookie.value}")
}
