package net.sigmabeta.sage.sample.server

import net.sigmabeta.sage.sample.database.FavoriteDao
import net.sigmabeta.sage.sample.database.FavoriteEntity
import net.sigmabeta.sage.sample.database.SampleDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Clock

/**
 * User-scoped favorites as the `/api/me/favorites` routes see them: every call names the User the
 * route resolved from the Session, and a User's data is never reachable through another's id.
 */
interface FavoritesStore {
    /** [userId]'s favorites, oldest first. */
    suspend fun list(userId: String): List<String>

    /** Favorites [contentId] for [userId]; already a favorite is a no-op. */
    suspend fun add(userId: String, contentId: String)

    /** Un-favorites [contentId] for [userId]; not a favorite is a no-op. */
    suspend fun remove(userId: String, contentId: String)
}

/** The shipped store: favorites in the server's [SampleDatabase], beside the Users that own them. */
class RoomFavoritesStore(
    private val dao: FavoriteDao,
    private val clock: Clock = Clock.System,
) : FavoritesStore {
    constructor(database: SampleDatabase) : this(database.favoriteDao())

    override suspend fun list(userId: String): List<String> = dao.listForUser(userId).map { it.contentId }

    override suspend fun add(userId: String, contentId: String) =
        dao.add(FavoriteEntity(userId, contentId, createdAt = clock.now().toEpochMilliseconds()))

    override suspend fun remove(userId: String, contentId: String) = dao.remove(userId, contentId)
}

/** Favorites held in memory, for tests and for a server wired without a database. */
class InMemoryFavoritesStore : FavoritesStore {
    private val byUser = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()

    override suspend fun list(userId: String): List<String> = byUser[userId]?.toList().orEmpty()

    override suspend fun add(userId: String, contentId: String) {
        byUser.getOrPut(userId) { CopyOnWriteArrayList() }.addIfAbsent(contentId)
    }

    override suspend fun remove(userId: String, contentId: String) {
        byUser[userId]?.remove(contentId)
    }
}
