package net.sigmabeta.sage.sample.jvm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
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
import net.sigmabeta.sage.sample.jvm.ui.DesktopPlatformBack
import net.sigmabeta.sage.sample.jvm.ui.rememberWindowFocusLifecycleOwner
import net.sigmabeta.sage.sample.jvm.ui.navigation.HeldBackKeys
import net.sigmabeta.sage.sample.jvm.ui.navigation.isBackKey
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider
import net.sigmabeta.sage.sample.ui.shell.PerScreenSampleScreenContainer
import net.sigmabeta.sage.sample.ui.shell.SampleApp

/**
 * Desktop entry point: builds the app-scoped Metro graph once and opens the Compose window.
 *
 * Screens resolve their own view models with `metroViewModel()`, so the window only installs the
 * graph's `MetroViewModelFactory`; there is no feature state to hand across the boundary.
 *
 * Escape and Backspace are caught at the window level so they work whenever the window is focused,
 * not only after a focusable child has been clicked. `onKeyEvent` is the bubble phase: a focused
 * text field consumes Backspace first, so editing is unaffected. Signals are emitted through
 * [backKeyEvents] and run the shell's shared Back policy; [HeldBackKeys] fires once per physical
 * press rather than repeating while the key auto-repeats. A window that loses focus forgets the
 * held keys, because the KeyUp may arrive while another window is focused.
 */
fun main() {
    val graph = createGraphFactory<JvmSampleAppGraph.Factory>().create()

    // Register before any composable resolves the singleton, so AsyncImage requests for
    // `generated://` URLs reach :core:bitmaps' fetcher instead of the default loader.
    SingletonImageLoader.setSafe { context -> generatedImageLoader(context) }

    application {
        val backKeyEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val heldBackKeys = remember { HeldBackKeys() }
        val platformBack = remember(backKeyEvents) { DesktopPlatformBack(backKeyEvents) }

        Window(
            onCloseRequest = ::exitApplication,
            title = SampleStringProvider.string(SampleStringId.APP_NAME),
            state = rememberWindowState(size = DpSize(1000.dp, 800.dp)),
            onKeyEvent = { event ->
                when {
                    !isBackKey(event.key) -> false

                    event.type == KeyEventType.KeyDown -> {
                        if (heldBackKeys.press(event.key)) {
                            backKeyEvents.tryEmit(Unit)
                        }
                        true
                    }

                    event.type == KeyEventType.KeyUp -> {
                        heldBackKeys.release(event.key)
                        true
                    }

                    else -> false
                }
            },
        ) {
            // A KeyUp can be delivered to another window after focus loss, so forget held keys as
            // soon as this window is unfocused rather than waiting for a release that never comes.
            val isWindowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(isWindowFocused) {
                heldBackKeys.onFocusChanged(isWindowFocused)
            }

            val screenLifecycleOwner = rememberWindowFocusLifecycleOwner()

            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalSampleStringProvider provides graph.stringProvider,
            ) {
                SampleApp(
                    settingsManager = graph.settingsManager,
                    interFontFamily = InterFontFamily,
                    platformBack = platformBack,
                    screenContainer = PerScreenSampleScreenContainer,
                    screenLifecycleOwner = screenLifecycleOwner,
                )
            }
        }
    }
}

/**
 * Desktop loads the bundled OTF files from the classpath (`src/main/resources/fonts`) rather than
 * an Android `R.font` resource; the three weights otherwise match the Android mapping.
 */
private val InterFontFamily = FontFamily(
    Font("fonts/inter_regular.otf", FontWeight.Normal),
    Font("fonts/inter_medium.otf", FontWeight.Medium),
    Font("fonts/inter_bold.otf", FontWeight.Bold),
)
