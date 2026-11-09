package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.sample.jvm.ui.navigation.backMouseButton
import net.sigmabeta.sage.sample.ui.shell.SamplePlatformBack

/**
 * The desktop window's Back input.
 *
 * [backKeys] is the Escape/Backspace signal stream built by `Main`'s window handler; each signal
 * invokes [onBack], the same handler as the top-bar arrow and `SageEvent.NavigateBack`. Unlike
 * Android, `enabled` is deliberately not applied: nothing else on desktop pops a pushed screen,
 * and the shared Back policy itself decides Pop / ShowTopLevel / Outer at every level. Gating on
 * `canPop` would swallow the Search-root and outer-root branches.
 *
 * The mouse back button is a pointer event, so it needs no focus and is observed across the whole
 * shell; [wrap] contributes it.
 */
class DesktopPlatformBack(private val backKeys: Flow<Unit>) : SamplePlatformBack {
    @Composable
    override fun Handler(enabled: Boolean, onBack: () -> Unit) {
        val currentOnBack by rememberUpdatedState(onBack)
        LaunchedEffect(backKeys) {
            backKeys.collect { currentOnBack() }
        }
    }

    override fun wrap(base: Modifier, onBack: () -> Unit): Modifier = base.backMouseButton(onBack)
}
