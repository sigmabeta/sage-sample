package net.sigmabeta.sage.sample.feature.listitems

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.first
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.ui.StringProvider

/**
 * List Items with Images' view model.
 *
 * It loads through the same [SampleItemRepository] as List Items, but opens a window and fetches
 * one page at a time instead of the whole generated set. Paging behaviour lives in
 * [PagedListItemsViewModel]; this class supplies the repository call and the state copy, then
 * keeps the screen's own click action.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class ListItemsWithImagesViewModel(
    stringProvider: StringProvider,
    analytics: Analytics,
    dispatchers: SageDispatchers,
    delayManager: SampleDelayManager,
    private val repository: SampleItemRepository,
) : PagedListItemsViewModel<ListItemsWithImagesState>(
    stringProvider = stringProvider,
    analytics = analytics,
    dispatchers = dispatchers,
    delayManager = delayManager,
    operationName = LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
) {
    override val screenIdentifier = AnalyticsScreen.NONE

    override val pageSize = LIST_ITEMS_WITH_IMAGES_PAGE_SIZE

    override fun initialState(): ListItemsWithImagesState = ListItemsWithImagesState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        if (handlePagingAction(action)) return

        when (action) {
            is ListItemsWithImagesAction.ItemClicked -> emitEvent(
                SageEvent.ShowSnackbar(
                    message = stringProvider.getStringOneArg(
                        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED,
                        action.item.name,
                    ),
                    withDismissAction = false,
                    source = "ListItemsWithImagesViewModel",
                ),
            )

            else -> Unit
        }
    }

    override suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): LCE<List<SampleItem>> = repository.load(offset = offset, limit = limit).first()

    override fun contentOf(state: ListItemsWithImagesState): LCE<List<SampleItem>> = state.content

    override fun windowStartOf(state: ListItemsWithImagesState): Int = state.windowStart

    override fun hasMoreAfterOf(state: ListItemsWithImagesState): Boolean = state.hasMoreAfter

    override fun withPaging(
        state: ListItemsWithImagesState,
        content: LCE<List<SampleItem>>,
        windowStart: Int,
        hasMoreAfter: Boolean,
        loadingMore: Boolean,
        loadingPrevious: Boolean,
    ): ListItemsWithImagesState = state.copy(
        content = content,
        windowStart = windowStart,
        hasMoreAfter = hasMoreAfter,
        loadingMore = loadingMore,
        loadingPrevious = loadingPrevious,
    )
}
