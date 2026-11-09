@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package net.sigmabeta.sage.sample.js

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.ComposeViewport
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableSharedFlow
import net.sigmabeta.sage.sample.js.di.WebSampleAppGraph
import net.sigmabeta.sage.sample.js.ui.WebPlatformBack
import net.sigmabeta.sage.sample.ui.components.api.subs.LocalForceFakeImages
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider
import net.sigmabeta.sage.sample.ui.shell.PerScreenSampleScreenContainer
import net.sigmabeta.sage.sample.ui.shell.SampleApp
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent

/**
 * Browser entry point. Mirrors `apps/jvm`'s `main` and Android's `MainActivity.onCreate`: build the
 * app-scoped graph once, install its ViewModel factory and strings, then mount the shared shell in
 * a [ComposeViewport].
 *
 * The web client is a release-shaped client: the server owns the data and the generated artwork is
 * drawn directly (not fetched through Coil), so the app only supplies browser seams.
 */
fun main() {
    val graph = createGraphFactory<WebSampleAppGraph.Factory>().create()

    val backKeys = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    window.addEventListener("keydown", { rawEvent ->
        val event = rawEvent.unsafeCast<KeyboardEvent>()
        if (event.key != "Escape") return@addEventListener
        val target = event.target
        if (target is HTMLInputElement || target is HTMLTextAreaElement) return@addEventListener
        backKeys.tryEmit(Unit)
    })

    document.getElementById("splash")?.remove()

    ComposeViewport(document.body!!) {
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            LocalSampleStringProvider provides graph.stringProvider,
            // The sample's images are generated gradients. Drawing them directly is the same path
            // previews use, and it avoids needing a Coil fetcher for `generated://` on the web.
            LocalForceFakeImages provides true,
        ) {
            // Compose for Web only wires Tab/Shift-Tab; arrow keys move focus like a D-pad, as the
            // desktop and Android builds do.
            val focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val direction = when (event.key) {
                            Key.DirectionUp -> FocusDirection.Up
                            Key.DirectionDown -> FocusDirection.Down
                            Key.DirectionLeft -> FocusDirection.Left
                            Key.DirectionRight -> FocusDirection.Right
                            else -> return@onKeyEvent false
                        }
                        focusManager.moveFocus(direction)
                    },
            ) {
                SampleApp(
                    settingsManager = graph.settingsManager,
                    platformBack = WebPlatformBack(backKeys),
                    screenContainer = PerScreenSampleScreenContainer,
                    browserNavigation = graph.browserNavigation,
                )
            }
        }
    }
}
