package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The window reopens at the size it was closed with, and only that: the position stays the
 * platform's business.
 */
class PersistedWindowStateTest {
    @Test
    fun `nothing stored opens at the default size and the platform position`() {
        val state = windowStateFor(width = null, height = null)

        assertEquals(DEFAULT_WINDOW_SIZE, state.size)
        assertEquals(WindowPosition.PlatformDefault, state.position)
    }

    @Test
    fun `a stored size is restored`() {
        val state = windowStateFor(width = 1280, height = 720)

        assertEquals(DpSize(1280.dp, 720.dp), state.size)
        assertEquals(WindowPosition.PlatformDefault, state.position)
    }

    @Test
    fun `an unusable stored size falls back to the default`() {
        val table = listOf(
            null to 720,
            1280 to null,
            0 to 720,
            1280 to 0,
            -1280 to -720,
        )

        for ((width, height) in table) {
            assertEquals(
                "width=$width height=$height",
                DEFAULT_WINDOW_SIZE,
                windowStateFor(width, height).size,
            )
        }
    }

    @Test
    fun `only a floating, un-minimized window is persisted`() {
        val table = listOf(
            Triple(WindowPlacement.Floating, false, true),
            Triple(WindowPlacement.Floating, true, false),
            Triple(WindowPlacement.Maximized, false, false),
            Triple(WindowPlacement.Maximized, true, false),
            Triple(WindowPlacement.Fullscreen, false, false),
            Triple(WindowPlacement.Fullscreen, true, false),
        )

        for ((placement, isMinimized, expected) in table) {
            val state = WindowState(
                placement = placement,
                isMinimized = isMinimized,
                size = DpSize(900.dp, 700.dp),
            )

            val size = state.persistableSize()

            if (expected) {
                assertEquals(DpSize(900.dp, 700.dp), size)
            } else {
                assertNull("$placement with isMinimized=$isMinimized must not be persisted", size)
            }
        }
    }

    @Test
    fun `a window whose size is not known yet is not persisted`() {
        val state = WindowState(size = DpSize.Unspecified)

        assertNull(state.persistableSize())
    }
}
