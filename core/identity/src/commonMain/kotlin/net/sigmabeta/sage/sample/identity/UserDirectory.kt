package net.sigmabeta.sage.sample.identity

/**
 * A User: the durable identity user-scoped data keys on. [id] is stable for the User's lifetime
 * and the same on every device they sign in from; [username] is what they sign in with.
 */
data class User(
    val id: String,
    val username: String,
)

/**
 * The Users the sample knows and the check of a username + password against them.
 *
 * Usernames match exactly: case-sensitive, nothing trimmed, and passwords likewise. Rejection
 * never says whether the username or the password was wrong.
 */
interface UserDirectory {
    /** The User [username] names if [password] is theirs; `null` for any rejection. */
    suspend fun verify(username: String, password: String): User?

    /** Adds a User with a fresh stable id; `null` if [username] is already taken. */
    suspend fun register(username: String, password: String): User?
}
