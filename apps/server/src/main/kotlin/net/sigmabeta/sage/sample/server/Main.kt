package net.sigmabeta.sage.sample.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.sample.database.SampleDatabases
import org.slf4j.LoggerFactory

/**
 * Entry point: read the config, open the Users and Sessions database, seed the Demo accounts, then
 * start Ktor/Netty with the shared route stack.
 *
 * Content still comes from the deterministic generators in `:core:repository:demo`; the state the
 * server holds is its Users and Sessions, in the Room database at [ServerConfig.dbPath].
 */
fun main() {
    val config = ServerConfig.fromEnv()
    val log = LoggerFactory.getLogger("net.sigmabeta.sage.sample.server")
    if (config.warnsOfInsecureCookie()) {
        log.warn(
            "Binding {} with SAGE_SAMPLE_SECURE_COOKIE off: Session cookies will travel without " +
                "Secure. Serve over TLS and set SAGE_SAMPLE_SECURE_COOKIE=true outside a trusted network.",
            config.bindHost,
        )
    }
    val database = SampleDatabases.file(config.dbPath)
    val identity = runBlocking { ServerIdentity.overRoom(database) }
    log.info("Users and Sessions in {}", config.dbPath)
    embeddedServer(
        Netty,
        port = config.port,
        host = config.bindHost,
    ) {
        configureSampleServer(identity, secureCookie = config.secureCookie, favorites = RoomFavoritesStore(database))
    }.start(wait = true)
}
