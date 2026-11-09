package net.sigmabeta.sage.sample.feature.home

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel

/**
 * One pluggable row of the Home screen.
 *
 * [HomeViewModel] collects every module's [state] flow in parallel and patches the corresponding
 * slot in [HomeState], so rows appear independently as their data arrives. [HomeModuleFactory]
 * builds the per-screen module list; ids must be unique within that list because the id keys the
 * section slot and its loading operation.
 *
 * **Keep [state] cheap.** Home collects every module's flow when the screen opens, so a row must
 * not pull the whole catalog and filter or shuffle it in memory. Push the filter, limit, and any
 * random pick down into a bounded repository query.
 */
interface HomeModule {
    /** Stable identity used to key the section's state slot and loading operation. */
    val id: String

    /** Lower values render earlier on screen. */
    val priority: Int

    /**
     * Whether to prepend a [SectionHeaderListModel] above the module's items. Defaults to true;
     * override to false for modules whose content already carries its own heading.
     */
    val showHeader: Boolean
        get() = true

    fun state(): Flow<LCE<HomeModuleSection>>
}

/**
 * A module's resolved row: the visible title plus the pre-built items that go inside it.
 *
 * Modules build the [ListModel]s (image, aspect ratio, click action); [HomeState] prepends the
 * section header and wraps multi-item rows in a horizontal scroller.
 */
data class HomeModuleSection(
    val title: String,
    val items: ImmutableList<ListModel>,
)
