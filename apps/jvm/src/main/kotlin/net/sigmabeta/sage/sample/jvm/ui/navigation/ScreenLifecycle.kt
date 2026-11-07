package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LifecycleResumeEffect
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction

/**
 * The [LifecycleOwner] screen entries observe for foreground/background, supplied by the app shell.
 *
 * Defaults to `null` so a screen rendered outside the shell — an isolated screen test, a
 * `@Preview` — receives no lifecycle actions instead of crashing on a missing owner.
 */
val LocalScreenLifecycleOwner = staticCompositionLocalOf<LifecycleOwner?> { null }

/**
 * The single lifecycle-event → foreground-action decision.
 *
 * Only `ON_RESUME` and `ON_PAUSE` are foreground transitions; every other event ([Lifecycle.Event])
 * is not, and returns `null`.
 */
internal fun foregroundActionFor(event: Lifecycle.Event): SageAction? = when (event) {
    Lifecycle.Event.ON_RESUME -> SageAction.Resume
    Lifecycle.Event.ON_PAUSE -> SageAction.Pause
    else -> null
}

/**
 * The desktop counterpart of Android's activity lifecycle: window focus is the platform's
 * foreground signal.
 *
 * The returned owner is `RESUMED` while the window has focus and `STARTED` (which dispatches
 * `ON_PAUSE`) while it does not, so a still-composed screen receives the same pair as a backgrounded
 * Android screen. The owner is destroyed when the shell leaves composition.
 */
@Composable
fun rememberWindowFocusLifecycleOwner(): LifecycleOwner {
    val windowInfo = LocalWindowInfo.current
    val owner = remember { WindowFocusLifecycleOwner() }
    val focused = windowInfo.isWindowFocused

    SideEffect { owner.setFocused(focused) }

    DisposableEffect(owner) {
        onDispose { owner.destroy() }
    }

    return owner
}

/**
 * A [LifecycleOwner] whose only inputs are window focus and disposal. It is a private detail of
 * [rememberWindowFocusLifecycleOwner]; screens only ever see it as a [LifecycleOwner].
 */
private class WindowFocusLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle = registry

    fun setFocused(focused: Boolean) {
        registry.currentState = if (focused) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/**
 * Feeds the screen's foreground lifecycle into [actionSink] (the screen's view model) as plain
 * actions:
 *
 * - `ON_RESUME` — including window focus returning, and entering composition while already
 *   resumed — sends [SageAction.Resume].
 * - `ON_PAUSE` and disposal send [SageAction.Pause].
 *
 * Only the active (composed) screen runs this effect, so only it receives the pair. Repeated
 * notifications are coalesced in `SampleListViewModel.sendAction`, which stays the single dedupe
 * point (SAGE's list screens also send `Resume` on title changes).
 *
 * No-ops when the shell provided no [LocalScreenLifecycleOwner].
 */
@Composable
fun ScreenLifecycleEffect(actionSink: ActionSink) {
    val owner = LocalScreenLifecycleOwner.current ?: return

    LifecycleResumeEffect(owner, lifecycleOwner = owner) {
        foregroundActionFor(Lifecycle.Event.ON_RESUME)?.let(actionSink::sendAction)
        onPauseOrDispose {
            foregroundActionFor(Lifecycle.Event.ON_PAUSE)?.let(actionSink::sendAction)
        }
    }
}
