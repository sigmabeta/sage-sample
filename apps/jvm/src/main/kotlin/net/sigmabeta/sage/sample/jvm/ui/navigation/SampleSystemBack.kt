package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * The platform's whole-window Back input.
 *
 * The desktop window forwards Escape/Backspace as signals through [LocalPlatformBackKeys]; each one
 * invokes [onBack], the same handler as the top-bar arrow and `SageEvent.NavigateBack`. Unlike
 * Android, [enabled] is deliberately not applied here: nothing else on desktop pops a pushed
 * screen, and the shared Back policy itself decides Pop / ShowTab / Outer at every level. Gating on
 * `canPop` would swallow the Search-root and outer-root branches.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SampleSystemBack(enabled: Boolean, onBack: () -> Unit) {
    val platformBackKeys = LocalPlatformBackKeys.current ?: return
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(platformBackKeys) {
        platformBackKeys.collect { currentOnBack() }
    }
}
