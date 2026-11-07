package net.sigmabeta.sage.sample.jvm.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chrome controller follows Chipbox's contract: default chrome shows the top bar, a screen may
 * hide it, and `SampleNavHost` restores [SampleScreenChrome.Default] for the next destination.
 */
class SampleChromeTest {
    @Test
    fun `chrome defaults to showing the top bar`() {
        assertTrue(SampleChromeController().state.showTopBar)
    }

    @Test
    fun `a screen can hide the top bar`() {
        val controller = SampleChromeController()

        controller.set(SampleScreenChrome(showTopBar = false))

        assertFalse(controller.state.showTopBar)
    }

    @Test
    fun `default chrome shows the top bar again`() {
        val controller = SampleChromeController()
        controller.set(SampleScreenChrome(showTopBar = false))

        controller.set(SampleScreenChrome.Default)

        assertTrue(controller.state.showTopBar)
    }
}
