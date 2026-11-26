package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess

/**
 * The browser client's typed GET helper over the sample's JSON API.
 *
 * Every path and query parameter comes from [net.sigmabeta.sage.sample.repository.SampleApiRoutes]
 * and [net.sigmabeta.sage.sample.repository.SampleApiParams], so the client and the server cannot
 * drift. [baseUrl] is the page origin; paths are absolute. The Session travels as the browser's
 * HttpOnly cookie on these same-origin requests, so no token is handled here.
 *
 * A content call the server answers 401 is not an error the screen shows: it reports the refusal to
 * [session] (the Sign-in gate then shows Login over the current route), waits until the client is
 * Signed in again, and retries. The calling screen stays in its loading state meanwhile.
 */
class SampleApiClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val session: RemoteSessionState,
) {
    /** Performs a GET and decodes the JSON body, failing with the server's message on non-2xx. */
    suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): T {
        while (true) {
            val sentWith = session.state.value
            val response = httpClient.get(url(path)) {
                url {
                    params.forEach { (key, value) -> parameters.append(key, value) }
                }
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                session.rejected(sentWith)
                session.awaitSignedIn()
                continue
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText().takeIf { it.isNotBlank() }
                throw IllegalStateException(
                    "GET $path failed (${response.status.value})" + (body?.let { ": $it" } ?: ""),
                )
            }
            return response.body()
        }
    }

    /**
     * Sends a state-changing [method] request (an empty JSON object body, as the server requires
     * `application/json`) and fails on a non-2xx answer.
     *
     * Unlike [get], a 401 is never retried after the next Sign in: that would apply this User's
     * change to whoever signs in next. It reports the refusal to [session] and fails instead.
     */
    suspend fun send(method: HttpMethod, path: String) {
        val sentWith = session.state.value
        val response = httpClient.request(url(path)) {
            this.method = method
            setBody(TextContent("{}", ContentType.Application.Json))
        }
        if (response.status == HttpStatusCode.Unauthorized) session.rejected(sentWith)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("${method.value} $path failed (${response.status.value})")
        }
    }

    @PublishedApi
    internal fun url(path: String): String = baseUrl.trimEnd('/') + path
}
