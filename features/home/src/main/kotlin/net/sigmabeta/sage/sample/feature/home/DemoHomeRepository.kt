package net.sigmabeta.sage.sample.feature.home

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.ui.StringGenerator

/**
 * The sample's generated Home datasource.
 *
 * Each module instance passes its id as [seed], and this repository builds a small catalog from
 * that seed. Two instances of the same module therefore show different titles (and the modules
 * add the same seed to their artwork URLs), while re-visiting one instance reproduces its
 * content exactly. The artificial latency comes from the shared `runInBackground` seam, which
 * applies the duration selected on the Settings screen; this repository does not add a delay of
 * its own.
 *
 * The Settings demo outcome is read at call time, not cached at construction:
 * [DemoOutcome.CONTENT] returns the generated catalog, [DemoOutcome.EMPTY] returns a successful
 * empty result, and [DemoOutcome.ERROR] fails the call. Both terminal outcomes therefore reach
 * Home's empty and all-error presentations, and changing the setting affects the next load with
 * no app restart.
 *
 * Metro binds it to [HomeRepository] once for the application graph.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoHomeRepository(
    private val settingsManager: SampleSettingsManager,
) : HomeRepository {
    override fun itemCount(seed: String): Int = when (settingsManager.demoOutcome.value) {
        DemoOutcome.CONTENT -> catalog(seed).size
        DemoOutcome.EMPTY -> 0
        DemoOutcome.ERROR -> throw demoFailure()
    }

    override fun itemAt(seed: String, index: Int): HomeItem? =
        when (settingsManager.demoOutcome.value) {
            DemoOutcome.CONTENT -> catalog(seed).getOrNull(index)
            DemoOutcome.EMPTY -> null
            DemoOutcome.ERROR -> throw demoFailure()
        }

    override fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>> = flow {
        emit(
            when (settingsManager.demoOutcome.value) {
                DemoOutcome.CONTENT -> LCE.Content(catalog(seed).take(limit))

                DemoOutcome.EMPTY -> LCE.Content(emptyList())

                DemoOutcome.ERROR -> LCE.Error(
                    operationName = "home.$seed.load",
                    error = demoFailure(),
                )
            },
        )
    }

    /** A stable catalog for [seed]; generating it is cheap and keeps the repository stateless. */
    private fun catalog(seed: String): List<HomeItem> {
        val generator = StringGenerator(Random(seed.hashCode().toLong()))
        return List(CATALOG_SIZE) { index ->
            HomeItem(
                id = index + 1L,
                title = generator.generateTitle(),
            )
        }
    }

    private fun demoFailure() = IllegalStateException("Demo outcome is ERROR")

    private companion object {
        const val CATALOG_SIZE = 18
    }
}
