package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.nav.SampleScreenChrome

/**
 * Mutable chrome state shared by the active screen and the shell.
 *
 * A screen hides the top bar by setting this in a `LaunchedEffect`; the next destination resets it
 * to [SampleScreenChrome.Default], so the screen does not need to restore it on disposal.
 *
 * The value type and the pure derivation live in `:core:nav`; only the Compose state holder stays
 * with the app.
 */
@Stable
class SampleChromeController {
    var state: SampleScreenChrome by mutableStateOf(SampleScreenChrome.Default)
        private set

    fun set(chrome: SampleScreenChrome) {
        state = chrome
    }
}

/** Provided by `SampleNavHost` around every screen; there is no sensible default. */
val LocalSampleChromeController = staticCompositionLocalOf<SampleChromeController> {
    error("No SampleChromeController provided — render screens inside SampleNavHost.")
}

/**
 * The active screen's report of its SAGE title state.
 *
 * The screen calls this whenever [net.sigmabeta.sage.list.ListStateActual.title] changes;
 * `SampleNavHost` holds the value and derives the shared top bar from it.
 */
fun interface SampleTitleBarSink {
    fun onTitleBar(titleBar: TitleBarModel)
}

/** Provided by `SampleNavHost` around the current screen; there is no sensible default. */
val LocalSampleTitleBarSink = staticCompositionLocalOf<SampleTitleBarSink> {
    error("No SampleTitleBarSink provided — render screens inside SampleNavHost.")
}

/** The current window's SAGE [WidthClass]; provided once by `SampleNavHost`. */
val LocalSampleWidthClass = staticCompositionLocalOf<WidthClass> {
    error("No WidthClass provided — render screens inside SampleNavHost.")
}
