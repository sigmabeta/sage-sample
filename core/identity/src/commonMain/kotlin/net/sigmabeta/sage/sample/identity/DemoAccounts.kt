package net.sigmabeta.sage.sample.identity

/** A published username/password pair the sample seeds as a User. */
data class DemoAccount(
    val username: String,
    val password: String,
)

/**
 * The two Demo accounts: placeholder identity, not real credentials, the way generated bitmaps are
 * placeholder art. Two exist so that separate Users' data can be seen side by side.
 */
object DemoAccounts {
    val DEMO = DemoAccount(username = "demo", password = "sage")
    val DEMO2 = DemoAccount(username = "demo2", password = "sage")

    val all: List<DemoAccount> = listOf(DEMO, DEMO2)

    /**
     * Registers every Demo account [directory] does not have yet. Idempotent: an account that
     * already exists keeps its User id and stored password, so seeding on every start is safe.
     */
    suspend fun seed(directory: UserDirectory) {
        all.forEach { directory.register(it.username, it.password) }
    }
}
