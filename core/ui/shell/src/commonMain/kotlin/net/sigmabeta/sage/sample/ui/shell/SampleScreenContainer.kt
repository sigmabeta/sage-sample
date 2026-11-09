package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen

/**
 * Wraps each Voyager screen's content.
 *
 * Android's Voyager supplies a per-screen `ViewModelStoreOwner` itself, so the passthrough
 * implementation is all it needs. Desktop Voyager does not, so that app supplies a container that
 * hangs a store off a `ScreenModel`; see `WithPerScreenViewModelStore` in `:apps:jvm`.
 */
interface SampleScreenContainer {
    @Composable
    fun Content(screen: Screen, content: @Composable () -> Unit)
}

/** The default: the screen renders directly. */
object PassthroughSampleScreenContainer : SampleScreenContainer {
    @Composable
    override fun Content(screen: Screen, content: @Composable () -> Unit) = content()
}

/** Provided by `SampleApp`; the tab host wraps every pushed screen with it. */
val LocalSampleScreenContainer = staticCompositionLocalOf<SampleScreenContainer> {
    PassthroughSampleScreenContainer
}
