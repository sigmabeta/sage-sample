package net.sigmabeta.sage.sample.server

import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.identity.RoomSessionStore
import net.sigmabeta.sage.sample.identity.RoomUserDirectory
import net.sigmabeta.sage.sample.identity.SESSION_LIFETIME
import net.sigmabeta.sage.sample.identity.SignInLimiter
import net.sigmabeta.sage.sample.identity.UserDirectory
import net.sigmabeta.sage.sample.identity.Session
import net.sigmabeta.sage.sample.identity.SessionStore
import net.sigmabeta.sage.sample.identity.SignInResult
import net.sigmabeta.sage.sample.identity.SignInService
import net.sigmabeta.sage.sample.identity.User
import net.sigmabeta.sage.sample.repository.ApiErrorResponse
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.SessionResponse
import net.sigmabeta.sage.sample.repository.SignInRequest
import net.sigmabeta.sage.sample.repository.UserDto
import kotlin.time.Duration

/** The cookie that carries a Session token. HttpOnly, so page script never sees it. */
const val SESSION_COOKIE = "sage_session"

/**
 * What the routes need from `:core:identity`: [signIn] opens Sessions (limiter, password, store, as
 * one rule) and [sessions] resolves and revokes the token a request presents.
 */
class ServerIdentity(
    val signIn: SignInService,
    val sessions: SessionStore,
) {
    companion object {
        /**
         * The shipped identity: Users and Sessions in [database], with the Demo accounts seeded (an
         * idempotent step, so it runs on every start) and an in-memory [SignInLimiter].
         */
        suspend fun overRoom(
            database: SampleDatabase,
            users: UserDirectory = RoomUserDirectory(database),
        ): ServerIdentity {
            DemoAccounts.seed(users)
            val sessions = RoomSessionStore(database)
            return ServerIdentity(SignInService(users, sessions, SignInLimiter()), sessions)
        }
    }
}

/**
 * `POST`/`GET`/`DELETE` [SampleApiRoutes.SESSION]: Sign in, read the current Session, Sign out.
 *
 * Sign in and Sign out must be `application/json`. Besides naming the body format, that makes a
 * cross-site form post impossible and forces a browser to preflight a cross-origin call, on top of
 * the cookie's `SameSite=Lax`.
 */
internal fun Route.sessionRoutes(identity: ServerIdentity, secureCookie: Boolean) {
    post(SampleApiRoutes.SESSION) {
        if (!call.requireJson()) return@post
        val request = call.receive<SignInRequest>()
        when (val result = identity.signIn.signIn(request.username, request.password, call.request.origin.remoteHost)) {
            is SignInResult.SignedIn -> {
                call.response.cookies.append(sessionCookie(result.issued.token, SESSION_LIFETIME, secureCookie))
                call.respond(SessionResponse(result.issued.session.user.toDto()))
            }

            SignInResult.Rejected -> call.respond(
                HttpStatusCode.Unauthorized,
                // One message for both causes: a reply must not reveal which usernames exist.
                ApiErrorResponse("Wrong username or password"),
            )

            is SignInResult.Throttled -> {
                call.response.header(HttpHeaders.RetryAfter, result.retryAfter.wholeSecondsRoundedUp())
                call.respond(HttpStatusCode.TooManyRequests, ApiErrorResponse("Too many attempts; try again later"))
            }
        }
    }

    get(SampleApiRoutes.SESSION) {
        val session = call.currentSession(identity.sessions)
        if (session == null) {
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse("Signed out"))
        } else {
            call.respond(SessionResponse(session.user.toDto()))
        }
    }

    delete(SampleApiRoutes.SESSION) {
        if (!call.requireJson()) return@delete
        call.sessionToken()?.let { identity.sessions.revoke(it) }
        call.response.cookies.append(sessionCookie("", Duration.ZERO, secureCookie))
        call.respond(HttpStatusCode.NoContent)
    }
}

/** The token in the request's Session cookie, if it sent one. */
internal fun ApplicationCall.sessionToken(): String? =
    request.cookies[SESSION_COOKIE, CookieEncoding.RAW]?.takeIf { it.isNotEmpty() }

/** The live Session this request's cookie names, or `null` when it is Signed out. */
internal suspend fun ApplicationCall.currentSession(sessions: SessionStore): Session? =
    sessionToken()?.let { sessions.lookup(it) }

/** Responds 415 and returns false unless the request body is declared `application/json`. */
internal suspend fun ApplicationCall.requireJson(): Boolean {
    if (request.contentType().match(ContentType.Application.Json)) return true
    respond(HttpStatusCode.UnsupportedMediaType, ApiErrorResponse("Expected application/json"))
    return false
}

private fun sessionCookie(value: String, maxAge: Duration, secure: Boolean) = Cookie(
    name = SESSION_COOKIE,
    value = value,
    encoding = CookieEncoding.RAW,
    maxAge = maxAge.inWholeSeconds.toInt(),
    path = "/",
    secure = secure,
    httpOnly = true,
    extensions = mapOf("SameSite" to "Lax"),
)

private fun Duration.wholeSecondsRoundedUp(): String {
    val millis = inWholeMilliseconds
    return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(1).toString()
}

private const val MILLIS_PER_SECOND = 1_000L

private fun User.toDto() = UserDto(id = id, username = username)
