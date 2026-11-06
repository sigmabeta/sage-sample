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
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Grid of Images' view model.
 *
 * It loads through [GridOfImagesRepository] and publishes each [LCE] through [updateState].
 * Collection runs in `viewModelScope` via [runInBackground], so it stops when the screen's
 * ViewModel is cleared and waits for the duration selected on the Settings screen.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class GridOfImagesViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: GridOfImagesRepository,
) : SampleListViewModel<GridOfImagesState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): GridOfImagesState = GridOfImagesState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs -> load()

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

    private fun load() {
        updateState {
            GridOfImagesState(LCE.Loading(GRID_OF_IMAGES_OPERATION_NAME))
        }

        repository.load()
            .onEach { content ->
                updateState { GridOfImagesState(content) }
            }
            .runInBackground()
    }
}
