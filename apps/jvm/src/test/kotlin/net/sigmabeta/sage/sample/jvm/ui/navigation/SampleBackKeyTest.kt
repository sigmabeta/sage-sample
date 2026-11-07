package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The desktop window forwards only the keys in this table to the shared Back policy.
 *
 * The table is deliberately literal: Escape and Backspace are Back, and the non-back rows are the
 * keys most likely to be caught by an over-broad mapping (text editing, focus traversal, and
 * shortcuts that must keep their normal meaning).
 */
class SampleBackKeyTest {
    private val expected: Map<Key, Boolean> = mapOf(
        Key.Escape to true,
        Key.Backspace to true,
        Key.Enter to false,
        Key.Tab to false,
        Key.Spacebar to false,
        Key.Delete to false,
        Key.DirectionLeft to false,
        Key.DirectionRight to false,
        Key.DirectionUp to false,
        Key.DirectionDown to false,
        Key.A to false,
        Key.F1 to false,
    )

    @Test
    fun `back key table maps Escape and Backspace and nothing else`() {
        expected.forEach { (key, isBack) ->
            assertEquals("isBackKey($key)", isBack, isBackKey(key))
        }
    }
}
