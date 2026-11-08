package net.sigmabeta.sage.sample.feature.home
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.Icon
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
            title = stringProvider.getString(SampleStringId.HOME_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        screenLevelItems(stringProvider) ?: sections.flatMap { it.toListItems() }

    /**
     * The one row that replaces per-module rows once the whole plan has settled to a terminal,
     * screen-level state: every module failed, or every module resolved to no content.
     *
     * Returns null while any section has not emitted at all: a freshly built plan (including the
     * brief [HomeViewModel.replacePlan] window) is indistinguishable from Empty demo mode by
     * [LCE] value alone, because a module that hides itself also publishes `Uninitialized`.
     * [HomeSectionState.hasEmitted] is what tells the two apart, so the empty state waits for
     * every module to report once and loading never flashes it.
     */
    private fun screenLevelItems(stringProvider: StringProvider): List<ListModel>? {
        if (sections.any { !it.hasEmitted }) return null
        return when {
            sections.isNotEmpty() && sections.all { it.lce is LCE.Error } -> listOf(
                ErrorStateListModel(
                    failedOperationName = HOME_LOAD_OPERATION,
                    errorString = stringProvider.getString(SampleStringId.HOME_ALL_ERROR),
                    error = (sections.first().lce as LCE.Error).error,
                ),
            )

            sections.all { it.lce.resolvesToNoItems() } -> listOf(
                EmptyStateListModel(
                    icon = Icon.Home,
                    explanation = stringProvider.getString(SampleStringId.HOME_EMPTY),
                    showCrossOut = false,
                ),
            )

            else -> null
        }
    }

    private fun LCE<HomeModuleSection>.resolvesToNoItems(): Boolean = when (this) {
        LCE.Uninitialized -> true
        is LCE.Content -> data.items.isEmpty()
        else -> false
    }

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

/**
 * One module's slot: its stable identity plus the state it last published.
 *
 * [hasEmitted] is false until the module's flow has published at least one value for this plan.
 * It separates "this plan has not started yet" from a module that genuinely resolved to nothing
 * (both can hold [LCE.Uninitialized]), so a plan rebuild never looks like Empty demo mode.
 */
data class HomeSectionState(
    val id: String,
    val priority: Int,
    val lce: LCE<HomeModuleSection>,
    val showHeader: Boolean = true,
    val hasEmitted: Boolean = false,
)

private const val HOME_LOADING_ITEMS = 6

/** Operation name for the screen-level plan result, when per-module operations are not shown. */
private const val HOME_LOAD_OPERATION = "home.load"
