package net.sigmabeta.sage.sample.server

/**
 * Runtime configuration, sourced from environment variables.
 *
 * The defaults target a local demo: bind all interfaces on port 8080 so a phone on the same LAN can
 * open the app. The server owns no data and needs no credentials, so there is nothing to configure
 * beyond the listen address.
 */
data class ServerConfig(
    val port: Int,
    val bindHost: String,
) {
    companion object {
        const val DEFAULT_PORT = 8080
        const val DEFAULT_BIND_HOST = "0.0.0.0"

        fun fromEnv(env: (String) -> String? = System::getenv): ServerConfig = ServerConfig(
            port = env("SAGE_SAMPLE_PORT")?.toIntOrNull() ?: DEFAULT_PORT,
            bindHost = env("SAGE_SAMPLE_BIND_HOST")?.takeIf { it.isNotBlank() } ?: DEFAULT_BIND_HOST,
        )
    }
}
