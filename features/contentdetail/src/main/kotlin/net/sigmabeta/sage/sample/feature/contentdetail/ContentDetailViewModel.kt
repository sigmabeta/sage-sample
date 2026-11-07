package net.sigmabeta.sage.sample.feature.contentdetail

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
 * Content detail's view model.
 *
 * It loads through [ContentDetailRepository] and publishes each [LCE] through [updateState].
 * Collection runs in `viewModelScope` via [runInBackground], so it stops when the screen's
 * ViewModel is cleared and waits for the duration selected on the Settings screen.
 *
 * The sample has no backend behaviour behind the generated rows: every tap reports the clicked
 * text in a snackbar.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class ContentDetailViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val repository: ContentDetailRepository,
) : SampleListViewModel<ContentDetailState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): ContentDetailState = ContentDetailState()

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs -> load()

            is ContentDetailAction.HeroClicked -> {
                val title = (internalUiState.value.content as? LCE.Content)?.data?.title
                if (title != null) showClickedName(title)
            }

            is ContentDetailAction.CtaClicked -> showClickedName(action.label)

            is ContentDetailAction.ItemClicked -> showClickedName(action.item.name)

            is ContentDetailAction.RelatedClicked -> showClickedName(action.related.name)

            else -> Unit
        }
    }

    private fun load() {
        updateState {
            it.copy(content = LCE.Loading(CONTENT_DETAIL_OPERATION_NAME))
        }

        repository.load()
            .onEach { content ->
                updateState { it.copy(content = content) }
            }
            .runInBackground()
    }

    private fun showClickedName(name: String) {
        emitEvent(
            SageEvent.ShowSnackbar(
                message = stringProvider.getStringOneArg(
                    ContentDetailStringId.CONTENT_DETAIL_ITEM_CLICKED,
                    name,
                ),
                withDismissAction = false,
                source = "ContentDetailViewModel",
            ),
        )
    }
}
