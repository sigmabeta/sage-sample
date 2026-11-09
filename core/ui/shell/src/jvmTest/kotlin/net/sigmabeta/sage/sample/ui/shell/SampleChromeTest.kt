package net.sigmabeta.sage.sample.ui.shell

import net.sigmabeta.sage.sample.nav.SampleScreenChrome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app-local chrome controller contract is: default chrome shows the top bar, a
 * screen may hide it, and `SampleNavHost` restores [SampleScreenChrome.Default] for the next
 * destination. The pure route-key/active-screen policy is tested once in `:core:nav`.
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
