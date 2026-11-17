package net.sigmabeta.sage.sample.repository.remote

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.SessionResponse
import net.sigmabeta.sage.sample.repository.SignInOutcome
import net.sigmabeta.sage.sample.repository.SignInRequest
import net.sigmabeta.sage.sample.repository.User
import net.sigmabeta.sage.sample.repository.UserDto
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The browser's [IdentityRepository] over `apps:server`'s [SampleApiRoutes.SESSION] routes.
 *
 * At startup it asks `GET /api/session` whether the browser's cookie still names a live Session:
 * 200 is [IdentityState.SignedIn], and 401 or no answer is [IdentityState.SignedOut]. The cookie is
 * HttpOnly and managed by the browser; this class never sees the token.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteIdentityRepository(
    private val api: SampleApiClient,
    private val session: RemoteSessionState,
    scope: CoroutineScope,
) : IdentityRepository {
    override val state: StateFlow<IdentityState> = session.state

    init {
        scope.launch { session.settled(startupState()) }
    }

    override suspend fun signIn(username: String, password: String): SignInOutcome {
        val response = attempt {
            api.httpClient.post(api.url(SampleApiRoutes.SESSION)) {
                contentType(ContentType.Application.Json)
                setBody(SignInRequest(username = username, password = password))
            }
        } ?: return SignInOutcome.Unreachable

        return when (response.status) {
            HttpStatusCode.OK -> {
                val user = response.body<SessionResponse>().user.toDomain()
                session.signedIn(user)
                SignInOutcome.Accepted(user)
            }

            HttpStatusCode.Unauthorized -> SignInOutcome.Rejected

            HttpStatusCode.TooManyRequests -> SignInOutcome.Throttled(response.retryAfter())

            // 5xx, or a 4xx the client should never cause (a malformed body): no usable answer.
            else -> SignInOutcome.Unreachable
        }
    }

    /**
     * Signs out locally whatever the server answers: the route always answers 204, and if it cannot
     * be reached the cookie outlives this client's state only until its Session is refused.
     */
    override suspend fun signOut() {
        attempt {
            // The server requires the JSON content type on DELETE too, and a body-less request
            // would not carry it.
            api.httpClient.delete(api.url(SampleApiRoutes.SESSION)) {
                setBody(TextContent("{}", ContentType.Application.Json))
            }
        }
        session.signedOut()
    }

    private suspend fun startupState(): IdentityState {
        val response = attempt { api.httpClient.get(api.url(SampleApiRoutes.SESSION)) }
        return if (response?.status == HttpStatusCode.OK) {
            IdentityState.SignedIn(response.body<SessionResponse>().user.toDomain())
        } else {
            IdentityState.SignedOut
        }
    }

    /** Runs one request, mapping a transport failure to `null`. */
    private suspend fun attempt(request: suspend () -> HttpResponse): HttpResponse? = try {
        request()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        null
    }
}

/** `Retry-After` in whole seconds; a missing or unparseable header means no wait. */
private fun HttpResponse.retryAfter(): Duration =
    headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()?.coerceAtLeast(0)?.seconds ?: Duration.ZERO

private fun UserDto.toDomain(): User = User(id = id, username = username)
