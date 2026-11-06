package net.sigmabeta.sage.sample.jvm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import net.sigmabeta.sage.sample.bitmaps.generatedImageLoader
import net.sigmabeta.sage.sample.jvm.di.JvmSampleAppGraph
import net.sigmabeta.sage.sample.jvm.ui.SampleApp
import net.sigmabeta.sage.sample.ui.components.api.LocalComponentStringProvider

/**
 * Desktop entry point: builds the app-scoped Metro graph once and opens the Compose window.
 *
 * Screens resolve their own view models with `metroViewModel()`, so the window only installs the
 * graph's `MetroViewModelFactory`; there is no feature state to hand across the boundary.
 */
fun main() {
    val graph = createGraphFactory<JvmSampleAppGraph.Factory>().create()

    // Register before any composable resolves the singleton, so AsyncImage requests for
    // `generated://` URLs reach :core:bitmaps' fetcher instead of the default loader.
    SingletonImageLoader.setSafe { context -> generatedImageLoader(context) }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = SampleStrings.APP_NAME,
            state = rememberWindowState(size = DpSize(1000.dp, 800.dp)),
        ) {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalComponentStringProvider provides graph.stringProvider,
            ) {
                SampleApp(settingsManager = graph.settingsManager)
            }
        }
    }
}
