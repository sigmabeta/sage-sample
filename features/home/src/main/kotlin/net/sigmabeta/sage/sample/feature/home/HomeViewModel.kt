package net.sigmabeta.sage.sample.feature.home

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.collections.immutable.toImmutableList
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
 * Home's view model.
 *
 * The randomized module list is provided by [HomeModuleFactory] through the app graph. Each
 * module publishes its own [LCE] flow; the view model collects them in parallel and patches only
 * that module's slot, so rows render independently as their data arrives — no `combine` /
 * `flatMapMerge` orchestration.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class HomeViewModel(
    private val modules: List<HomeModule>,
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
) : SampleListViewModel<HomeState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    override fun initialState(): HomeState = HomeState(
        sections = modules
            .sortedBy { it.priority }
            .map { HomeSectionState(it.id, it.priority, LCE.Uninitialized, it.showHeader) }
            .toImmutableList(),
    )

    init {
        sendAction(SageAction.InitNoArgs)

        modules.forEach { module ->
            module.state()
                .onEach { lce ->
                    updateState { state ->
                        state.copy(
                            sections = state.sections
                                .map { section ->
                                    if (section.id == module.id) {
                                        section.copy(lce = lce)
                                    } else {
                                        section
                                    }
                                }
                                .toImmutableList(),
                        )
                    }
                }
                .runInBackground()
        }
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is HomeAction.ItemClicked -> emitEvent(
                SageEvent.ShowSnackbar(
                    message = stringProvider.getStringOneArg(
                        HomeStringId.HOME_ITEM_CLICKED,
                        action.title,
                    ),
                    withDismissAction = false,
                    source = "HomeViewModel",
                ),
            )

            else -> Unit
        }
    }
}
