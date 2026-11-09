package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.sample.nav.SampleRoute

/**
 * The host's side of browser history, supplied by `:apps:js` and absent on Android/desktop.
 *
 * The shell reports the active route after every navigation and asks the host whether Back should
 * be handled by the browser (an app entry exists behind the current one). The host drives the
 * shell through [SampleShellNavigation] for deep links and `popstate`.
 */
interface SampleBrowserNavigation {
    /** Called once the shell's navigators exist; the host applies the current URL here. */
    fun onShellReady(navigation: SampleShellNavigation)

    /** Reports the shell's active route. `push` is false when the change was an in-app pop. */
    fun onRouteChanged(route: SampleRoute, push: Boolean)

    /**
     * Called before the shell performs its own Back policy. Returns true when the host has an app
     * entry behind the current one and has started navigating back through browser history.
     */
    fun onBackRequested(): Boolean

    /** The shell left composition; the host must stop driving it. */
    fun onShellDisposed()
}

/** The shell's navigation entry point for the browser host. */
interface SampleShellNavigation {
    /** Navigates to [route], switching tabs and rebuilding that tab's stack as needed. */
    fun navigateTo(route: SampleRoute)

    /**
     * True while a browser-driven route waits for its tab's inner navigator to register. The host
     * ignores route reports during that window so a stale pre-application route cannot push a
     * duplicate history entry.
     */
    val isBrowserNavigationPending: Boolean
}

/** Provided by `SampleApp`; null on hosts without a URL bar. */
val LocalSampleBrowserNavigation = staticCompositionLocalOf<SampleBrowserNavigation?> { null }
