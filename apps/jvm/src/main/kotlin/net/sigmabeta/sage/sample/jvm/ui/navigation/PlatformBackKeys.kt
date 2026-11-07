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
