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
 * Grid of Images' view model.
 *
 * It loads through [GridOfImagesRepository] one page at a time. Paging behaviour lives in
 * [PagedListItemsViewModel]; this class supplies the repository call and the state copy, then
 * keeps the screen's own cell action.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class GridOfImagesViewModel(
    stringProvider: StringProvider,
    analytics: Analytics,
    dispatchers: SageDispatchers,
    delayManager: SampleDelayManager,
    private val repository: GridOfImagesRepository,
) : PagedListItemsViewModel<GridOfImagesState>(
    stringProvider = stringProvider,
    analytics = analytics,
    dispatchers = dispatchers,
    delayManager = delayManager,
    operationName = GRID_OF_IMAGES_OPERATION_NAME,
) {
    override val screenIdentifier = AnalyticsScreen.NONE

    override val pageSize = GRID_OF_IMAGES_PAGE_SIZE

    override fun initialState(): GridOfImagesState = GridOfImagesState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        if (handlePagingAction(action)) return

        when (action) {
            is GridOfImagesAction.ItemClicked -> emitEvent(
                SageEvent.ShowSnackbar(
                    message = stringProvider.getStringOneArg(
                        ListItemsStringId.GRID_OF_IMAGES_ITEM_CLICKED,
                        action.item.name,
                    ),
                    withDismissAction = false,
                    source = "GridOfImagesViewModel",
                ),
            )

            else -> Unit
        }
    }

    override suspend fun fetchPage(
        offset: Int,
        limit: Int,
    ): LCE<List<SampleItem>> = repository.load(offset = offset, limit = limit).first()

    override fun contentOf(state: GridOfImagesState): LCE<List<SampleItem>> = state.content

    override fun windowStartOf(state: GridOfImagesState): Int = state.windowStart

    override fun hasMoreAfterOf(state: GridOfImagesState): Boolean = state.hasMoreAfter

    override fun withPaging(
        state: GridOfImagesState,
        content: LCE<List<SampleItem>>,
        windowStart: Int,
        hasMoreAfter: Boolean,
        loadingMore: Boolean,
        loadingPrevious: Boolean,
    ): GridOfImagesState = state.copy(
        content = content,
        windowStart = windowStart,
        hasMoreAfter = hasMoreAfter,
        loadingMore = loadingMore,
        loadingPrevious = loadingPrevious,
    )
}
