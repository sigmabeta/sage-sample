package net.sigmabeta.sage.sample.server

/**
 * Runtime configuration, sourced from environment variables.
 *
 * The defaults target a local demo: bind all interfaces on port 8080 so a phone on the same LAN can
 * open the app, and keep Users and Sessions in `sage-sample.db` in the working directory. The server
 * holds Users and Sessions (see `docs/adr/0001-server-held-users-and-sessions.md`), so besides the
 * listen address it needs the database file and whether its Session cookie is `Secure`.
 *
 * [secureCookie] defaults off because the local demo is plain HTTP; turn it on behind TLS. A server
 * that binds a non-loopback address with it off sends Session cookies in the clear, which
 * [warnsOfInsecureCookie] flags at startup.
 */
data class ServerConfig(
    val port: Int,
    val bindHost: String,
    val dbPath: String,
    val secureCookie: Boolean,
) {
    /** True when this config would send Session cookies without `Secure` to a non-loopback peer. */
    fun warnsOfInsecureCookie(): Boolean = !secureCookie && !isLoopbackHost(bindHost)

    companion object {
        const val DEFAULT_PORT = 8080
        const val DEFAULT_BIND_HOST = "0.0.0.0"
        const val DEFAULT_DB_PATH = "sage-sample.db"

        fun fromEnv(env: (String) -> String? = System::getenv): ServerConfig = ServerConfig(
            port = env("SAGE_SAMPLE_PORT")?.toIntOrNull() ?: DEFAULT_PORT,
            bindHost = env("SAGE_SAMPLE_BIND_HOST")?.takeIf { it.isNotBlank() } ?: DEFAULT_BIND_HOST,
            dbPath = env("SAGE_SAMPLE_DB_PATH")?.takeIf { it.isNotBlank() } ?: DEFAULT_DB_PATH,
            secureCookie = env("SAGE_SAMPLE_SECURE_COOKIE")?.let(::isTruthy) ?: false,
        )
    }
}

private val TRUTHY = setOf("true", "1", "yes", "on")

private fun isTruthy(raw: String): Boolean = raw.trim().lowercase() in TRUTHY

/**
 * Whether [host] names the loopback interface, judged from the text alone (no DNS lookup): `localhost`,
 * any `127.x.x.x` address, or IPv6 `::1` in its short, bracketed or full form.
 */
internal fun isLoopbackHost(host: String): Boolean {
    val bare = host.trim().removePrefix("[").removeSuffix("]").lowercase()
    return bare == "localhost" ||
        bare.startsWith("127.") ||
        bare == "::1" ||
        bare == "0:0:0:0:0:0:0:1"
}
