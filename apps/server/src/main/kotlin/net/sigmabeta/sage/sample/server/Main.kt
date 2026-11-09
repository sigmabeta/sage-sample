package net.sigmabeta.sage.sample.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Entry point: read the listen address, then start Ktor/Netty with the shared route stack.
 *
 * The server is stateless — every response comes from the deterministic generators in
 * `:core:repository:demo` — so there is no startup scan, database, or background work.
 */
fun main() {
    val config = ServerConfig.fromEnv()
    embeddedServer(
        Netty,
        port = config.port,
        host = config.bindHost,
    ) {
        configureSampleServer()
    }.start(wait = true)
}
