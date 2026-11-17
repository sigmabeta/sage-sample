package net.sigmabeta.sage.sample.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import net.sigmabeta.sage.sample.identity.SessionStore
import net.sigmabeta.sage.sample.repository.ApiErrorResponse

/**
 * Routes declared in [build] answer only a request whose Session cookie names a live Session; any
 * other request gets a 401 `{"error":"Sign in required"}` before the route runs.
 *
 * The gate is a transparent routing node (like Ktor's `authenticate {}`), so it adds no path segment
 * and a route opts in by being declared inside it rather than by matching a path prefix.
 */
internal fun Route.requireSession(sessions: SessionStore, build: Route.() -> Unit): Route =
    createChild(SessionGateSelector).apply {
        install(RequireSession) { this.sessions = sessions }
        build()
    }

private object SessionGateSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(requireSession)"
}

internal class RequireSessionConfig {
    lateinit var sessions: SessionStore
}

private val RequireSession = createRouteScopedPlugin("RequireSession", ::RequireSessionConfig) {
    val sessions = pluginConfig.sessions
    onCall { call ->
        if (call.currentSession(sessions) == null) {
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse("Sign in required"))
        }
    }
}
