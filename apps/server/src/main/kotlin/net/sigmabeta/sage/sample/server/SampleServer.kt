package net.sigmabeta.sage.sample.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import net.sigmabeta.sage.sample.models.contentIdOrNull
import net.sigmabeta.sage.sample.repository.ApiErrorResponse
import net.sigmabeta.sage.sample.repository.ContentDetailResponse
import net.sigmabeta.sage.sample.repository.HealthResponse
import net.sigmabeta.sage.sample.repository.HomeCountResponse
import net.sigmabeta.sage.sample.repository.HomeItemResponse
import net.sigmabeta.sage.sample.repository.HomeItemsResponse
import net.sigmabeta.sage.sample.repository.SampleApiParams
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.SampleItemsResponse
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.SearchResultsResponse
import net.sigmabeta.sage.sample.repository.normalizedPage
import net.sigmabeta.sage.sample.repository.toDto
import net.sigmabeta.sage.sample.repository.demo.DemoContent

/**
 * The sample's HTTP surface: a JSON API over the deterministic [DemoContent] plus the browser
 * bundle.
 *
 * The browser app's `RemoteRepository` implementations call these routes. The route bodies are the
 * only place the server touches data; every generator lives in `:core:repository:demo`, which the
 * Android/desktop demo sources also use.
 *
 * The same function is mounted by `main()` and by the route tests, so tests exercise the shipped
 * stack.
 */
fun Application.configureSampleServer() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorResponse(cause.message ?: "Internal server error"),
            )
        }
    }

    routing {
        get(SampleApiRoutes.HEALTH) {
            call.respond(HealthResponse(ok = true))
        }

        get(SampleApiRoutes.HOME_COUNT) {
            val seed = call.parameters[SampleApiParams.SEED] ?: return@get call.badRequest("seed")
            call.respond(HomeCountResponse(DemoContent.homeItemCount(seed)))
        }

        get(SampleApiRoutes.HOME_ITEM) {
            val seed = call.parameters[SampleApiParams.SEED] ?: return@get call.badRequest("seed")
            val index = call.parameters[SampleApiParams.INDEX]?.toIntOrNull()
                ?: return@get call.badRequest("index")
            call.respond(HomeItemResponse(DemoContent.homeItem(seed, index)?.toDto()))
        }

        get(SampleApiRoutes.HOME_GRID) {
            val seed = call.parameters[SampleApiParams.SEED] ?: return@get call.badRequest("seed")
            val (_, limit) = normalizedPage(
                offset = null,
                limit = call.parameters[SampleApiParams.LIMIT]?.toIntOrNull(),
            )
            call.respond(HomeItemsResponse(DemoContent.homeGrid(seed, limit).map { it.toDto() }))
        }

        get(SampleApiRoutes.CONTENT_DETAIL) {
            val rawId = call.parameters[SampleApiParams.CONTENT_ID] ?: return@get call.badRequest("id")
            val contentId = contentIdOrNull(rawId)
            call.respond(
                ContentDetailResponse(
                    contentId?.let { DemoContent.contentDetail(it) }?.toDto(),
                ),
            )
        }

        get(SampleApiRoutes.LIST_ITEMS) {
            val (offset, limit) = call.page()
            call.respond(
                SampleItemsResponse(
                    DemoContent.sampleItems(
                        offset = offset,
                        limit = limit,
                        total = net.sigmabeta.sage.sample.repository.LIST_ITEMS_ITEM_COUNT,
                    ).map { it.toDto() },
                ),
            )
        }

        get(SampleApiRoutes.GRID_OF_IMAGES) {
            val (offset, limit) = call.page()
            call.respond(
                SampleItemsResponse(
                    DemoContent.sampleItems(
                        offset = offset,
                        limit = limit,
                        total = net.sigmabeta.sage.sample.repository.GRID_OF_IMAGES_ITEM_COUNT,
                    ).map { it.toDto() },
                ),
            )
        }

        get(SampleApiRoutes.SEARCH) {
            val query = call.parameters[SampleApiParams.QUERY] ?: return@get call.badRequest("q")
            val kindSegment = call.parameters[SampleApiParams.KIND] ?: return@get call.badRequest("kind")
            val kind = SearchResultKind.entries.firstOrNull { it.routeSegment == kindSegment }
                ?: return@get call.badRequest("kind")
            call.respond(
                SearchResultsResponse(
                    DemoContent.searchResults(query, kind).map { it.toDto() },
                ),
            )
        }

        // Unknown /api paths are a JSON 404, never the SPA shell.
        route("/api/{...}") {
            handle {
                call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Not found"))
            }
        }
        route("/api") {
            handle {
                call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Not found"))
            }
        }

        // Bundle assets come from resources; app-shaped paths get the SPA shell. A missing asset
        // stays a real 404 so a broken script/WASM URL surfaces as an error instead of HTML parsed
        // as JavaScript.
        get("/{...}") {
            val path = call.request.path().removePrefix("/")
            if (path.split('/').any { it == ".." }) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val resource = Thread.currentThread().contextClassLoader
                ?.getResource("static/$path")
            when {
                resource != null -> call.respondBytes(
                    resource.openStream().use { it.readBytes() },
                    ContentType.defaultForFilePath(path),
                )

                // App routes may contain dots (content IDs allow them); only a known asset
                // extension makes a resource miss a real 404.
                path.substringAfterLast('/').substringAfterLast('.', "")
                    .lowercase() in ASSET_EXTENSIONS -> call.respond(HttpStatusCode.NotFound)

                else -> call.respondIndexHtml()
            }
        }
        get("/") { call.respondIndexHtml() }
    }
}

/** Extensions the webpack/Skiko bundle can emit; a miss on one is a real 404, not the SPA shell. */
private val ASSET_EXTENSIONS = setOf(
    "js", "mjs", "wasm", "html", "css", "png", "ico", "svg", "json", "map",
    "woff", "woff2", "ttf", "otf",
)

private suspend fun io.ktor.server.application.ApplicationCall.badRequest(parameter: String) =
    respond(
        HttpStatusCode.BadRequest,
        ApiErrorResponse("Missing or invalid '$parameter'"),
    )

private fun io.ktor.server.application.ApplicationCall.page(): Pair<Int, Int> = normalizedPage(
    offset = parameters[SampleApiParams.OFFSET]?.toIntOrNull(),
    limit = parameters[SampleApiParams.LIMIT]?.toIntOrNull(),
)

private suspend fun io.ktor.server.application.ApplicationCall.respondIndexHtml() {
    val html = Thread.currentThread().contextClassLoader
        ?.getResource("static/index.html")
        ?.readText()
    if (html == null) {
        respond(
            HttpStatusCode.ServiceUnavailable,
            ApiErrorResponse(
                "The web bundle is not bundled into this server. " +
                    "Run `./gradlew :apps:server:run -Psage.js=true`.",
            ),
        )
    } else {
        respondText(html, ContentType.Text.Html)
    }
}
