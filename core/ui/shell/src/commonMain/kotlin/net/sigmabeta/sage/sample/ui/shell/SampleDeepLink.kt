package net.sigmabeta.sage.sample.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.sample.nav.SampleRoute

/**
 * A native route waiting to open the app, supplied by an app entry point.
 *
 * Android offers the intent's URI from `onCreate`/`onNewIntent`; desktop offers its launch argument
 * before the window opens. Unlike [SampleBrowserNavigation] this seam has no reverse direction: the
 * browser owns its URL, but an intent or a launch argument is a one-shot request, so the shell
 * simply applies it and calls [consume].
 */
interface SampleDeepLinkSource {
    /** The route to apply, or null when there is nothing to do. */
    val pending: StateFlow<SampleRoute?>

    /**
     * Called by the shell after [pending] has been applied. A recreated host (rotation, a reopened
     * window) must not replay a link the user already followed.
     */
    fun consume()
}

/**
 * [SampleDeepLinkSource] whose route is set by the host and cleared by the shell.
 *
 * Android keeps one in `SampleApplication` so a link survives activity recreation until the shell
 * consumes it; desktop creates one from the process arguments. [offer] ignores null because a plain
 * launcher intent (`intent.data == null`) must not clear an unconsumed link.
 */
class MutableSampleDeepLinkSource(initial: SampleRoute? = null) : SampleDeepLinkSource {
    private val mutablePending = MutableStateFlow(initial)

    override val pending: StateFlow<SampleRoute?> = mutablePending.asStateFlow()

    /** Offers [route]; a null route (an ordinary launch) leaves any pending link alone. */
    fun offer(route: SampleRoute?) {
        if (route != null) mutablePending.value = route
    }

    override fun consume() {
        mutablePending.value = null
    }
}

/** Provided by `SampleApp`; null on hosts without native deep links. */
val LocalSampleDeepLinkSource = staticCompositionLocalOf<SampleDeepLinkSource?> { null }
