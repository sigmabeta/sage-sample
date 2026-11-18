package net.sigmabeta.sage.sample.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sigmabeta.sage.sample.models.ContentId

/**
 * The signed-in User's favorites: User-scoped data, resolved from the Session and never from an id a
 * caller supplies. The Android and desktop apps bind the in-process implementation in
 * `:core:repository:demo`; the browser binds the HTTP one in `:core:repository:remote`.
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

/**
 * Where favorites are really kept: the in-process database on Android and desktop, `apps:server`
 * for the browser. [CachedFavoritesRepository] holds one User's copy in memory in front of it.
 */
interface FavoritesSource {
    /** Every favorite [user] holds. */
    suspend fun load(user: User): Set<ContentId>

    /** Saves one change for [user]; throws when it cannot be saved. */
    suspend fun set(user: User, contentId: ContentId, favorite: Boolean)
}

/**
 * A [FavoritesRepository] that keeps the signed-in User's favorites in memory, over a
 * [FavoritesSource].
 *
 * The cache is app-scoped, so it outlives a Sign out, which is why it is keyed by User: it holds one
 * User's set at a time, is only ever read for that User's id, and is dropped whenever [identity]
 * leaves [IdentityState.SignedIn] or switches to a different User. Signing in as `demo2` after
 * `demo` therefore reads `demo2`'s favorites from the source, never `demo`'s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CachedFavoritesRepository(
    private val identity: IdentityRepository,
    private val source: FavoritesSource,
    scope: CoroutineScope,
) : FavoritesRepository {
    private class Snapshot(val userId: String, val ids: Set<ContentId>)

    private val cache = MutableStateFlow<Snapshot?>(null)
    private val writes = Mutex()

    init {
        scope.launch {
            identity.state.collect { state ->
                val signedInAs = (state as? IdentityState.SignedIn)?.user?.id
                cache.update { held -> held?.takeIf { it.userId == signedInAs } }
            }
        }
    }

    override val favorites: Flow<Set<ContentId>> = identity.state.flatMapLatest { state ->
        if (state is IdentityState.SignedIn) {
            val user = state.user
            flow {
                if (!ensureLoaded(user)) return@flow emit(emptySet())
                emitAll(cache.filterNotNull().filterUser(user).map { it.ids })
            }
        } else {
            flowOf(emptySet())
        }
    }

    override suspend fun setFavorite(contentId: ContentId, favorite: Boolean): Boolean {
        val user = (identity.state.value as? IdentityState.SignedIn)?.user ?: return false
        return writes.withLock {
            if (!ensureLoaded(user)) return@withLock false
            val before = cache.value?.takeIf { it.userId == user.id }?.ids ?: return@withLock false
            replace(user, before.with(contentId, favorite))
            try {
                source.set(user, contentId, favorite)
                true
            } catch (cancellation: CancellationException) {
                replace(user, before)
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
                replace(user, before)
                false
            }
        }
    }

    /** Loads [user]'s favorites unless they are already held; `false` if the source failed. */
    private suspend fun ensureLoaded(user: User): Boolean {
        if (cache.value?.userId == user.id) return true
        val loaded = try {
            source.load(user)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            return false
        }
        cache.update { held -> if (held?.userId == user.id) held else Snapshot(user.id, loaded) }
        return true
    }

    /** Swaps in [ids] for [user], unless the cache has since moved on to another User. */
    private fun replace(user: User, ids: Set<ContentId>) {
        cache.update { held -> if (held?.userId == user.id) Snapshot(user.id, ids) else held }
    }

    private fun Set<ContentId>.with(contentId: ContentId, favorite: Boolean): Set<ContentId> =
        if (favorite) this + contentId else this - contentId

    private fun Flow<Snapshot>.filterUser(user: User): Flow<Snapshot> = flow {
        collect { if (it.userId == user.id) emit(it) }
    }
}
