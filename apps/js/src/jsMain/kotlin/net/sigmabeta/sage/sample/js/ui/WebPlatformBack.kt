package net.sigmabeta.sage.sample.js.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.sample.ui.shell.SamplePlatformBack

/**
 * The browser window's Back input: Escape presses outside a text field.
 *
 * The shell asks the browser bridge first (an app entry behind the current URL), so Escape behaves
 * like the browser's Back button while in-app history exists. Backspace is deliberately not wired:
 * the browser already uses it for URL back-navigation, and text editing needs it.
 */
class WebPlatformBack(private val backKeys: Flow<Unit>) : SamplePlatformBack {
    @Composable
    override fun Handler(enabled: Boolean, onBack: () -> Unit) {
        val currentOnBack by rememberUpdatedState(onBack)
        LaunchedEffect(backKeys) {
            backKeys.collect { currentOnBack() }
        }
    }
}
