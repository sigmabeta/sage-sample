package net.sigmabeta.sage.sample.repository.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * The browser client's typed GET helper over the sample's JSON API.
 *
 * Every path and query parameter comes from [net.sigmabeta.sage.sample.repository.SampleApiRoutes]
 * and [net.sigmabeta.sage.sample.repository.SampleApiParams], so the client and the server cannot
 * drift. [baseUrl] is the page origin (or a development override); paths are absolute.
 */
class SampleApiClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val baseUrl: String,
) {
    /** Performs a GET and decodes the JSON body, failing with the server's message on non-2xx. */
    suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): T {
        val response = httpClient.get(baseUrl.trimEnd('/') + path) {
            url {
                params.forEach { (key, value) -> parameters.append(key, value) }
            }
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
