package net.sigmabeta.sage.sample.jvm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import net.sigmabeta.sage.sample.bitmaps.generatedImageLoader
import net.sigmabeta.sage.sample.jvm.di.JvmSampleAppGraph
import net.sigmabeta.sage.sample.jvm.ui.SampleApp
import net.sigmabeta.sage.sample.jvm.ui.navigation.isBackKey
import net.sigmabeta.sage.sample.ui.components.api.LocalComponentStringProvider

/**
 * Desktop entry point: builds the app-scoped Metro graph once and opens the Compose window.
 *
 * Screens resolve their own view models with `metroViewModel()`, so the window only installs the
 * graph's `MetroViewModelFactory`; there is no feature state to hand across the boundary.
 *
 * Escape and Backspace are caught at the window level so they work whenever the window is focused,
 * not only after a focusable child has been clicked. `onKeyEvent` is the bubble phase: a focused
 * text field consumes Backspace first, so editing is unaffected. Signals are emitted through
 * [backKeyEvents] and run the shell's shared Back policy; [heldBackKeys] fires once per physical
 * press rather than repeating while the key auto-repeats.
 */
fun main() {
    val graph = createGraphFactory<JvmSampleAppGraph.Factory>().create()

    // Register before any composable resolves the singleton, so AsyncImage requests for
    // `generated://` URLs reach :core:bitmaps' fetcher instead of the default loader.
    SingletonImageLoader.setSafe { context -> generatedImageLoader(context) }

    application {
        val backKeyEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val heldBackKeys = remember { mutableSetOf<Key>() }
        Window(
            onCloseRequest = ::exitApplication,
            title = SampleStrings.APP_NAME,
            state = rememberWindowState(size = DpSize(1000.dp, 800.dp)),
            onKeyEvent = { event ->
                when {
                    !isBackKey(event.key) -> false

                    event.type == KeyEventType.KeyDown -> {
                        if (heldBackKeys.add(event.key)) {
                            backKeyEvents.tryEmit(Unit)
                        }
                        true
                    }

                    event.type == KeyEventType.KeyUp -> {
                        heldBackKeys.remove(event.key)
                        true
                    }

                    else -> false
                }
            },
        ) {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalComponentStringProvider provides graph.stringProvider,
            ) {
                SampleApp(
                    settingsManager = graph.settingsManager,
                    platformBackKeys = backKeyEvents,
                )
            }
        }
    }
}
