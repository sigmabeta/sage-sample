package net.sigmabeta.sage.sample.feature.contentdetail
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.contentIdOrNull
import net.sigmabeta.sage.sample.models.contentDetailRoute
import net.sigmabeta.sage.sample.models.ContentId

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
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
import net.sigmabeta.sage.sample.repository.CONTENT_DETAIL_OPERATION_NAME
import net.sigmabeta.sage.sample.repository.ContentDetailRepository

/**
 * Content detail's view model.
 *
 * The addressed [ContentId] arrives from the screen through [SageAction.InitWithString], not from
 * the graph: `ContentDetailScreen` is a parameterized destination with its own view model per ID,
 * and the action is how that ID reaches this instance. A missing or invalid ID never reaches the
 * repository — it publishes the explicit not-found state ([LCE.Content] with a `null` payload).
 *
 * [SageAction.InitNoArgs] is only the screen-view report the base class logs; it does not load.
 * Collection runs in `viewModelScope` via [runInBackground], so it stops when the screen's
 * ViewModel is cleared and waits for the duration selected on the Settings screen. A second
 * `InitWithString` cancels the previous load, so an old ID's late emission cannot overwrite the
 * new screen state.
 *
 * Related rows and content parts address their own detail IDs (parent-qualified, so the opened
 * detail keeps that row's name and artwork) and push another detail, so Back returns through the
 * chain. The hero and generated call-to-action rows have no destination and still report their
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

    private var loadJob: Job? = null

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SageAction.InitNoArgs -> Unit

            is SageAction.InitWithString -> {
                val contentId = contentIdOrNull(action.arg)
                if (contentId == null) {
                    showNotFound()
                } else {
                    load(contentId)
                }
            }

            is ContentDetailAction.HeroClicked -> {
                val title = (internalUiState.value.content as? LCE.Content)?.data?.title
                if (title != null) showClickedName(title)
            }

            is ContentDetailAction.CtaClicked -> showClickedName(action.label)

            is ContentDetailAction.ItemClicked -> navigateTo(action.contentId)

            is ContentDetailAction.RelatedClicked -> navigateTo(action.contentId)

            else -> Unit
        }
    }

    private fun load(contentId: ContentId) {
        loadJob?.cancel()
        updateState {
            it.copy(content = LCE.Loading(CONTENT_DETAIL_OPERATION_NAME))
        }

        loadJob = repository.load(contentId)
            .onEach { content ->
                updateState { it.copy(content = content) }
            }
            .runInBackground()
    }

    /** A missing or malformed ID is a defined destination state, not an error. */
    private fun showNotFound() {
        loadJob?.cancel()
        updateState { it.copy(content = LCE.Content(null)) }
    }

    /** Pushes another detail on the active tab; Back returns to this one. */
    private fun navigateTo(contentId: ContentId) {
        emitEvent(
            SageEvent.NavigateTo(
                destination = contentDetailRoute(contentId),
                source = "ContentDetailViewModel",
            ),
        )
    }

    private fun showClickedName(name: String) {
        emitEvent(
            SageEvent.ShowSnackbar(
                message = stringProvider.getStringOneArg(
                    SampleStringId.CONTENT_DETAIL_ITEM_CLICKED,
                    name,
                ),
                withDismissAction = false,
                source = "ContentDetailViewModel",
            ),
        )
    }
}
