package net.sigmabeta.sage.sample.jvm.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    @Test
    fun `the route key is the active tab plus the top screen of its stack`() {
        val cases = listOf(
            Triple("home-tab", "home", "home-tab/home"),
            Triple("browse-tab", "home", "browse-tab/home"),
            Triple("home-tab", "settings", "home-tab/settings"),
        )

        cases.forEach { (tabKey, screenKey, expected) ->
            assertEquals(expected, routeKeyFor(tabKey, screenKey))
        }
    }

    @Test
    fun `a screen registered by the outgoing tab is ignored during a tab switch`() {
        // Home's navigator can still be registered for the frame in which Browse mounts, so the
        // chrome and route key must fall back to Browse's own root, not Home's pushed Settings.
        val outgoingSettings = SettingsScreen

        assertSame(
            BrowseScreen,
            activeScreenFor(BrowseTab, registeredTabKey = HomeTab.key, registeredScreen = outgoingSettings),
        )
        assertEquals(
            routeKeyFor(BrowseTab.key, BrowseScreen.key),
            routeKeyFor(
                BrowseTab.key,
                activeScreenFor(BrowseTab, HomeTab.key, outgoingSettings).key,
            ),
        )

        // A registration that belongs to the current tab, including a pushed screen, is used.
        assertSame(
            outgoingSettings,
            activeScreenFor(BrowseTab, registeredTabKey = BrowseTab.key, registeredScreen = outgoingSettings),
        )
    }
}
