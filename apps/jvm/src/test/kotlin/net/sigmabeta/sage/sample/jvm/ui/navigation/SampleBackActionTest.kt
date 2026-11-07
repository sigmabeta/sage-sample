package net.sigmabeta.sage.sample.jvm.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Back is one policy for every affordance: the top-bar arrow, the desktop Back inputs, and a
 * feature's `SageEvent.NavigateBack` all resolve through [backActionFor].
 *
 * The table covers every top-level tab at both `canPop` values. A new top-level destination fails
 * the completeness assertion until it gets a policy row, which is the point: a tab silently
 * inheriting Back behaviour is how the shell drifted apart before.
 */
class SampleBackActionTest {
    private val expected: Map<Pair<SampleDestination, Boolean>, SampleBackAction> = mapOf(
        (SampleDestination.HOME to false) to SampleBackAction.Outer,
        (SampleDestination.HOME to true) to SampleBackAction.Pop,
        (SampleDestination.BROWSE to false) to SampleBackAction.Outer,
        (SampleDestination.BROWSE to true) to SampleBackAction.Pop,
        (SampleDestination.SEARCH to false) to SampleBackAction.ShowTab(SampleDestination.HOME),
        (SampleDestination.SEARCH to true) to SampleBackAction.Pop,
    )

    @Test
    fun `back policy table covers every tab at both canPop values`() {
        assertEquals(
            SampleDestination.topLevel.toSet(),
            expected.keys.map { (tab, _) -> tab }.toSet(),
        )

        expected.forEach { (input, action) ->
            val (tab, canPop) = input
            assertEquals("back for $tab (canPop=$canPop)", action, backActionFor(tab, canPop))
        }
    }

    @Test
    fun `a pushed screen pops inside the active tab`() {
        SampleDestination.topLevel.forEach { tab ->
            assertEquals(
                "a pushed screen under $tab must pop, not leave the tab",
                SampleBackAction.Pop,
                backActionFor(tab, canPop = true),
            )
        }
    }

    @Test
    fun `search root returns home`() {
        assertEquals(
            SampleBackAction.ShowTab(SampleDestination.HOME),
            backActionFor(SampleDestination.SEARCH, canPop = false),
        )
    }

    @Test
    fun `home and browse roots fall through to the outer behaviour`() {
        assertEquals(
            SampleBackAction.Outer,
            backActionFor(SampleDestination.HOME, canPop = false),
        )
        assertEquals(
            SampleBackAction.Outer,
            backActionFor(SampleDestination.BROWSE, canPop = false),
        )
    }
}
