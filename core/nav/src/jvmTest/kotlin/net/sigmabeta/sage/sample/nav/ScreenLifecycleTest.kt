package net.sigmabeta.sage.sample.nav

import androidx.lifecycle.Lifecycle
import net.sigmabeta.sage.appcomm.SageAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The one place a screen lifecycle event becomes a foreground action.
 *
 * The table names every [Lifecycle.Event] explicitly: a new event in a future lifecycle release
 * fails the coverage assertion below instead of silently mapping to nothing.
 */
class ScreenLifecycleTest {
    @Test
    fun `only resume and pause lifecycle events carry foreground actions`() {
        val expected = mapOf(
            Lifecycle.Event.ON_CREATE to null,
            Lifecycle.Event.ON_START to null,
            Lifecycle.Event.ON_RESUME to SageAction.Resume,
            Lifecycle.Event.ON_PAUSE to SageAction.Pause,
            Lifecycle.Event.ON_STOP to null,
            Lifecycle.Event.ON_DESTROY to null,
            Lifecycle.Event.ON_ANY to null,
        )

        assertEquals(
            "the table must name every Lifecycle.Event",
            Lifecycle.Event.entries.toSet(),
            expected.keys,
        )
        expected.forEach { (event, action) ->
            assertEquals(event.name, action, foregroundActionFor(event))
        }
    }
}
