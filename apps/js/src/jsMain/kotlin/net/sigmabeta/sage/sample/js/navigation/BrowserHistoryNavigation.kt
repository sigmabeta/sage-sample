package net.sigmabeta.sage.sample.js.navigation

import kotlinx.browser.window
import net.sigmabeta.sage.sample.nav.SampleRoute
import net.sigmabeta.sage.sample.nav.SampleRouteUrl
import net.sigmabeta.sage.sample.ui.shell.SampleBrowserNavigation
import net.sigmabeta.sage.sample.ui.shell.SampleShellNavigation
import org.w3c.dom.events.Event

/**
 * Mirrors the shell's route into browser history and drives the shell from Back/Forward.
 *
 * Forward navigation (tab tap, push) adds a history entry; an in-app pop replaces the current one.
 * `popstate` decodes the path and calls [SampleShellNavigation.navigateTo]. [applying] suppresses
 * the shell's report while a browser-driven change is in flight, so the two directions cannot ping
 * pong. The history entry carries `{depth}` so [onBackRequested] knows whether an app entry exists
 * behind the current URL; when it does, Back runs `history.back()` and lets `popstate` apply it.
 */
class BrowserHistoryNavigation : SampleBrowserNavigation {
    private var shell: SampleShellNavigation? = null
    private var depth: Int = 0
    private var applying: Boolean = false

    /** False until the initial URL has been decoded and applied; reports before that are noise. */
    private var ready: Boolean = false

    private val apiOverride: String? = window.location.search
        .removePrefix("?")
        .split('&')
        .firstOrNull { it.startsWith("api=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotBlank() }

    init {
        window.addEventListener("popstate", { _: Event -> onPopState() })
    }

    override fun onShellReady(navigation: SampleShellNavigation) {
        shell = navigation
        depth = readDepth()
        val route = decodeCurrentPath() ?: SampleRoute.HOME
        apply(route)
        // Canonicalise the loaded entry: "/" becomes "/home", unknown paths become the home route.
        replaceCurrent(route)
        ready = true
    }

    override fun onRouteChanged(route: SampleRoute, push: Boolean) {
        val navigation = shell ?: return
        if (!ready || applying || navigation.isExternalNavigationPending) return
        val url = SampleRouteUrl.encode(route)
        if (push) {
            if (window.location.pathname == url) return
            depth += 1
            window.history.pushState(depth, "", withApiOverride(url))
        } else {
            replaceCurrent(route)
        }
    }

    override fun onBackRequested(): Boolean {
        if (shell == null || depth <= 0) return false
        window.history.back()
        return true
    }

    override fun onShellDisposed() {
        shell = null
        ready = false
    }

    private fun onPopState() {
        val navigation = shell ?: return
        depth = readDepth()
        val decoded = decodeCurrentPath()
        val route = decoded ?: SampleRoute.HOME
        applying = true
        try {
            navigation.navigateTo(route)
        } finally {
            applying = false
        }
        // A Back onto an unknown path falls back to Home above; rewrite the dead URL.
        if (decoded == null) replaceCurrent(route)
    }

    private fun apply(route: SampleRoute) {
        val navigation = shell ?: return
        applying = true
        try {
            navigation.navigateTo(route)
        } finally {
            applying = false
        }
    }

    private fun decodeCurrentPath(): SampleRoute? =
        SampleRouteUrl.decode(window.location.pathname)

    private fun replaceCurrent(route: SampleRoute) {
        window.history.replaceState(
            depth,
            "",
            withApiOverride(SampleRouteUrl.encode(route)),
        )
    }

    /** Preserves the dev `?api=` override across every URL this bridge writes. */
    private fun withApiOverride(url: String): String = apiOverride?.let { "$url?api=$it" } ?: url

    private fun readDepth(): Int = when (val state = window.history.state) {
        is Int -> state
        is Number -> state.toInt()
        else -> 0
    }
}
