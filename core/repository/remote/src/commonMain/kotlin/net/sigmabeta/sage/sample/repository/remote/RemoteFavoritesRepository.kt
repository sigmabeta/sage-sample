package net.sigmabeta.sage.sample.repository.remote

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.CachedFavoritesRepository
import net.sigmabeta.sage.sample.repository.FavoritesRepository
import net.sigmabeta.sage.sample.repository.FavoritesResponse
import net.sigmabeta.sage.sample.repository.FavoritesSource
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.SampleApiRoutes
import net.sigmabeta.sage.sample.repository.User

/**
 * The browser's [FavoritesRepository]: `apps:server`'s `/api/me/favorites` behind the shared
 * User-keyed cache. The server resolves the User from the Session cookie, so a request names none.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RemoteFavoritesRepository(
    identity: IdentityRepository,
    api: SampleApiClient,
    scope: CoroutineScope,
) : FavoritesRepository by CachedFavoritesRepository(identity, RemoteFavoritesSource(api), scope)

internal class RemoteFavoritesSource(private val api: SampleApiClient) : FavoritesSource {
    override suspend fun load(user: User): Set<ContentId> =
        api.get<FavoritesResponse>(SampleApiRoutes.ME_FAVORITES).contentIds
            .mapNotNullTo(linkedSetOf()) { runCatching { ContentId(it) }.getOrNull() }

    override suspend fun set(user: User, contentId: ContentId, favorite: Boolean) {
        api.send(if (favorite) HttpMethod.Put else HttpMethod.Delete, SampleApiRoutes.meFavorite(contentId.value))
    }
}
