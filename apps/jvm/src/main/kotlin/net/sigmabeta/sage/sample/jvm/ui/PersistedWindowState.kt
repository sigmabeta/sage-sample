package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.storage.common.Storage
import kotlin.math.roundToInt

/**
 * A [WindowState] whose size survives process death: the window opens at the size it was closed
 * with, read from SAGE's [Storage] seam before the platform window is created.
 *
 * Restoring has to happen at construction rather than from an effect: [androidx.compose.ui.window.Window]
 * applies the state while it builds the AWT window, so a later write would show the default size for
 * a frame and then jump. That read is synchronous because `Storage`'s desktop implementation seeds
 * its flows from the properties file in its constructor.
 *
 * The position is deliberately left to the platform: window managers own it, and Wayland clients
 * cannot set one at all.
 *
 * Only a floating window is saved. A maximized or fullscreen window reports the bounds of the
 * screen it covers, and a minimized one reports whatever the platform does with its window, so
 * persisting those would reopen the *next* launch as a floating window sized like the whole
 * display. Saves are debounced, so dragging an edge is one write when the drag settles instead of
 * one per pixel.
 */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberPersistedWindowState(storage: Storage): WindowState {
    val state = remember(storage) { restoredWindowState(storage) }

    LaunchedEffect(state, storage) {
        snapshotFlow { state.persistableSize() }
            .filterNotNull()
            .debounce(SIZE_PERSIST_DEBOUNCE_MS)
            .collect { size ->
                storage.saveInt(KEY_WINDOW_WIDTH, size.width.value.roundToInt())
                storage.saveInt(KEY_WINDOW_HEIGHT, size.height.value.roundToInt())
            }
    }

    return state
}

/**
 * The state a fresh launch starts from. The reads are synchronous for the same reason restoring
 * happens at construction at all; see [rememberPersistedWindowState].
 */
internal fun restoredWindowState(storage: Storage): WindowState = windowStateFor(
    width = runBlocking { storage.savedIntFlow(KEY_WINDOW_WIDTH).first() },
    height = runBlocking { storage.savedIntFlow(KEY_WINDOW_HEIGHT).first() },
)

/**
 * A window of the stored size when both dimensions are usable, and of [DEFAULT_WINDOW_SIZE]
 * otherwise — nothing stored yet, a half-written pair, or a value no window could have.
 */
internal fun windowStateFor(width: Int?, height: Int?): WindowState {
    val hasUsableSize = width != null && height != null && width > 0 && height > 0
    val size = if (hasUsableSize) DpSize(width.dp, height.dp) else DEFAULT_WINDOW_SIZE

    return WindowState(size = size, position = WindowPosition.PlatformDefault)
}

/**
 * The size worth saving right now, or `null` when the window is not showing the size the user
 * chose — see [rememberPersistedWindowState].
 */
internal fun WindowState.persistableSize(): DpSize? {
    val floatingSize = size
    val isFloating = placement == WindowPlacement.Floating && !isMinimized

    return if (isFloating && floatingSize.isSpecified) floatingSize else null
}

// Geometry is not a user-facing preference like the `setting.*` keys, so it gets its own
// namespace in the same properties file.
private const val KEY_WINDOW_WIDTH = "window.width"
private const val KEY_WINDOW_HEIGHT = "window.height"

/** The size the window opens with before anything has been stored. */
internal val DEFAULT_WINDOW_SIZE = DpSize(1000.dp, 800.dp)

private const val SIZE_PERSIST_DEBOUNCE_MS = 500L
