package net.sigmabeta.sage.sample.ui.navigation

import net.sigmabeta.sage.components.TitleBarModel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The top bar is derived from the active screen's SAGE title state, with the destination as the
 * pre-report fallback.
 *
 * These are the two facts the chrome needs: which text to show and whether the leading affordance
 * is the hamburger ([SampleTopBarLeading.MENU]) or the back arrow
 * ([SampleTopBarLeading.BACK]).
 */
class SampleTopBarStateTest {
    @Test
    fun `falls back to the destination label and back arrow before the screen reports`() {
        assertEquals(
            SampleTopBarState(title = "Browse", leading = SampleTopBarLeading.BACK),
            topBarState(titleBar = null, destinationLabel = "Browse", isTopLevel = false),
        )
    }

    @Test
    fun `falls back to the hamburger on a top-level destination`() {
        assertEquals(
            SampleTopBarState(title = "Home", leading = SampleTopBarLeading.MENU),
            topBarState(titleBar = null, destinationLabel = "Home", isTopLevel = true),
        )
    }

    @Test
    fun `a reported title replaces the destination label`() {
        val titleBar = TitleBarModel(title = "Welcome to SAGE", shouldShowBack = false)

        assertEquals(
            SampleTopBarState(title = "Welcome to SAGE", leading = SampleTopBarLeading.MENU),
            topBarState(titleBar, destinationLabel = "Home", isTopLevel = true),
        )
    }

    @Test
    fun `shouldShowBack true shows the back arrow even on a top-level destination`() {
        val titleBar = TitleBarModel(title = "Details", shouldShowBack = true)

        assertEquals(
            SampleTopBarState(title = "Details", leading = SampleTopBarLeading.BACK),
            topBarState(titleBar, destinationLabel = "Browse", isTopLevel = true),
        )
    }

    @Test
    fun `shouldShowBack false shows the hamburger even below the top level`() {
        val titleBar = TitleBarModel(title = "Settings", shouldShowBack = false)

        assertEquals(
            SampleTopBarState(title = "Settings", leading = SampleTopBarLeading.MENU),
            topBarState(titleBar, destinationLabel = "Settings", isTopLevel = false),
        )
    }

    @Test
    fun `a reported title bar without a title falls back to the destination label`() {
        val titleBar = TitleBarModel(title = null, shouldShowBack = false)

        assertEquals(
            SampleTopBarState(title = "Search", leading = SampleTopBarLeading.MENU),
            topBarState(titleBar, destinationLabel = "Search", isTopLevel = false),
        )
    }
}
