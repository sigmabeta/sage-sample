package net.sigmabeta.sage.sample.repository

import net.sigmabeta.sage.sample.models.ContentAction
import net.sigmabeta.sage.sample.models.ContentDetail
import net.sigmabeta.sage.sample.models.ContentDetailField
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.ContentItem
import net.sigmabeta.sage.sample.models.HomeItem
import net.sigmabeta.sage.sample.models.RelatedContent
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.models.SearchResult
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The JSON contract shared by `apps:server` and the browser client. */
class RepositoryWireTest {
    @Test
    fun `simple rows round trip`() {
        assertEquals(HomeItem(1, "Title"), HomeItem(1, "Title").toDto().toDomain())
        assertEquals(
            SampleItem(2, "Name", "Caption"),
            SampleItem(2, "Name", "Caption").toDto().toDomain(),
        )
        assertEquals(
            SearchResult(3, "Name", "Caption"),
            SearchResult(3, "Name", "Caption").toDto().toDomain(),
        )
    }

    @Test
    fun `content detail round trips including icons`() {
        val detail = ContentDetail(
            id = ContentId("list-items:7"),
            title = "Title",
            details = listOf(ContentDetailField("Label", "Value")),
            actions = listOf(ContentAction("Play", Icon.Play)),
            related = listOf(RelatedContent(1, "Related")),
            items = listOf(ContentItem(2, "Item", "Caption")),
        )

        assertEquals(detail, detail.toDto().toDomainOrNull())
    }

    @Test
    fun `every icon has a stable wire name and round trips`() {
        val icons = listOf(
            Icon.Album, Icon.Back, Icon.Browse, Icon.Calendar, Icon.Caret, Icon.Chip,
            Icon.Clear, Icon.Computer, Icon.Delete, Icon.CrossOut, Icon.CrossOutError,
            Icon.CrossOutMenu, Icon.Description, Icon.Difficulty, Icon.Edit,
            Icon.FavoriteEmpty, Icon.FavoriteFilled, Icon.Forward, Icon.Home, Icon.Library,
            Icon.Menu, Icon.Minus, Icon.MusicNote, Icon.OfflineFilled, Icon.OfflineOutline,
            Icon.Overflow, Icon.Pause, Icon.Person, Icon.Play, Icon.Plus, Icon.QueueMusic,
            Icon.Refresh, Icon.Repeat, Icon.RepeatOne, Icon.Save, Icon.Search,
            Icon.SearchYoutube, Icon.Shuffle, Icon.SkipNext, Icon.SkipPrevious, Icon.Tag,
            Icon.Visibility, Icon.VisibilityOff, Icon.Warning,
        )

        icons.forEach { icon ->
            assertEquals(icon, iconFromWireName(icon.wireName()))
        }
        assertEquals(icons.size, icons.map { it.wireName() }.toSet().size)
        assertNull(iconFromWireName("not-an-icon"))
    }

    @Test
    fun `a malformed content id decodes to null instead of throwing`() {
        val dto = ContentDetail(
            ContentId("list-items:1"),
            "Title",
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        ).toDto().copy(id = "bad id")

        assertNull(dto.toDomainOrNull())
    }

    @Test
    fun `paging is clamped to the documented bounds`() {
        assertEquals(0 to DEFAULT_PAGE_LIMIT, normalizedPage(null, null))
        assertEquals(0 to 1, normalizedPage(-5, 1))
        assertEquals(0 to MAX_PAGE_LIMIT, normalizedPage(null, 10_000))
    }
}
