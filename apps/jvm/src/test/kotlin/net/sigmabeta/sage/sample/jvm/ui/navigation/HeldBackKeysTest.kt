package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window emits one Back signal per physical press. Auto-repeat is filtered by tracking held
 * keys, and losing focus must forget them: the KeyUp can arrive while another window has focus, so
 * without the reset the first press after refocus would be swallowed.
 */
class HeldBackKeysTest {
    @Test
    fun `auto-repeat is swallowed until the key is released`() {
        val held = HeldBackKeys()

        assertTrue(held.press(Key.Escape))
        assertFalse(held.press(Key.Escape))

        held.release(Key.Escape)

        assertTrue(held.press(Key.Escape))
    }

    @Test
    fun `losing focus forgets every held key`() {
        val held = HeldBackKeys()
        assertTrue(held.press(Key.Escape))
        assertTrue(held.press(Key.Backspace))

        held.onFocusChanged(isFocused = false)

        assertTrue("Escape must fire on the first press after refocus", held.press(Key.Escape))
        assertTrue("Backspace must fire on the first press after refocus", held.press(Key.Backspace))
    }

    @Test
    fun `gaining focus does not clear the held-key state`() {
        val held = HeldBackKeys()
        assertTrue(held.press(Key.Escape))

        held.onFocusChanged(isFocused = true)

        assertFalse("the key is still physically held", held.press(Key.Escape))
    }
}
