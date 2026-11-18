package net.sigmabeta.sage.sample.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import net.sigmabeta.sage.sample.models.contentIdOrNull
import net.sigmabeta.sage.sample.repository.ApiErrorResponse
import net.sigmabeta.sage.sample.repository.FavoritesResponse
import net.sigmabeta.sage.sample.repository.SampleApiRoutes

/**
 * `GET`/`PUT`/`DELETE` [SampleApiRoutes.ME_FAVORITES]: the signed-in User's favorites.
 *
 * Declared inside [requireSession], so every route here already has a Session and reads its User
 * from [signedInUser]; none accepts a user id. Changing a favorite must be `application/json`, for
 * the same cross-site reasons as Sign in and Sign out.
 */
internal fun Route.favoritesRoutes(favorites: FavoritesStore) {
    get(SampleApiRoutes.ME_FAVORITES) {
        call.respond(FavoritesResponse(favorites.list(call.signedInUser.id)))
    }

    put("${SampleApiRoutes.ME_FAVORITES}/{${SampleApiRoutes.ME_FAVORITE_PARAM}}") {
        val contentId = call.favoriteContentIdOrRespond() ?: return@put
        favorites.add(call.signedInUser.id, contentId)
        call.respond(HttpStatusCode.NoContent)
    }

    delete("${SampleApiRoutes.ME_FAVORITES}/{${SampleApiRoutes.ME_FAVORITE_PARAM}}") {
        val contentId = call.favoriteContentIdOrRespond() ?: return@delete
        favorites.remove(call.signedInUser.id, contentId)
        call.respond(HttpStatusCode.NoContent)
    }
}

/** The addressed Content id, or `null` after answering 415 (not JSON) or 400 (not a Content id). */
private suspend fun io.ktor.server.application.ApplicationCall.favoriteContentIdOrRespond(): String? {
    if (!requireJson()) return null
    val raw = parameters[SampleApiRoutes.ME_FAVORITE_PARAM]
    val contentId = raw?.let { contentIdOrNull(it) }
    if (contentId == null) {
        respond(HttpStatusCode.BadRequest, ApiErrorResponse("Missing or invalid 'contentId'"))
        return null
    }
    return contentId.value
}
