package net.sigmabeta.sage.sample.feature.browse
import net.sigmabeta.sage.sample.models.DEFAULT_CONTENT_ID
import net.sigmabeta.sage.sample.models.contentDetailRoute

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's view model.
 *
 * The screen is a static destination list, so the view model owns only navigation intents: each
 * row's [BrowseAction] becomes a one-shot [SageEvent.NavigateTo]. There is no repository load, so
 * the destinations never sit behind a loading state.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class BrowseViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
) : SampleListViewModel<BrowseState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): BrowseState = BrowseState

    init {
        sendAction(SageAction.InitNoArgs)
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is BrowseAction.OpenListItems -> emitEvent(
                SageEvent.NavigateTo(
                    // Route contract with the app: this must stay in sync with
                    // SampleDestination.LIST_ITEMS.destName. SampleDestinationTest guards the
                    // mapping, so changing either side alone fails a test.
                    destination = "list-items",
                    source = "BrowseViewModel",
                ),
            )

            is BrowseAction.OpenListItemsWithImages -> emitEvent(
                SageEvent.NavigateTo(
                    // Same route contract, guarded by SampleDestinationTest.
                    destination = "list-items-with-images",
                    source = "BrowseViewModel",
                ),
            )

            is BrowseAction.OpenGridOfImages -> emitEvent(
                SageEvent.NavigateTo(
                    // Same route contract, guarded by SampleDestinationTest.
                    destination = "grid-of-images",
                    source = "BrowseViewModel",
                ),
            )

            is BrowseAction.OpenContentDetail -> emitEvent(
                SageEvent.NavigateTo(
                    // Browse opens a defined default item; the route still carries a stable ID.
                    destination = contentDetailRoute(DEFAULT_CONTENT_ID),
                    source = "BrowseViewModel",
                ),
            )

            else -> Unit
        }
    }
}
