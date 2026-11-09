package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * The desktop counterpart of Android's activity lifecycle: window focus is the platform's
 * foreground signal.
 *
 * The returned owner is `RESUMED` while the window has focus and `STARTED` (which dispatches
 * `ON_PAUSE`) while it does not, so a still-composed screen receives the same pair as a
 * backgrounded Android screen. The owner is destroyed when the shell leaves composition.
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
