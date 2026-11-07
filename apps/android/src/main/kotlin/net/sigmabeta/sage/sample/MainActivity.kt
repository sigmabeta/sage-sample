package net.sigmabeta.sage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import net.sigmabeta.sage.sample.ui.SampleApp
import net.sigmabeta.sage.sample.ui.components.api.LocalComponentStringProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No @AndroidEntryPoint member injection: entry points read the graph off the
        // application. Here that is only the ViewModel factory — screens resolve their own
        // view models from it with metroViewModel().
        val graph = (application as SampleApplication).appGraph

        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalComponentStringProvider provides graph.stringProvider,
            ) {
                SampleApp(settingsManager = graph.settingsManager)
            }
        }
    }
}
