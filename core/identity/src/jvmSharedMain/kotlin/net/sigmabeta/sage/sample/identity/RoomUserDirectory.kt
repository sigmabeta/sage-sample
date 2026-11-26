package net.sigmabeta.sage.sample.identity

import net.sigmabeta.sage.sample.database.SampleDatabase
import net.sigmabeta.sage.sample.database.UserEntity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * [UserDirectory] over the `users` table.
 *
 * Each User gets a random [SALT_BYTES]-byte salt and a PBKDF2-HMAC-SHA256 verifier derived with
 * [iterations] rounds; the row records the algorithm and count, so verification uses whatever
 * produced the stored hash and the default can be raised without touching old rows. Hashes compare
 * in constant time. An unknown username still runs one derivation against a throwaway salt, so the
 * response time does not reveal which usernames exist.
 */
class RoomUserDirectory internal constructor(
    database: SampleDatabase,
    private val iterations: Int,
    private val derive: PasswordKdf,
) : UserDirectory {
    constructor(
        database: SampleDatabase,
        iterations: Int = DEFAULT_PBKDF2_ITERATIONS,
    ) : this(database, iterations, ::pbkdf2HmacSha256)

    private val users = database.userDao()
    private val random = SecureRandom()

    override suspend fun verify(username: String, password: String): User? {
        val row = users.findByUsername(username)
        if (row == null || row.kdfAlgorithm != PBKDF2_ALGORITHM) {
            derive(password.toCharArray(), random.nextBytes(SALT_BYTES), iterations)
            return null
        }
        val candidate = derive(password.toCharArray(), row.salt.fromBase64(), row.kdfIterations)
        return if (MessageDigest.isEqual(candidate, row.passwordHash.fromBase64())) row.toUser() else null
    }

    /**
     * Checks the username first, so a taken name is `null` without a derivation. Two concurrent
     * registrations of one new name both pass that check; the loser's insert aborts on the unique
     * index and throws.
     */
    override suspend fun register(username: String, password: String): User? {
        if (users.findByUsername(username) != null) return null
        val salt = random.nextBytes(SALT_BYTES)
        val row = UserEntity(
            id = UUID.randomUUID().toString(),
            username = username,
            passwordHash = derive(password.toCharArray(), salt, iterations).toBase64(),
            salt = salt.toBase64(),
            kdfAlgorithm = PBKDF2_ALGORITHM,
            kdfIterations = iterations,
        )
        users.insert(row)
        return row.toUser()
    }
}

internal fun UserEntity.toUser(): User = User(id = id, username = username)
