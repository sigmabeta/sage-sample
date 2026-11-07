package net.sigmabeta.sage.sample.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.feature.contentdetail.contentDetailRoute
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider

/**
 * Home's view model.
 *
 * [HomeModuleFactory] builds one plan from the graph's registered definitions; each module
 * publishes its own [LCE] flow, and the view model collects them in parallel, patching only that
 * module's slot so rows render independently as their data arrives.
 *
 * The tab root is retained across tab switches, so the screen is not rebuilt when the "Home shows
 * same content" preference changes. The view model observes the setting and, on a real change,
 * cancels the current plan's collectors, builds the new plan, and starts collecting its modules.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class HomeViewModel(
    private val moduleFactory: HomeModuleFactory,
    private val settingsManager: SampleSettingsManager,
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
) : SampleListViewModel<HomeState>() {
    override val screenIdentifier = AnalyticsScreen.NONE

    private var modules: List<HomeModule> = moduleFactory.create(
        homeShowsSameContent = settingsManager.homeShowsSameContent.value,
    )

    /** The plan mode the current [modules] were built for. */
    private var showsSameContent = settingsManager.homeShowsSameContent.value

    /** Collectors for the current plan; cancelled when the plan is rebuilt. */
    private val moduleJobs = mutableListOf<Job>()

    /**
     * Bumped for every plan build. A collector from the previous plan may already have queued an
     * update when it was cancelled; the generation check keeps that stale row out of the new plan.
     */
    private var planGeneration = 0

    override fun initialState(): HomeState = HomeState(sections = modules.toSections())

    init {
        sendAction(SageAction.InitNoArgs)
        collectModules(modules)

        viewModelScope.launch {
            settingsManager.homeShowsSameContent.collect { showSameContent ->
                if (showSameContent != showsSameContent) {
                    showsSameContent = showSameContent
                    replacePlan(showSameContent)
                }
            }
        }
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is HomeAction.ItemClicked -> emitEvent(
                SageEvent.NavigateTo(
                    destination = contentDetailRoute(action.contentId),
                    source = "HomeViewModel",
                ),
            )

            else -> Unit
        }
    }

    private fun replacePlan(homeShowsSameContent: Boolean) {
        moduleJobs.forEach { it.cancel() }
        moduleJobs.clear()

        val newModules = moduleFactory.create(homeShowsSameContent)
        modules = newModules
        // Drop the old plan's rows before the new modules start emitting, so a stale section
        // cannot linger after a preference flip.
        updateState { it.copy(sections = newModules.toSections()) }
        collectModules(newModules)
    }

    private fun collectModules(modules: List<HomeModule>) {
        val generation = ++planGeneration
        modules.forEach { module ->
            moduleJobs += module.state()
                .onEach { lce ->
                    updateState { state ->
                        if (generation != planGeneration) {
                            state
                        } else {
                            state.copy(
                                sections = state.sections
                                    .map { section ->
                                        if (section.id == module.id) {
                                            section.copy(lce = lce, hasEmitted = true)
                                        } else {
                                            section
                                        }
                                    }
                                    .toImmutableList(),
                            )
                        }
                    }
                }
                .runInBackground()
        }
    }
}

private fun List<HomeModule>.toSections(): ImmutableList<HomeSectionState> =
    sortedBy { it.priority }
        .map { HomeSectionState(it.id, it.priority, LCE.Uninitialized, it.showHeader) }
        .toImmutableList()
