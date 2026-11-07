package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * Home's content, declared as a SAGE [ListState].
 *
 * [sections] is one slot per configured [HomeModule], in priority order. Each slot owns its own
 * [LCE] and renders independently, so a slow or failed row never blocks the rows around it.
 */
data class HomeState(
    val sections: ImmutableList<HomeSectionState> = persistentListOf(),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(HomeStringId.HOME_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        sections.flatMap { it.toListItems() }

    // Map the states directly rather than through `withStandardErrorAndLoading`: each section
    // owns its id, so its loading and error rows get a per-module operation name and one module's
    // failure never replaces the rows around it.
    private fun HomeSectionState.toListItems(): List<ListModel> = when (val section = lce) {
        LCE.Uninitialized -> emptyList()

        is LCE.Loading -> loading(
            operationName = "home.$id.load",
            loadingType = LoadingType.COVER,
            loadingItemCount = HOME_LOADING_ITEMS,
            loadingWithHeader = showHeader,
            loadingHorizScrollable = true,
        )

        is LCE.Error -> error("home.$id.load", section.error)

        is LCE.Content -> sectionContent(section.data)
    }

    private fun HomeSectionState.sectionContent(data: HomeModuleSection): List<ListModel> {
        val header: List<ListModel> = if (showHeader) {
            listOf(SectionHeaderListModel(data.title))
        } else {
            emptyList()
        }

        return when (data.items.size) {
            // A module that resolved to nothing should not leave a bare heading behind.
            0 -> emptyList()

            // Modules emitting a single item get the full screen width for that item — no
            // carousel wrapping — so a hero can stand alone.
            1 -> header + data.items

            else -> header + HorizontalScrollerListModel(
                dataId = "home.$id.scroller".hashCode().toLong(),
                scrollingItems = data.items,
            )
        }
    }
}

/** One module's slot: its stable identity plus the state it last published. */
data class HomeSectionState(
    val id: String,
    val priority: Int,
    val lce: LCE<HomeModuleSection>,
    val showHeader: Boolean = true,
)

private const val HOME_LOADING_ITEMS = 6
