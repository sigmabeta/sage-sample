package net.sigmabeta.sage.sample.jvm.ui.navigation

import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Turning a feature event into a back-stack operation is app policy: the feature only names a
 * route. Top-level routes select their tab (clearing the outgoing tab's stack); every other route
 * pushes onto the active tab, exactly like the hamburger's Settings push.
 */
class SampleNavActionTest {
    @Test
    fun `every top-level route selects its tab`() {
        SampleDestination.topLevel.forEach { destination ->
            val action = navActionFor(SageEvent.NavigateTo(destination.destName, source = "test"))

            assertEquals(SampleNavAction.ShowTopLevel(destination), action)
            assertSame(
                "a top-level route must land on a tab, not a pushed screen",
                tabFor(destination),
                tabFor((action as SampleNavAction.ShowTopLevel).destination),
            )
        }
    }

    @Test
    fun `every pushed route belongs to the active tab and has no tab of its own`() {
        SampleDestination.entries.filterNot { it.isTopLevel }.forEach { destination ->
            assertNull("$destination must not be a tab", tabFor(destination))
            assertEquals(
                SampleNavAction.Push(destination),
                navActionFor(SageEvent.NavigateTo(destination.destName, source = "test")),
            )
            assertEquals(destination, screenFor(destination).destination)
        }
    }

    @Test
    fun `a non-top-level route is pushed onto the stack`() {
        assertEquals(
            SampleNavAction.Push(SampleDestination.SETTINGS),
            navActionFor(SageEvent.NavigateTo(destination = "settings", source = "test")),
        )
        assertEquals(
            SampleNavAction.Push(SampleDestination.LIST_ITEMS),
            navActionFor(SageEvent.NavigateTo(destination = "list-items", source = "test")),
        )
        assertEquals(
            SampleNavAction.Push(SampleDestination.LIST_ITEMS_WITH_IMAGES),
            navActionFor(
                SageEvent.NavigateTo(destination = "list-items-with-images", source = "test"),
            ),
        )
        assertEquals(
            SampleNavAction.Push(SampleDestination.GRID_OF_IMAGES),
            navActionFor(
                SageEvent.NavigateTo(destination = "grid-of-images", source = "test"),
            ),
        )
        assertEquals(
            SampleNavAction.Push(SampleDestination.CONTENT_DETAIL),
            navActionFor(
                SageEvent.NavigateTo(destination = "content-detail", source = "test"),
            ),
        )
    }

    @Test
    fun `a content detail route carries its typed id`() {
        assertEquals(
            SampleNavAction.Push(
                SampleDestination.CONTENT_DETAIL,
                ContentId("list-items:7"),
            ),
            navActionFor(
                SageEvent.NavigateTo(
                    destination = "content-detail/list-items:7",
                    source = "test",
                ),
            ),
        )
    }

    @Test
    fun `a content detail route reports the destination without a usable id as not-found`() {
        listOf("content-detail", "content-detail/", "content-detail/has space").forEach { route ->
            assertEquals(
                "route $route must still open the not-found destination",
                SampleNavAction.Push(SampleDestination.CONTENT_DETAIL, null),
                navActionFor(SageEvent.NavigateTo(destination = route, source = "test")),
            )
        }
    }

    @Test
    fun `single-top-level navigation follows the same route policy`() {
        assertEquals(
            SampleNavAction.ShowTopLevel(SampleDestination.SEARCH),
            navActionFor(SageEvent.NavigateSingleTopLevel(destination = "search", source = "test")),
        )
        assertEquals(
            SampleNavAction.Push(SampleDestination.SETTINGS),
            navActionFor(SageEvent.NavigateSingleTopLevel(destination = "settings", source = "test")),
        )
    }

    @Test
    fun `an unknown route has no navigation action`() {
        assertNull(navActionFor(SageEvent.NavigateTo(destination = "nowhere", source = "test")))
    }

    @Test
    fun `back pops the current screen`() {
        assertEquals(SampleNavAction.Pop, navActionFor(SageEvent.NavigateBack(source = "test")))
    }

    @Test
    fun `events the app does not translate have no navigation action`() {
        assertNull(navActionFor(SageEvent.UpdateTitle(title = "Ignored", source = "test")))
    }
}
