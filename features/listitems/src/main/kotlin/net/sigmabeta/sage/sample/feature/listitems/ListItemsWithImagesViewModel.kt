package net.sigmabeta.sage.sample.feature.listitems

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.onEach
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import net.sigmabeta.sage.sample.feature.contentdetail.contentDetailRoute
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * List Items with Images' view model.
 *
 * It loads through the same [SampleItemRepository] as List Items, so both screens show the same
 * generated rows; only the rendered model type differs (image + name + caption instead of name +
 * caption). Collection runs in `viewModelScope` via [runInBackground], so it stops when the
 * screen's ViewModel is cleared and waits for the duration selected on the Settings screen.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class ListItemsWithImagesViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: SampleItemRepository,
) : SampleListViewModel<ListItemsWithImagesState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): ListItemsWithImagesState = ListItemsWithImagesState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs -> load()

            is ListItemsWithImagesAction.ItemClicked -> emitEvent(
                SageEvent.NavigateTo(
                    destination = contentDetailRoute(
                        ContentId("list-items-with-images:${action.item.id}"),
                    ),
                    source = "ListItemsWithImagesViewModel",
                ),
            )

            else -> Unit
        }
    }

    private fun load() {
        updateState {
            ListItemsWithImagesState(
                LCE.Loading(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME),
            )
        }

        repository.load()
            .onEach { content ->
                updateState { ListItemsWithImagesState(content) }
            }
            .runInBackground()
    }
}
