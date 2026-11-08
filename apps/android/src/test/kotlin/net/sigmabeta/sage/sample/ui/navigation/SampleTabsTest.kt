package net.sigmabeta.sage.sample.ui.navigation
import net.sigmabeta.sage.sample.nav.SampleDestination

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Structural proof of per-tab root retention.
 *
 * This repo has no Compose UI test harness, so live retention is proven by construction rather than
 * by driving a `TabNavigator`: every tab is a stable object whose `root` is the same `SampleScreen`
 * instance on every visit, each tab has its own stable `key`, and the tab list is the app's
 * top-level destinations in display order. Voyager's `TabNavigator` keeps each tab's inner
 * `Navigator` and its per-screen `ViewModelStore`/saveable state when the tab leaves composition,
 * so a stable screen key is what makes the Browse and Search view models and scroll state survive a
 * switch. Dropping the tab objects and pushing the roots onto one navigator would break that, and
 * these assertions are the guard.
 */
class SampleTabsTest {
    @Test
    fun `tabs are the top-level destinations in display order`() {
        assertEquals(SampleDestination.topLevel, sampleTabs.map { it.destination })
    }

    @Test
    fun `every tab maps to its fixed root screen`() {
        assertEquals(
            mapOf(
                SampleDestination.HOME to HomeScreen,
                SampleDestination.BROWSE to BrowseScreen,
                SampleDestination.SEARCH to SearchScreen,
            ),
            sampleTabs.associate { it.destination to it.root },
        )
    }

    @Test
    fun `tabs and their roots are distinct stable instances`() {
        assertEquals(sampleTabs.size, sampleTabs.map { it.key }.toSet().size)
        assertEquals(sampleTabs.size, sampleTabs.map { it.root.key }.toSet().size)

        assertSame(HomeTab, tabFor(SampleDestination.HOME))
        assertSame(BrowseTab, tabFor(SampleDestination.BROWSE))
        assertSame(SearchTab, tabFor(SampleDestination.SEARCH))
        assertSame(HomeScreen, HomeTab.root)
        assertSame(BrowseScreen, BrowseTab.root)
        assertSame(SearchScreen, SearchTab.root)
    }

    @Test
    fun `pushed destinations do not own a tab`() {
        SampleDestination.entries.filterNot { it.isTopLevel }.forEach { destination ->
            assertNull("$destination must be pushed onto the active tab", tabFor(destination))
        }
    }

    @Test
    fun `every destination maps to a screen that keeps it`() {
        SampleDestination.entries.forEach { destination ->
            assertEquals(destination, screenFor(destination).destination)
        }
    }
}
