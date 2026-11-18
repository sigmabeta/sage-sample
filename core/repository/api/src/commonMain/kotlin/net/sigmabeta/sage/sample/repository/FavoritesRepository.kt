package net.sigmabeta.sage.sample.repository

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.sample.models.ContentId

/**
 * The signed-in User's favorites: User-scoped data, resolved from the Session and never from an id a
 * caller supplies. The Android and desktop apps bind the in-process implementation in
 * `:core:repository:demo`; the browser binds the HTTP one in `:core:repository:remote`. Both put
 * `:core:repository:cache`'s User-keyed cache in front of their store.
 */
interface FavoritesRepository {
    /**
     * The Content ids the current User has favorited, emitting again after every change. Empty while
     * the client is not Signed in, and never carries another User's favorites.
     */
    val favorites: Flow<Set<ContentId>>

    /**
     * Favorites or un-favorites [contentId] for the current User. Returns `false`, leaving
     * [favorites] as it was, when the client is Signed out or the change could not be saved.
     */
    suspend fun setFavorite(contentId: ContentId, favorite: Boolean): Boolean
}
