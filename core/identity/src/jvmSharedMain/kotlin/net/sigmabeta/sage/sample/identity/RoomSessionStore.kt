package net.sigmabeta.sage.sample.identity

import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.database.SessionEntity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [SessionStore] over the `sessions` table.
 *
 * A token is [TOKEN_BYTES] bytes from [SecureRandom], handed to the client as unpadded URL-safe
 * Base64. The row is keyed by the SHA-256 of the token's UTF-8 bytes (standard Base64), so the
 * database alone cannot produce a live Session. Expiry is checked against [clock] on every lookup,
 * and an expired row is deleted when it is found.
 */
class RoomSessionStore(
    database: SampleDatabase,
    private val clock: Clock = Clock.System,
) : SessionStore {
    private val sessions = database.sessionDao()
    private val users = database.userDao()
    private val random = SecureRandom()

    override suspend fun create(user: User): IssuedSession {
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(random.nextBytes(TOKEN_BYTES))
        val createdAt = clock.now().toEpochMilliseconds()
        val row = SessionEntity(
            tokenHash = hashOf(token),
            userId = user.id,
            createdAt = createdAt,
            expiresAt = createdAt + SESSION_LIFETIME.inWholeMilliseconds,
        )
        sessions.insert(row)
        return IssuedSession(token = token, session = row.toSession(user))
    }

    override suspend fun lookup(token: String): Session? {
        val tokenHash = hashOf(token)
        val row = sessions.findByTokenHash(tokenHash) ?: return null
        if (clock.now().toEpochMilliseconds() >= row.expiresAt) {
            sessions.deleteByTokenHash(tokenHash)
            return null
        }
        // The foreign key cascades a deleted User's Sessions, so a found row always has its User.
        val user = users.findById(row.userId)?.toUser() ?: return null
        return row.toSession(user)
    }

    override suspend fun revoke(token: String) {
        sessions.deleteByTokenHash(hashOf(token))
    }

    private fun hashOf(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.encodeToByteArray()).toBase64()

    private fun SessionEntity.toSession(user: User) = Session(
        user = user,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        expiresAt = Instant.fromEpochMilliseconds(expiresAt),
    )

    private companion object {
        /** 256 bits. */
        const val TOKEN_BYTES = 32
    }
}
