package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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

    @PublishedApi
    internal fun url(path: String): String = baseUrl.trimEnd('/') + path
}
