package net.sigmabeta.sage.sample.server

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerConfigTest {
    @Test
    fun `an empty environment gives the local demo defaults`() {
        val config = ServerConfig.fromEnv { null }
        assertEquals(
            ServerConfig(port = 8080, bindHost = "0.0.0.0", dbPath = "sage-sample.db", secureCookie = false),
            config,
        )
    }

    @Test
    fun `every variable is read from the environment`() {
        val env = mapOf(
            "SAGE_SAMPLE_PORT" to "9000",
            "SAGE_SAMPLE_BIND_HOST" to "127.0.0.1",
            "SAGE_SAMPLE_DB_PATH" to "/var/lib/sage/sample.db",
            "SAGE_SAMPLE_SECURE_COOKIE" to "true",
        )
        assertEquals(
            ServerConfig(port = 9000, bindHost = "127.0.0.1", dbPath = "/var/lib/sage/sample.db", secureCookie = true),
            ServerConfig.fromEnv(env::get),
        )
    }

    @Test
    fun `a blank db path falls back to the default`() {
        assertEquals("sage-sample.db", ServerConfig.fromEnv { if (it == "SAGE_SAMPLE_DB_PATH") " " else null }.dbPath)
    }

    @Test
    fun `the secure cookie flag accepts the usual truthy spellings and nothing else`() {
        val cases = mapOf(
            "true" to true,
            "TRUE" to true,
            "1" to true,
            "yes" to true,
            "on" to true,
            " true " to true,
            "false" to false,
            "0" to false,
            "no" to false,
            "off" to false,
            "" to false,
            "enabled" to false,
        )
        for ((raw, expected) in cases) {
            val config = ServerConfig.fromEnv { if (it == "SAGE_SAMPLE_SECURE_COOKIE") raw else null }
            assertEquals("SAGE_SAMPLE_SECURE_COOKIE=\"$raw\"", expected, config.secureCookie)
        }
    }

    @Test
    fun `only a non-loopback bind with Secure off warns`() {
        val cases = listOf(
            "0.0.0.0" to true,
            "::" to true,
            "192.168.1.5" to true,
            "example.lan" to true,
            "127.0.0.1" to false,
            "127.4.5.6" to false,
            "localhost" to false,
            "LOCALHOST" to false,
            "::1" to false,
            "[::1]" to false,
            "0:0:0:0:0:0:0:1" to false,
        )
        for ((host, warns) in cases) {
            assertEquals("insecure, $host", warns, config(host, secure = false).warnsOfInsecureCookie())
            assertEquals("secure, $host", false, config(host, secure = true).warnsOfInsecureCookie())
        }
    }

    private fun config(host: String, secure: Boolean) =
        ServerConfig(port = 8080, bindHost = host, dbPath = "x.db", secureCookie = secure)
}
