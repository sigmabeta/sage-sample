package net.sigmabeta.sage.sample.ui.shell
import net.sigmabeta.sage.sample.nav.SampleDestination

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    /**
     * A switch marks the tab it leaves rather than popping it, so the tab's exit animation can
     * still show the screen the user was on. The tab's navigator saver restores the marked tab at
     * its root, and the mark is cleared once that composition succeeds.
     */
    @Test
    fun `a tab left by a switch is marked to reopen at its root until it is composed`() {
        val activeTabNavigator = SampleActiveTabNavigator()

        activeTabNavigator.resetToRootOnNextEntry(BrowseTab.key)

        assertTrue(activeTabNavigator.rootResetPending(BrowseTab.key))
        assertFalse(activeTabNavigator.rootResetPending(HomeTab.key))

        activeTabNavigator.clearRootReset(BrowseTab.key)

        assertFalse(activeTabNavigator.rootResetPending(BrowseTab.key))
    }

    /** A rotation or process restore keeps the pending reset, so the tab still reopens at root. */
    @Test
    fun `a pending reset survives a saved-state round trip`() {
        val activeTabNavigator = SampleActiveTabNavigator()
        activeTabNavigator.resetToRootOnNextEntry(BrowseTab.key)

        val saved = with(SampleActiveTabNavigator.Saver) {
            SaverScope { true }.save(activeTabNavigator)
        }
        val restored = SampleActiveTabNavigator.Saver.restore(checkNotNull(saved))

        assertTrue(checkNotNull(restored).rootResetPending(BrowseTab.key))
        assertFalse(checkNotNull(restored).rootResetPending(HomeTab.key))
    }
}
