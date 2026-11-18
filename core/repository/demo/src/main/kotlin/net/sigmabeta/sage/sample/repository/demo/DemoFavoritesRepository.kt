package net.sigmabeta.sage.sample.repository.demo

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.database.FavoriteEntity
import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.repository.CachedFavoritesRepository
import net.sigmabeta.sage.sample.repository.FavoritesRepository
import net.sigmabeta.sage.sample.repository.FavoritesSource
import net.sigmabeta.sage.sample.repository.IdentityRepository
import net.sigmabeta.sage.sample.repository.User
import kotlin.time.Clock

/**
 * The Android and desktop apps' [FavoritesRepository]: favorites are rows in the app's
 * [SampleDatabase], keyed by the User id of the Session, behind the shared User-keyed cache.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoFavoritesRepository(
    identity: IdentityRepository,
    database: SampleDatabase,
    scope: CoroutineScope,
) : FavoritesRepository by CachedFavoritesRepository(identity, DemoFavoritesSource(database), scope)

/** [FavoritesSource] over the `favorites` table. */
internal class DemoFavoritesSource(
    database: SampleDatabase,
    private val clock: Clock = Clock.System,
) : FavoritesSource {
    private val dao = database.favoriteDao()

    override suspend fun load(user: User): Set<ContentId> =
        dao.listForUser(user.id).mapTo(linkedSetOf()) { ContentId(it.contentId) }

    override suspend fun set(user: User, contentId: ContentId, favorite: Boolean) {
        if (favorite) {
            dao.add(FavoriteEntity(user.id, contentId.value, createdAt = clock.now().toEpochMilliseconds()))
        } else {
            dao.remove(user.id, contentId.value)
        }
    }
}
