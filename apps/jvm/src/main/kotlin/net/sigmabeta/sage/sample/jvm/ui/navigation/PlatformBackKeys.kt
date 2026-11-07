package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.Flow

/**
 * A stream of "the user pressed a Back key" signals from the desktop window.
 *
 * `Main` builds it from `Window(onKeyEvent = ...)`, `SampleApp` provides it, and the shell collects
 * it and calls the same Back handler as the top-bar arrow. Window-level handling is what makes
 * Escape/Backspace work whenever the window is focused, not only after a focusable child has been
 * clicked; `onKeyEvent` is the bubble phase, so a focused text field keeps Backspace for editing
 * and only unconsumed presses become signals. `null` (the default) means no window is wired, as in
 * previews and tests.
 */
val LocalPlatformBackKeys = staticCompositionLocalOf<Flow<Unit>?> { null }

/**
 * Whether [key] means Back on desktop.
 *
 * Kept pure and separate from the window handler so the mapping is unit-tested without an AWT
 * event. Escape and Backspace are the whole set: Enter, Delete, Tab, arrows and shortcuts keep
 * their normal meaning.
 */
fun isBackKey(key: Key): Boolean = key == Key.Escape || key == Key.Backspace

/**
 * Tracks the Back keys currently held down for one desktop window.
 *
 * Key auto-repeat delivers KeyDown without an intervening KeyUp, so a seen key is ignored until its
 * KeyUp arrives. The window can lose focus before that KeyUp (alt-tab, another window), leaving the
 * key marked held forever; [onFocusChanged] clears the set so the first press after refocus fires.
 */
class HeldBackKeys {
    private val held = mutableSetOf<Key>()

    /** Records a KeyDown; true when this is a new press rather than auto-repeat. */
    fun press(key: Key): Boolean = held.add(key)

    /** Records a KeyUp. */
    fun release(key: Key) {
        held.remove(key)
    }

    /** Forgets every held key while the window is unfocused. */
    fun onFocusChanged(isFocused: Boolean) {
        if (!isFocused) held.clear()
    }
}
