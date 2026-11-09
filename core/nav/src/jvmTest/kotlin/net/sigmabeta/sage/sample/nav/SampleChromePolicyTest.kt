package net.sigmabeta.sage.sample.nav

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pure chrome/route-key policy. The Compose controller stays in each app; this covers the
 * decisions the shell makes from a tab key and the registered screen.
 */
class SampleChromePolicyTest {
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
        val browseRoot = "browse-root"
        val outgoingSettings = "settings"

        assertEquals(
            browseRoot,
            activeScreenFor(
                currentTabKey = "browse-tab",
                currentTabRoot = browseRoot,
                registeredTabKey = "home-tab",
                registeredScreen = outgoingSettings,
            ),
        )
        assertEquals(
            "browse-tab/browse-root",
            routeKeyFor(
                "browse-tab",
                activeScreenFor("browse-tab", browseRoot, "home-tab", outgoingSettings),
            ),
        )

        // A registration that belongs to the current tab, including a pushed screen, is used.
        assertEquals(
            outgoingSettings,
            activeScreenFor("browse-tab", browseRoot, "browse-tab", outgoingSettings),
        )
        assertEquals(
            browseRoot,
            activeScreenFor("browse-tab", browseRoot, "browse-tab", null),
        )
    }
}
